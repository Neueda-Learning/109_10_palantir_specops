package com.transaction.transaction_monitoring.repository;

import com.transaction.transaction_monitoring.enums.AlertStatus;
import com.transaction.transaction_monitoring.enums.Severity;
import com.transaction.transaction_monitoring.model.Alert;
import com.transaction.transaction_monitoring.model.AlertStatusHistory;
import com.transaction.transaction_monitoring.model.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class AlertRepository {

    private final JdbcTemplate jdbc;
    private final TransactionRepository transactionRepository;

    public AlertRepository(JdbcTemplate jdbc, TransactionRepository transactionRepository) {
        this.jdbc = jdbc;
        this.transactionRepository = transactionRepository;
    }

    private final RowMapper<Alert> alertRowMapper = (rs, rn) -> {
        Alert a = new Alert();
        a.setId(rs.getLong("id"));
        a.setRuleId(rs.getLong("rule_id"));
        a.setSeverity(Severity.valueOf(rs.getString("severity")));
        a.setStatus(AlertStatus.valueOf(rs.getString("status")));
        a.setDescription(rs.getString("description"));
        a.setResolutionNotes(rs.getString("resolution_notes"));
        Timestamp ca = rs.getTimestamp("created_at");
        a.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        Timestamp ua = rs.getTimestamp("updated_at");
        a.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        Timestamp aa = rs.getTimestamp("acknowledged_at");
        a.setAcknowledgedAt(aa != null ? aa.toLocalDateTime() : null);
        Timestamp cla = rs.getTimestamp("closed_at");
        a.setClosedAt(cla != null ? cla.toLocalDateTime() : null);
        return a;
    };

    private final RowMapper<AlertStatusHistory> historyRowMapper = (rs, rn) -> {
        AlertStatusHistory h = new AlertStatusHistory();
        h.setId(rs.getLong("id"));
        h.setAlertId(rs.getLong("alert_id"));
        h.setPreviousStatus(rs.getString("previous_status"));
        h.setNewStatus(rs.getString("new_status"));
        h.setNotes(rs.getString("notes"));
        Timestamp ca = rs.getTimestamp("changed_at");
        h.setChangedAt(ca != null ? ca.toLocalDateTime() : null);
        return h;
    };

    public Alert save(Alert a) {
        String sql = "INSERT INTO alerts (rule_id, severity, status, description) VALUES (?,?,?,?)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, a.getRuleId());
            ps.setString(2, a.getSeverity().name());
            ps.setString(3, a.getStatus().name());
            ps.setString(4, a.getDescription());
            return ps;
        }, key);
        a.setId(key.getKey().longValue());
        return a;
    }

    public Optional<Alert> findById(Long id) {
        List<Alert> results = jdbc.query("SELECT * FROM alerts WHERE id = ?", alertRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Alert> findAll(String status, String severity, LocalDateTime from, LocalDateTime to) {
        StringBuilder sql = new StringBuilder("SELECT * FROM alerts WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (severity != null && !severity.isBlank()) {
            sql.append(" AND severity = ?");
            params.add(severity);
        }
        if (from != null) {
            sql.append(" AND created_at >= ?");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND created_at <= ?");
            params.add(Timestamp.valueOf(to));
        }
        sql.append(" ORDER BY created_at DESC");
        return jdbc.query(sql.toString(), alertRowMapper, params.toArray());
    }

    public void updateStatus(Long id, AlertStatus newStatus, String resolutionNotes,
                             LocalDateTime acknowledgedAt, LocalDateTime closedAt) {
        jdbc.update("UPDATE alerts SET status=?, resolution_notes=?, acknowledged_at=?, closed_at=? WHERE id=?",
                newStatus.name(), resolutionNotes,
                acknowledgedAt != null ? Timestamp.valueOf(acknowledgedAt) : null,
                closedAt != null ? Timestamp.valueOf(closedAt) : null,
                id);
    }

    public void saveAlertTransaction(Long alertId, Long transactionId) {
        jdbc.update("INSERT IGNORE INTO alert_transactions (alert_id, transaction_id) VALUES (?,?)", alertId, transactionId);
    }

    public List<Long> findTransactionIdsByAlertId(Long alertId) {
        return jdbc.queryForList("SELECT transaction_id FROM alert_transactions WHERE alert_id = ?", Long.class, alertId);
    }

    public void saveStatusHistory(AlertStatusHistory h) {
        jdbc.update("INSERT INTO alert_status_history (alert_id, previous_status, new_status, notes) VALUES (?,?,?,?)",
                h.getAlertId(), h.getPreviousStatus(), h.getNewStatus(), h.getNotes());
    }

    public List<AlertStatusHistory> findStatusHistoryByAlertId(Long alertId) {
        return jdbc.query("SELECT * FROM alert_status_history WHERE alert_id = ? ORDER BY changed_at ASC",
                historyRowMapper, alertId);
    }

    public boolean existsOpenAlertForRule(Long ruleId, String accountId) {
        String sql = """
                SELECT COUNT(*) FROM alerts a
                JOIN alert_transactions at2 ON a.id = at2.alert_id
                JOIN transactions t ON at2.transaction_id = t.id
                WHERE a.rule_id = ? AND a.status = 'OPEN' AND t.account_id = ?
                """;
        Integer count = jdbc.queryForObject(sql, Integer.class, ruleId, accountId);
        return count != null && count > 0;
    }

    public Map<String, Object> getStats() {
        List<Map<String, Object>> statusCounts = jdbc.queryForList(
                "SELECT status, COUNT(*) as count FROM alerts GROUP BY status");
        Map<String, Object> stats = new java.util.HashMap<>();
        for (Map<String, Object> row : statusCounts) {
            stats.put(row.get("status").toString().toLowerCase() + "Count", row.get("count"));
        }
        // Alerts today
        Integer today = jdbc.queryForObject(
                "SELECT COUNT(*) FROM alerts WHERE DATE(created_at) = CURDATE()", Integer.class);
        stats.put("alertsToday", today != null ? today : 0);

        // Average resolution time in minutes
        Double avgMinutes = jdbc.queryForObject(
                "SELECT AVG(TIMESTAMPDIFF(MINUTE, created_at, closed_at)) FROM alerts WHERE closed_at IS NOT NULL",
                Double.class);
        stats.put("avgResolutionMinutes", avgMinutes != null ? Math.round(avgMinutes) : 0);

        return stats;
    }
}
