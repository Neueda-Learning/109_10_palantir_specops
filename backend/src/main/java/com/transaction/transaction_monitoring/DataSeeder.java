package com.transaction.transaction_monitoring;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Standalone database seeder.
 * Inserts 1000 transactions across 10 accounts/80 payees over the last 90 days,
 * then evaluates all 4 monitoring rules and creates alerts with full history.
 *
 * Prerequisites:
 *   - MySQL running with the schema created (start the app once first)
 *   - monitoring_rules table populated (data.sql runs automatically on first start)
 *
 * Run:
 *   cd 109_10_palantir_specops\backend
 *   .\mvnw.cmd compile exec:java -Dexec.mainClass="com.transaction.transaction_monitoring.DataSeeder"
 */
public class DataSeeder {

    // ── DB config (matches application.properties) ───────────────────────────
    private static final String DB_URL =
        "jdbc:mysql://localhost:3306/transaction_monitoring" +
        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "n3u3da!";

    // ── Seed constants ───────────────────────────────────────────────────────
    private static final Random RNG = new Random(42L);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] ACCOUNTS = {
        "ACC001","ACC002","ACC003","ACC004","ACC005",
        "ACC006","ACC007","ACC008","ACC009","ACC010"
    };

    // PAYEE001-060 → established payees | PAYEE061-080 → brand-new payees (trigger NEW_PAYEE rule)
    private static final List<String> KNOWN_PAYEES = new ArrayList<>();
    private static final List<String> NEW_PAYEES   = new ArrayList<>();
    static {
        for (int i = 1;  i <= 60; i++) KNOWN_PAYEES.add(String.format("PAYEE%03d", i));
        for (int i = 61; i <= 80; i++) NEW_PAYEES.add(String.format("PAYEE%03d", i));
    }

    // ── Simple data holder ───────────────────────────────────────────────────
    record Tx(long id, String acc, String payee, BigDecimal amount,
              String type, String status, LocalDateTime ts) {}

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            conn.setAutoCommit(false);
            try {
                System.out.println("========================================");
                System.out.println("   Transaction Monitoring Data Seeder   ");
                System.out.println("========================================");

                // 1. Load rule IDs
                Map<String, Long> rules = loadRules(conn);
                System.out.println("Rules loaded: " + rules);

                // 2. Build 1000 transactions in memory, then insert
                List<Tx> all = insertTransactions(conn, buildTransactions());
                System.out.printf("Inserted %d transactions%n", all.size());

                // 3. Evaluate each rule
                int a1 = evalAmountThreshold(conn, all, rules.get("AMOUNT_THRESHOLD"));
                int a2 = evalVelocity(conn, all, rules.get("VELOCITY"));
                int a3 = evalNewPayee(conn, all, rules.get("NEW_PAYEE"));
                int a4 = evalDailyLimit(conn, all, rules.get("DAILY_LIMIT"));

                conn.commit();

                System.out.println("\n============ Seeding Complete ============");
                System.out.printf("Transactions     : %d%n", all.size());
                System.out.printf("Total Alerts     : %d%n", a1 + a2 + a3 + a4);
                System.out.printf("  AMOUNT_THRESHOLD : %d%n", a1);
                System.out.printf("  VELOCITY         : %d%n", a2);
                System.out.printf("  NEW_PAYEE        : %d%n", a3);
                System.out.printf("  DAILY_LIMIT      : %d%n", a4);
                System.out.println("==========================================");

            } catch (Exception ex) {
                conn.rollback();
                System.err.println("Seeding FAILED – rolled back. Error: " + ex.getMessage());
                ex.printStackTrace();
                throw ex;
            }
        }
    }

    // ── Build 1000 transactions (in memory, id=0 placeholder) ────────────────
    //  870 random + 30 amount_threshold + 20 new_payee + 50 velocity + 30 daily_limit = 1000
    static List<Tx> buildTransactions() {
        LocalDateTime now = LocalDateTime.now();
        List<Tx> list = new ArrayList<>(1000);

        // 870 – random, amounts $50–$9,500 (stay below $10k threshold)
        for (int i = 0; i < 870; i++) {
            LocalDateTime ts = now
                .minusDays(1 + RNG.nextInt(89))
                .minusHours(RNG.nextInt(24))
                .minusMinutes(RNG.nextInt(60));
            list.add(new Tx(0, pick(ACCOUNTS), pick(KNOWN_PAYEES),
                randomAmt(50, 9_500), rndType(), rndStatus(), ts));
        }

        // 30 – guaranteed AMOUNT_THRESHOLD triggers ($11,000–$90,000)
        for (int i = 0; i < 30; i++) {
            LocalDateTime ts = now.minusDays(1 + RNG.nextInt(89)).minusHours(RNG.nextInt(12));
            list.add(new Tx(0, pick(ACCOUNTS), pick(KNOWN_PAYEES),
                randomAmt(11_000, 90_000), "DEBIT", "COMPLETED", ts));
        }

        // 20 – guaranteed NEW_PAYEE triggers (PAYEE061-PAYEE080)
        for (int i = 0; i < 20; i++) {
            LocalDateTime ts = now.minusDays(1 + RNG.nextInt(60)).minusHours(RNG.nextInt(8));
            list.add(new Tx(0, ACCOUNTS[i % ACCOUNTS.length], NEW_PAYEES.get(i),
                randomAmt(200, 8_000), "DEBIT", "COMPLETED", ts));
        }

        // 50 – velocity burst: 5 accounts × 10 consecutive txns, 1 min apart (10 txns in 9 min > threshold of 5)
        for (int a = 0; a < 5; a++) {
            LocalDateTime base = now
                .minusDays(1 + RNG.nextInt(60))
                .withHour(9 + a).withMinute(0).withSecond(0).withNano(0);
            for (int j = 0; j < 10; j++) {
                list.add(new Tx(0, ACCOUNTS[a], pick(KNOWN_PAYEES),
                    randomAmt(100, 3_000), "DEBIT", "COMPLETED", base.plusMinutes(j)));
            }
        }

        // 30 – daily limit: 3 accounts × 10 txns ($6k–$8k each) → day total ~$70k > $50k limit
        for (int a = 0; a < 3; a++) {
            LocalDateTime base = now
                .minusDays(1 + RNG.nextInt(30))
                .withHour(8).withMinute(0).withSecond(0).withNano(0);
            for (int j = 0; j < 10; j++) {
                list.add(new Tx(0, ACCOUNTS[7 + a], pick(KNOWN_PAYEES),
                    randomAmt(6_000, 8_000), "DEBIT", "COMPLETED", base.plusHours(j)));
            }
        }

        return list; // exactly 1000
    }

    // ── Insert transactions via batch, collect generated IDs ─────────────────
    static List<Tx> insertTransactions(Connection conn, List<Tx> pending) throws SQLException {
        String sql = "INSERT INTO transactions (account_id,payee_id,amount,type,status,description,timestamp) VALUES(?,?,?,?,?,?,?)";
        List<Tx> result = new ArrayList<>(pending.size());

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Tx t : pending) {
                ps.setString(1, t.acc());
                ps.setString(2, t.payee());
                ps.setBigDecimal(3, t.amount());
                ps.setString(4, t.type());
                ps.setString(5, t.status());
                ps.setString(6, t.type() + " → " + t.payee());
                ps.setString(7, t.ts().format(TS));
                ps.addBatch();
            }
            ps.executeBatch();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int i = 0;
                while (keys.next()) {
                    Tx o = pending.get(i++);
                    result.add(new Tx(keys.getLong(1), o.acc(), o.payee(), o.amount(), o.type(), o.status(), o.ts()));
                }
            }
        }
        return result;
    }

    // ── Rule 1: AMOUNT_THRESHOLD – any single transaction > $10,000 ──────────
    static int evalAmountThreshold(Connection conn, List<Tx> txs, long ruleId) throws SQLException {
        int count = 0;
        for (Tx t : txs) {
            if (t.amount().compareTo(BigDecimal.valueOf(10_000)) > 0) {
                createAlert(conn, ruleId, "HIGH",
                    String.format("Account %s made a $%.2f transaction exceeding the $10,000 threshold",
                        t.acc(), t.amount()),
                    List.of(t.id()), t.ts());
                count++;
            }
        }
        return count;
    }

    // ── Rule 2: VELOCITY – more than 5 transactions within 10 minutes ────────
    static int evalVelocity(Connection conn, List<Tx> txs, long ruleId) throws SQLException {
        Map<String, List<Tx>> byAcc = txs.stream().collect(Collectors.groupingBy(Tx::acc));
        int count = 0;

        for (Map.Entry<String, List<Tx>> e : byAcc.entrySet()) {
            List<Tx> sorted = e.getValue().stream()
                .sorted(Comparator.comparing(Tx::ts)).toList();

            int i = 0;
            while (i < sorted.size()) {
                LocalDateTime winEnd = sorted.get(i).ts().plusMinutes(10);
                List<Long> window = new ArrayList<>();
                int j = i;
                while (j < sorted.size() && !sorted.get(j).ts().isAfter(winEnd)) {
                    window.add(sorted.get(j).id());
                    j++;
                }
                if (window.size() > 5) {
                    createAlert(conn, ruleId, "MEDIUM",
                        String.format("Account %s made %d transactions within 10 minutes",
                            e.getKey(), window.size()),
                        window, sorted.get(i).ts());
                    count++;
                    i = j; // jump past this burst to avoid duplicate alerts
                } else {
                    i++;
                }
            }
        }
        return count;
    }

    // ── Rule 3: NEW_PAYEE – transaction to a first-time payee ────────────────
    static int evalNewPayee(Connection conn, List<Tx> txs, long ruleId) throws SQLException {
        int count = 0;
        for (Tx t : txs) {
            if (NEW_PAYEES.contains(t.payee())) {
                createAlert(conn, ruleId, "LOW",
                    String.format("Account %s made first-ever transaction to new payee %s ($%.2f)",
                        t.acc(), t.payee(), t.amount()),
                    List.of(t.id()), t.ts());
                count++;
            }
        }
        return count;
    }

    // ── Rule 4: DAILY_LIMIT – cumulative daily total > $50,000 ───────────────
    static int evalDailyLimit(Connection conn, List<Tx> txs, long ruleId) throws SQLException {
        Map<String, Map<LocalDate, List<Tx>>> grouped = txs.stream().collect(
            Collectors.groupingBy(Tx::acc, Collectors.groupingBy(t -> t.ts().toLocalDate())));

        int count = 0;
        BigDecimal limit = BigDecimal.valueOf(50_000);

        for (Map.Entry<String, Map<LocalDate, List<Tx>>> accE : grouped.entrySet()) {
            for (Map.Entry<LocalDate, List<Tx>> dayE : accE.getValue().entrySet()) {
                BigDecimal total = dayE.getValue().stream()
                    .map(Tx::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

                if (total.compareTo(limit) > 0) {
                    List<Long> ids = dayE.getValue().stream().map(Tx::id).toList();
                    LocalDateTime earliest = dayE.getValue().stream()
                        .min(Comparator.comparing(Tx::ts)).get().ts();
                    createAlert(conn, ruleId, "HIGH",
                        String.format("Account %s cumulative daily total $%.2f exceeded $50,000 limit on %s",
                            accE.getKey(), total, dayE.getKey()),
                        ids, earliest);
                    count++;
                }
            }
        }
        return count;
    }

    // ── Create alert + link transactions + insert status history ─────────────
    static void createAlert(Connection conn, long ruleId, String severity,
                            String description, List<Long> txIds, LocalDateTime createdAt)
            throws SQLException {

        long daysOld = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        String[][] prog = statusProgression(daysOld);
        String finalStatus = prog[prog.length - 1][1];

        // Derive acknowledged_at / closed_at from progression timestamps
        LocalDateTime changeTs   = createdAt;
        LocalDateTime ackAt      = null;
        LocalDateTime closedAt   = null;
        for (String[] step : prog) {
            changeTs = changeTs.plusHours(1 + RNG.nextInt(6));
            if ("ACKNOWLEDGED".equals(step[1]) && ackAt == null)    ackAt    = changeTs;
            if (("CLOSED".equals(step[1]) || "DISMISSED".equals(step[1])) && closedAt == null) closedAt = changeTs;
        }

        // --- insert alert ---
        long alertId;
        String alertSql =
            "INSERT INTO alerts (rule_id, severity, status, description, created_at, updated_at, acknowledged_at, closed_at)" +
            " VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(alertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, ruleId);
            ps.setString(2, severity);
            ps.setString(3, finalStatus);
            ps.setString(4, description);
            ps.setString(5, createdAt.format(TS));
            ps.setString(6, createdAt.format(TS));
            if (ackAt    != null) ps.setString(7, ackAt.format(TS));    else ps.setNull(7, Types.TIMESTAMP);
            if (closedAt != null) ps.setString(8, closedAt.format(TS)); else ps.setNull(8, Types.TIMESTAMP);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); alertId = keys.getLong(1); }
        }

        // --- link alert ↔ transactions ---
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO alert_transactions (alert_id, transaction_id) VALUES (?,?)")) {
            for (long txId : txIds) {
                ps.setLong(1, alertId);
                ps.setLong(2, txId);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // --- status history ---
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO alert_status_history (alert_id, previous_status, new_status, notes, changed_at) VALUES (?,?,?,?,?)")) {
            changeTs = createdAt;
            for (String[] step : prog) {
                ps.setLong(1, alertId);
                if (step[0] != null) ps.setString(2, step[0]); else ps.setNull(2, Types.VARCHAR);
                ps.setString(3, step[1]);
                ps.setString(4, step[2]);
                ps.setString(5, changeTs.format(TS));
                ps.addBatch();
                changeTs = changeTs.plusHours(1 + RNG.nextInt(6));
            }
            ps.executeBatch();
        }
    }

    /**
     * Returns status transition steps based on how old the alert is.
     * Each row: [ previousStatus (nullable), newStatus, notes ]
     *
     * Age > 60 days → fully CLOSED with full history
     * Age 30-60 days → randomly CLOSED or DISMISSED
     * Age 7-30 days → ACKNOWLEDGED
     * Age < 7 days → OPEN
     */
    static String[][] statusProgression(long daysOld) {
        if (daysOld > 60) {
            return new String[][]{
                {null,            "OPEN",           "Alert triggered automatically"},
                {"OPEN",          "ACKNOWLEDGED",   "Assigned to analyst"},
                {"ACKNOWLEDGED",  "INVESTIGATING",  "Active investigation started"},
                {"INVESTIGATING", "CLOSED",         "Investigation complete – risk mitigated"}
            };
        } else if (daysOld > 30) {
            return RNG.nextBoolean()
                ? new String[][]{
                    {null,          "OPEN",          "Alert triggered automatically"},
                    {"OPEN",        "ACKNOWLEDGED",  "Reviewing transaction"},
                    {"ACKNOWLEDGED","DISMISSED",     "False positive – no risk detected"}
                  }
                : new String[][]{
                    {null,          "OPEN",          "Alert triggered automatically"},
                    {"OPEN",        "ACKNOWLEDGED",  "Assigned to analyst"},
                    {"ACKNOWLEDGED","CLOSED",        "Resolved – no further action required"}
                  };
        } else if (daysOld > 7) {
            return new String[][]{
                {null,      "OPEN",         "Alert triggered automatically"},
                {"OPEN",    "ACKNOWLEDGED", "Under review"}
            };
        } else {
            return new String[][]{
                {null, "OPEN", "Alert triggered automatically"}
            };
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    static Map<String, Long> loadRules(Connection conn) throws SQLException {
        Map<String, Long> map = new LinkedHashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, type FROM monitoring_rules WHERE active = 1")) {
            while (rs.next()) map.put(rs.getString("type"), rs.getLong("id"));
        }
        if (map.size() < 4) {
            throw new RuntimeException(
                "Expected 4 active rules in monitoring_rules, found " + map.size() + ".\n" +
                "Start the Spring Boot app at least once so data.sql populates the rules table.");
        }
        return map;
    }

    static BigDecimal randomAmt(int min, int max) {
        return BigDecimal.valueOf(min + RNG.nextDouble() * (max - min))
            .setScale(2, RoundingMode.HALF_UP);
    }

    static String rndType()   { return RNG.nextInt(10) < 7 ? "DEBIT" : "CREDIT"; }

    static String rndStatus() {
        int r = RNG.nextInt(10);
        return r < 7 ? "COMPLETED" : r < 9 ? "PENDING" : "FAILED";
    }

    static <T> T pick(T[] arr)       { return arr[RNG.nextInt(arr.length)]; }
    static <T> T pick(List<T> list)  { return list.get(RNG.nextInt(list.size())); }
}
