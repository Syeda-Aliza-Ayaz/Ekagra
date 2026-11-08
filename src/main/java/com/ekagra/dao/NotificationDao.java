package com.ekagra.dao;

import com.ekagra.model.Notification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class NotificationDao extends BaseDao implements Dao<Notification> {

    @Override
    public Optional<Notification> findById(int id) {
        return queryOne("SELECT * FROM notifications WHERE id = ?", this::map, id);
    }

    public List<Notification> findRecent(int userId, int limit) {
        return query("SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ?",
            this::map, userId, limit);
    }

    public int countUnread(int userId) {
        return (int) queryLong("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0", userId);
    }

    /** Whether a notification of this type whose message contains {@code text} was already created today. */
    public boolean existsToday(int userId, Notification.Type type, String text) {
        return queryLong("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ? AND message LIKE ? "
            + "AND created_at >= ?", userId, type, "%" + text + "%", java.time.LocalDate.now().atStartOfDay()) > 0;
    }

    public void markAllRead(int userId) {
        update("UPDATE notifications SET is_read = 1 WHERE user_id = ?", userId);
    }

    public void markRead(int id) {
        update("UPDATE notifications SET is_read = 1 WHERE id = ?", id);
    }

    @Override
    public int insert(Notification n) {
        int id = insert("""
            INSERT INTO notifications (user_id, sender_id, type, title, message, is_read, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)""",
            n.getUserId(), n.getSenderId(), n.getType(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt());
        n.setId(id);
        return id;
    }

    @Override
    public void update(Notification n) {
        update("UPDATE notifications SET is_read = ? WHERE id = ?", n.isRead(), n.getId());
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM notifications WHERE id = ?", id);
    }

    public void deleteAll(int userId) {
        update("DELETE FROM notifications WHERE user_id = ?", userId);
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification(rs.getInt("user_id"), Notification.Type.valueOf(rs.getString("type")),
            rs.getString("title"), rs.getString("message"));
        n.setId(rs.getInt("id"));
        n.setSenderId(nullableInt(rs, "sender_id"));
        n.setRead(rs.getInt("is_read") == 1);
        n.setCreatedAt(toDateTime(rs.getString("created_at")));
        return n;
    }
}
