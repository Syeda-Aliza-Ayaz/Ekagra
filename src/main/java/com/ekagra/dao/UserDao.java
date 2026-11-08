package com.ekagra.dao;

import com.ekagra.model.Mentor;
import com.ekagra.model.Role;
import com.ekagra.model.Student;
import com.ekagra.model.User;
import com.ekagra.pattern.factory.UserFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDao implements Dao<User> {

    private static final String SELECT =
        "SELECT u.*, ml.mentor_id FROM users u LEFT JOIN mentor_links ml ON ml.student_id = u.id ";

    @Override
    public Optional<User> findById(int id) {
        return queryOne(SELECT + "WHERE u.id = ?", this::map, id);
    }

    public Optional<User> findByEmail(String email) {
        return queryOne(SELECT + "WHERE u.email = ?", this::map, email.trim());
    }

    public Optional<Mentor> findMentorByInviteCode(String code) {
        return queryOne(SELECT + "WHERE u.role = 'MENTOR' AND u.invite_code = ?", this::map, code.trim().toUpperCase())
            .map(Mentor.class::cast);
    }

    public boolean emailExists(String email) {
        return queryLong("SELECT COUNT(*) FROM users WHERE email = ?", email.trim()) > 0;
    }

    public List<Student> findStudentsOfMentor(int mentorId) {
        return query(SELECT + "WHERE ml.mentor_id = ? ORDER BY u.full_name", this::map, mentorId)
            .stream().map(Student.class::cast).toList();
    }

    @Override
    public int insert(User u) {
        String university = null;
        Integer semester = null;
        int target = 240;
        String invite = null;
        if (u instanceof Student s) {
            university = s.getUniversity();
            semester = s.getSemester();
            target = s.getDailyTargetMin();
        } else if (u instanceof Mentor m) {
            invite = m.getInviteCode();
        }
        int id = insert("""
            INSERT INTO users (role, full_name, email, password_hash, salt, avatar_color,
                               university, semester, daily_target_min, invite_code, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            u.getRole(), u.getFullName(), u.getEmail().trim(), u.getPasswordHash(), u.getSalt(),
            u.getAvatarColor(), university, semester, target, invite, u.getCreatedAt());
        u.setId(id);
        return id;
    }

    /** Updates profile fields (not password, not XP). */
    @Override
    public void update(User u) {
        if (u instanceof Student s) {
            update("""
                UPDATE users SET full_name = ?, avatar_color = ?, university = ?, semester = ?, daily_target_min = ?
                WHERE id = ?""",
                s.getFullName(), s.getAvatarColor(), s.getUniversity(), s.getSemester(), s.getDailyTargetMin(), s.getId());
        } else {
            update("UPDATE users SET full_name = ?, avatar_color = ? WHERE id = ?",
                u.getFullName(), u.getAvatarColor(), u.getId());
        }
    }

    @Override
    public void delete(int id) {
        update("DELETE FROM users WHERE id = ?", id);
    }

    public void updatePassword(int userId, String hash, String salt) {
        update("UPDATE users SET password_hash = ?, salt = ? WHERE id = ?", hash, salt, userId);
    }

    public void addXp(int userId, int amount) {
        update("UPDATE users SET xp = xp + ? WHERE id = ?", amount, userId);
    }

    public void linkStudentToMentor(int studentId, int mentorId) {
        update("INSERT OR REPLACE INTO mentor_links (student_id, mentor_id, linked_at) VALUES (?, ?, ?)",
            studentId, mentorId, LocalDateTime.now());
    }

    public void unlinkStudent(int studentId) {
        update("DELETE FROM mentor_links WHERE student_id = ?", studentId);
    }

    /** Maps one DB row to the correct subclass, chosen by the factory from the 'role' column. */
    private User map(ResultSet rs) throws SQLException {
        User u = UserFactory.create(Role.valueOf(rs.getString("role")));
        u.setId(rs.getInt("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setAvatarColor(rs.getString("avatar_color"));
        u.setCreatedAt(toDateTime(rs.getString("created_at")));
        if (u instanceof Student s) {
            s.setUniversity(rs.getString("university"));
            s.setSemester(rs.getInt("semester"));
            s.setDailyTargetMin(rs.getInt("daily_target_min"));
            s.setXp(rs.getInt("xp"));
            s.setMentorId(nullableInt(rs, "mentor_id"));
        } else if (u instanceof Mentor m) {
            m.setInviteCode(rs.getString("invite_code"));
        }
        return u;
    }
}
