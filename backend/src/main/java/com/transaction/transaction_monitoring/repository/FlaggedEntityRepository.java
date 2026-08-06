package com.transaction.transaction_monitoring.repository;

import com.transaction.transaction_monitoring.enums.Severity;
import com.transaction.transaction_monitoring.model.FlaggedEntity;
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
public class FlaggedEntityRepository {

    private final JdbcTemplate jdbc;

    public FlaggedEntityRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<FlaggedEntity> rowMapper = (rs, rn) -> {
        FlaggedEntity e = new FlaggedEntity();
        e.setId(rs.getLong("id"));
        e.setEntityName(rs.getString("entity_name"));
        e.setPayeeId(rs.getString("payee_id"));
        e.setReason(rs.getString("reason"));
        e.setRiskLevel(Severity.valueOf(rs.getString("risk_level")));
        e.setActive(rs.getBoolean("active"));
        Timestamp ca = rs.getTimestamp("created_at");
        e.setCreatedAt(ca != null ? ca.toLocalDateTime() : null);
        return e;
    };

    public List<FlaggedEntity> findAll() {
        return jdbc.query("SELECT * FROM flagged_entities ORDER BY created_at DESC", rowMapper);
    }

    public Optional<FlaggedEntity> findById(Long id) {
        List<FlaggedEntity> results = jdbc.query("SELECT * FROM flagged_entities WHERE id = ?", rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<FlaggedEntity> findByPayeeId(String payeeId) {
        List<FlaggedEntity> results = jdbc.query("SELECT * FROM flagged_entities WHERE payee_id = ?", rowMapper, payeeId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<FlaggedEntity> findActiveByPayeeId(String payeeId) {
        List<FlaggedEntity> results = jdbc.query(
                "SELECT * FROM flagged_entities WHERE payee_id = ? AND active = TRUE", rowMapper, payeeId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public FlaggedEntity save(FlaggedEntity e) {
        String sql = "INSERT INTO flagged_entities (entity_name, payee_id, reason, risk_level, active) VALUES (?,?,?,?,?)";
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, e.getEntityName());
            ps.setString(2, e.getPayeeId());
            ps.setString(3, e.getReason());
            ps.setString(4, e.getRiskLevel().name());
            ps.setBoolean(5, e.isActive());
            return ps;
        }, key);
        e.setId(key.getKey().longValue());
        return e;
    }

    public void update(FlaggedEntity e) {
        jdbc.update("UPDATE flagged_entities SET entity_name=?, reason=?, risk_level=?, active=? WHERE id=?",
                e.getEntityName(), e.getReason(), e.getRiskLevel().name(), e.isActive(), e.getId());
    }

    public void setActive(Long id, boolean active) {
        jdbc.update("UPDATE flagged_entities SET active = ? WHERE id = ?", active, id);
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM flagged_entities WHERE id = ?", id);
    }
}
