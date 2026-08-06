package com.transaction.transaction_monitoring.repository;

import com.transaction.transaction_monitoring.enums.TransactionStatus;
import com.transaction.transaction_monitoring.enums.TransactionType;
import com.transaction.transaction_monitoring.model.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TransactionRepository {

    private final JdbcTemplate jdbc;

    public TransactionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Transaction> rowMapper = (rs, rn) -> {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setAccountId(rs.getString("account_id"));
        t.setPayeeId(rs.getString("payee_id"));
        t.setAmount(rs.getBigDecimal("amount"));
        t.setType(TransactionType.valueOf(rs.getString("type")));
        t.setStatus(TransactionStatus.valueOf(rs.getString("status")));
        t.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("timestamp");
        t.setTimestamp(ts != null ? ts.toLocalDateTime() : null);
        return t;
    };

    public Transaction save(Transaction t) {
        String sql = "INSERT INTO transactions (account_id, payee_id, amount, type, status, description, timestamp) VALUES (?,?,?,?,?,?,?)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, t.getAccountId());
            ps.setString(2, t.getPayeeId());
            ps.setBigDecimal(3, t.getAmount());
            ps.setString(4, t.getType().name());
            ps.setString(5, t.getStatus().name());
            ps.setString(6, t.getDescription());
            ps.setTimestamp(7, Timestamp.valueOf(t.getTimestamp() != null ? t.getTimestamp() : LocalDateTime.now()));
            return ps;
        }, key);
        t.setId(key.getKey().longValue());
        return t;
    }

    public Optional<Transaction> findById(Long id) {
        List<Transaction> results = jdbc.query("SELECT * FROM transactions WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Transaction> findAll(String accountId, LocalDateTime from, LocalDateTime to,
                                     BigDecimal minAmount, BigDecimal maxAmount, String search) {
        StringBuilder sql = new StringBuilder("SELECT * FROM transactions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (accountId != null && !accountId.isBlank()) {
            sql.append(" AND account_id = ?");
            params.add(accountId);
        }
        if (from != null) {
            sql.append(" AND timestamp >= ?");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND timestamp <= ?");
            params.add(Timestamp.valueOf(to));
        }
        if (minAmount != null) {
            sql.append(" AND amount >= ?");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            sql.append(" AND amount <= ?");
            params.add(maxAmount);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (description LIKE ? OR account_id LIKE ? OR payee_id LIKE ?)");
            String term = "%" + search + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }
        sql.append(" ORDER BY timestamp DESC");
        return jdbc.query(sql.toString(), rowMapper, params.toArray());
    }

    public int countByAccountInWindow(String accountId, int windowMinutes) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE account_id = ? AND timestamp > NOW() - INTERVAL ? MINUTE";
        Integer count = jdbc.queryForObject(sql, Integer.class, accountId, windowMinutes);
        return count != null ? count : 0;
    }

    public BigDecimal sumDebitsForAccountToday(String accountId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account_id = ? AND type = 'DEBIT' AND DATE(timestamp) = CURDATE()";
        BigDecimal sum = jdbc.queryForObject(sql, BigDecimal.class, accountId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public int countPreviousPayeeTransactions(String accountId, String payeeId, LocalDateTime before) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE account_id = ? AND payee_id = ? AND timestamp < ?";
        Integer count = jdbc.queryForObject(sql, Integer.class, accountId, payeeId, Timestamp.valueOf(before));
        return count != null ? count : 0;
    }

    public BigDecimal sumFlaggedPayeeDebits(String accountId, LocalDateTime from) {
        String sql = """
                SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
                JOIN flagged_entities f ON t.payee_id = f.payee_id
                WHERE t.account_id = ? AND f.active = TRUE AND t.type = 'DEBIT' AND t.timestamp >= ?
                """;
        BigDecimal sum = jdbc.queryForObject(sql, BigDecimal.class, accountId, Timestamp.valueOf(from));
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public BigDecimal sumDebitsInWindow(String accountId, LocalDateTime from) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account_id = ? AND type = 'DEBIT' AND timestamp >= ?";
        BigDecimal sum = jdbc.queryForObject(sql, BigDecimal.class, accountId, Timestamp.valueOf(from));
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public List<Transaction> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        return jdbc.query("SELECT * FROM transactions WHERE id IN (" + placeholders + ")", rowMapper, ids.toArray());
    }
}
