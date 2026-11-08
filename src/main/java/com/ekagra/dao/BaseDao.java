package com.ekagra.dao;

import com.ekagra.db.DataAccessException;
import com.ekagra.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC helpers for all DAOs (removes repeated try/catch + PreparedStatement code).
 * All access is synchronized on the single connection because the activity tracker
 * writes from a background thread while the UI reads.
 */
public abstract class BaseDao {

    protected static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    protected Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        Connection c = conn();
        synchronized (c) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(mapper.map(rs));
                    }
                    return out;
                }
            } catch (SQLException e) {
                throw new DataAccessException("Query failed: " + sql, e);
            }
        }
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    protected long queryLong(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    protected int update(String sql, Object... params) {
        Connection c = conn();
        synchronized (c) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, params);
                return ps.executeUpdate();
            } catch (SQLException e) {
                throw new DataAccessException("Update failed: " + sql, e);
            }
        }
    }

    /** Runs an INSERT and returns the generated primary key. */
    protected int insert(String sql, Object... params) {
        Connection c = conn();
        synchronized (c) {
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : -1;
                }
            } catch (SQLException e) {
                throw new DataAccessException("Insert failed: " + sql, e);
            }
        }
    }

    private void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            if (p == null) {
                ps.setObject(idx, null);
            } else if (p instanceof LocalDateTime ldt) {
                ps.setString(idx, ldt.format(DATE_TIME));   // fixed width so string comparison = time comparison
            } else if (p instanceof LocalDate || p instanceof LocalTime) {
                ps.setString(idx, p.toString());
            } else if (p instanceof Enum<?> e) {
                ps.setString(idx, e.name());
            } else if (p instanceof Boolean b) {
                ps.setInt(idx, b ? 1 : 0);
            } else {
                ps.setObject(idx, p);
            }
        }
    }

    protected static LocalDateTime toDateTime(String s) {
        return s == null ? null : LocalDateTime.parse(s);
    }

    protected static LocalDate toDate(String s) {
        return s == null ? null : LocalDate.parse(s);
    }

    protected static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }
}
