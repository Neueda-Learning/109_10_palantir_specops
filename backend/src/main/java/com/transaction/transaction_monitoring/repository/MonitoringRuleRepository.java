package com.transaction.transaction_monitoring.repository;

import com.transaction.transaction_monitoring.enums.RuleType;
import com.transaction.transaction_monitoring.enums.Severity;
import com.transaction.transaction_monitoring.model.MonitoringRule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class MonitoringRuleRepository {

    private final JdbcTemplate jdbc;

    public MonitoringRuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<MonitoringRule> rowMapper = (rs, rn) -> {
        MonitoringRule r = new MonitoringRule();
        r.setId(rs.getLong("id"));
        r.setName(rs.getString("name"));
        r.setDescription(rs.getString("description"));
        r.setType(RuleType.valueOf(rs.getString("type")));
        r.setSeverity(Severity.valueOf(rs.getString("severity")));
        r.setActive(rs.getBoolean("active"));
        r.setThresholdAmount(rs.getBigDecimal("threshold_amount"));
        r.setTransactionCount(rs.getObject("transaction_count") != null ? rs.getInt("transaction_count") : null);
        r.setTimeWindowMinutes(rs.getObject("time_window_minutes") != null ? rs.getInt("time_window_minutes") : null);
        r.setDailyLimit(rs.getBigDecimal("daily_limit"));
        Timestamp ca = rs.getTimestamp("created_at");
        r.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        Timestamp ua = rs.getTimestamp("updated_at");
        r.setUpdatedAt(ua != null ? ua.toLocalDateTime() : null);
        return r;
    };

    public List<MonitoringRule> findAll() {
        return jdbc.query("SELECT * FROM monitoring_rules ORDER BY created_at DESC", rowMapper);
    }

    public List<MonitoringRule> findAllActive() {
        return jdbc.query("SELECT * FROM monitoring_rules WHERE active = TRUE ORDER BY created_at DESC", rowMapper);
    }

    public Optional<MonitoringRule> findById(Long id) {
        List<MonitoringRule> results = jdbc.query("SELECT * FROM monitoring_rules WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public MonitoringRule save(MonitoringRule r) {
        String sql = "INSERT INTO monitoring_rules (name, description, type, severity, active, threshold_amount, transaction_count, time_window_minutes, daily_limit) VALUES (?,?,?,?,?,?,?,?,?)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, r.getName());
            ps.setString(2, r.getDescription());
            ps.setString(3, r.getType().name());
            ps.setString(4, r.getSeverity().name());
            ps.setBoolean(5, r.isActive());
            ps.setBigDecimal(6, r.getThresholdAmount());
            if (r.getTransactionCount() != null) ps.setInt(7, r.getTransactionCount());
            else ps.setNull(7, java.sql.Types.INTEGER);
            if (r.getTimeWindowMinutes() != null) ps.setInt(8, r.getTimeWindowMinutes());
            else ps.setNull(8, java.sql.Types.INTEGER);
            ps.setBigDecimal(9, r.getDailyLimit());
            return ps;
        }, key);
        r.setId(key.getKey().longValue());
        return r;
    }

    public void update(MonitoringRule r) {
        String sql = "UPDATE monitoring_rules SET name=?, description=?, type=?, severity=?, active=?, threshold_amount=?, transaction_count=?, time_window_minutes=?, daily_limit=? WHERE id=?";
        jdbc.update(sql, r.getName(), r.getDescription(), r.getType().name(), r.getSeverity().name(),
                r.isActive(), r.getThresholdAmount(), r.getTransactionCount(),
                r.getTimeWindowMinutes(), r.getDailyLimit(), r.getId());
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM monitoring_rules WHERE id = ?", id);
    }

    public void setActive(Long id, boolean active) {
        jdbc.update("UPDATE monitoring_rules SET active = ? WHERE id = ?", active, id);
    }
}
