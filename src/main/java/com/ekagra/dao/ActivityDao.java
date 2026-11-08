package com.ekagra.dao;

import com.ekagra.model.ActivityLog;
import com.ekagra.model.Category;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ActivityDao extends BaseDao implements Dao<ActivityLog> {

    /** Total time in one app over a period. */
    public record AppUsage(String appName, Category category, int seconds) {
    }

    /** Seconds per category within one hour of the day (0..23). */
    public record HourUsage(int hour, Category category, int seconds) {
    }

    @Override
    public Optional<ActivityLog> findById(int id) {
        return queryOne("SELECT * FROM activity_logs WHERE id = ?", this::map, id);
    }

    public List<ActivityLog> findBetween(int userId, LocalDateTime from, LocalDateTime to) {
        return query("SELECT * FROM activity_logs WHERE user_id = ? AND started_at >= ? AND started_at < ? "
            + "ORDER BY started_at", this::map, userId, from, to);
    }

    public List<ActivityLog> findRecent(int userId, int limit) {
        return query("SELECT * FROM activity_logs WHERE user_id = ? ORDER BY started_at DESC LIMIT ?",
            this::map, userId, limit);
    }

    public Map<Category, Integer> secondsByCategory(int userId, LocalDateTime from, LocalDateTime to) {
        Map<Category, Integer> out = new EnumMap<>(Category.class);
        for (Category c : Category.values()) {
            out.put(c, 0);
        }
        query("""
            SELECT category, SUM(duration_sec) FROM activity_logs
            WHERE user_id = ? AND idle = 0 AND started_at >= ? AND started_at < ?
            GROUP BY category""", rs -> {
            out.put(Category.valueOf(rs.getString(1)), rs.getInt(2));
            return null;
        }, userId, from, to);
        return out;
    }

    /** Apps ranked by time used. When an app has mixed categories, its dominant one is reported. */
    public List<AppUsage> topApps(int userId, LocalDateTime from, LocalDateTime to, int limit) {
        return query("""
            SELECT app_name,
                   (SELECT category FROM activity_logs a2
                     WHERE a2.user_id = a.user_id AND a2.app_name = a.app_name
                       AND a2.started_at >= ? AND a2.started_at < ?
                     GROUP BY category ORDER BY SUM(duration_sec) DESC LIMIT 1) AS cat,
                   SUM(duration_sec) AS total
            FROM activity_logs a
            WHERE user_id = ? AND idle = 0 AND started_at >= ? AND started_at < ?
            GROUP BY app_name ORDER BY total DESC LIMIT ?""",
            rs -> new AppUsage(rs.getString(1), Category.valueOf(rs.getString(2)), rs.getInt(3)),
            from, to, userId, from, to, limit);
    }

    /**
     * Seconds spent on an app today, matching either the process name ("discord.exe")
     * or a word in the window title ("Instagram" inside Chrome). Used for per-app limits.
     */
    public int secondsMatching(int userId, String appOrKeyword, LocalDateTime from, LocalDateTime to) {
        String key = appOrKeyword.trim();
        return (int) queryLong("""
            SELECT COALESCE(SUM(duration_sec), 0) FROM activity_logs
            WHERE user_id = ? AND idle = 0 AND started_at >= ? AND started_at < ?
              AND (app_name = ? COLLATE NOCASE OR app_name = ? COLLATE NOCASE OR window_title LIKE ?)""",
            userId, from, to, key, key + ".exe", "%" + key + "%");
    }

    public int totalSeconds(int userId, LocalDateTime from, LocalDateTime to) {
        return (int) queryLong("SELECT COALESCE(SUM(duration_sec), 0) FROM activity_logs "
            + "WHERE user_id = ? AND idle = 0 AND started_at >= ? AND started_at < ?", userId, from, to);
    }

    public List<HourUsage> hourly(int userId, LocalDateTime from, LocalDateTime to) {
        return query("""
            SELECT CAST(substr(started_at, 12, 2) AS INTEGER) AS hr, category, SUM(duration_sec)
            FROM activity_logs
            WHERE user_id = ? AND idle = 0 AND started_at >= ? AND started_at < ?
            GROUP BY hr, category ORDER BY hr""",
            rs -> new HourUsage(rs.getInt(1), Category.valueOf(rs.getString(2)), rs.getInt(3)),
            userId, from, to);
    }

    @Override
    public int insert(ActivityLog a) {
        int id = insert("""
            INSERT INTO activity_logs (user_id, app_name, window_title, category, started_at, ended_at, duration_sec, idle)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0)""",
            a.getUserId(), a.getAppName(), a.getWindowTitle(), a.getCategory(), a.getStartedAt(), a.getEndedAt(),
            a.getDurationSec());
        a.setId(id);
        return id;
    }

    @Override
    public void update(ActivityLog a) {
        update("UPDATE activity_logs SET window_title = ?, ended_at = ?, duration_sec = ? WHERE id = ?",
            a.getWindowTitle(), a.getEndedAt(), a.getDurationSec(), a.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM activity_logs WHERE id = ?", id);
    }

    private ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog a = new ActivityLog();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setAppName(rs.getString("app_name"));
        a.setWindowTitle(rs.getString("window_title"));
        a.setCategory(Category.valueOf(rs.getString("category")));
        a.setStartedAt(toDateTime(rs.getString("started_at")));
        a.setEndedAt(toDateTime(rs.getString("ended_at")));
        a.setDurationSec(rs.getInt("duration_sec"));
        return a;
    }
}
