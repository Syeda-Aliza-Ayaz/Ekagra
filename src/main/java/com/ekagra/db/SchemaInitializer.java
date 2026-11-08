package com.ekagra.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates every table the system needs and seeds default data (app rules, badges). */
public class SchemaInitializer {

    private static final String[] TABLES = {
        """
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            role TEXT NOT NULL CHECK (role IN ('STUDENT','MENTOR')),
            full_name TEXT NOT NULL,
            email TEXT NOT NULL UNIQUE COLLATE NOCASE,
            password_hash TEXT NOT NULL,
            salt TEXT NOT NULL,
            avatar_color TEXT NOT NULL DEFAULT '#226192',
            university TEXT,
            semester INTEGER,
            daily_target_min INTEGER NOT NULL DEFAULT 240,
            xp INTEGER NOT NULL DEFAULT 0,
            invite_code TEXT UNIQUE,
            created_at TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS mentor_links (
            student_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
            mentor_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            linked_at TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS tasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            title TEXT NOT NULL,
            description TEXT,
            subject TEXT,
            priority TEXT NOT NULL DEFAULT 'MEDIUM',
            status TEXT NOT NULL DEFAULT 'TODO',
            deadline TEXT,
            estimated_min INTEGER NOT NULL DEFAULT 0,
            spent_min INTEGER NOT NULL DEFAULT 0,
            created_at TEXT NOT NULL,
            completed_at TEXT
        )""",
        """
        CREATE TABLE IF NOT EXISTS subtasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            task_id INTEGER NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
            title TEXT NOT NULL,
            done INTEGER NOT NULL DEFAULT 0
        )""",
        """
        CREATE TABLE IF NOT EXISTS focus_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            task_id INTEGER REFERENCES tasks(id) ON DELETE SET NULL,
            type TEXT NOT NULL,
            started_at TEXT NOT NULL,
            ended_at TEXT,
            planned_min INTEGER NOT NULL,
            actual_sec INTEGER NOT NULL DEFAULT 0,
            completed INTEGER NOT NULL DEFAULT 0,
            distractions INTEGER NOT NULL DEFAULT 0,
            focus_rating INTEGER
        )""",
        """
        CREATE TABLE IF NOT EXISTS activity_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            app_name TEXT NOT NULL,
            window_title TEXT,
            category TEXT NOT NULL,
            started_at TEXT NOT NULL,
            ended_at TEXT NOT NULL,
            duration_sec INTEGER NOT NULL,
            idle INTEGER NOT NULL DEFAULT 0
        )""",
        "CREATE INDEX IF NOT EXISTS idx_activity_user_time ON activity_logs(user_id, started_at)",
        """
        CREATE TABLE IF NOT EXISTS app_rules (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            process_name TEXT,
            title_keyword TEXT,
            category TEXT NOT NULL,
            created_by INTEGER REFERENCES users(id) ON DELETE SET NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS screen_limits (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            app_name TEXT,
            limit_min INTEGER NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS goals (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            title TEXT NOT NULL,
            type TEXT NOT NULL,
            period TEXT NOT NULL,
            target_value INTEGER NOT NULL,
            active INTEGER NOT NULL DEFAULT 1,
            created_at TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS habits (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            title TEXT NOT NULL,
            icon TEXT,
            active INTEGER NOT NULL DEFAULT 1,
            created_at TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS habit_checks (
            habit_id INTEGER NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
            date TEXT NOT NULL,
            PRIMARY KEY (habit_id, date)
        )""",
        """
        CREATE TABLE IF NOT EXISTS mood_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            date TEXT NOT NULL,
            mood INTEGER NOT NULL,
            stress INTEGER NOT NULL,
            note TEXT,
            UNIQUE (user_id, date)
        )""",
        """
        CREATE TABLE IF NOT EXISTS sleep_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            date TEXT NOT NULL,
            bedtime TEXT NOT NULL,
            wake_time TEXT NOT NULL,
            duration_min INTEGER NOT NULL,
            UNIQUE (user_id, date)
        )""",
        """
        CREATE TABLE IF NOT EXISTS badges (
            code TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            description TEXT NOT NULL,
            icon TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS user_badges (
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            badge_code TEXT NOT NULL REFERENCES badges(code),
            earned_at TEXT NOT NULL,
            PRIMARY KEY (user_id, badge_code)
        )""",
        """
        CREATE TABLE IF NOT EXISTS notifications (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            sender_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
            type TEXT NOT NULL,
            title TEXT NOT NULL,
            message TEXT NOT NULL,
            is_read INTEGER NOT NULL DEFAULT 0,
            created_at TEXT NOT NULL
        )""",
        """
        CREATE TABLE IF NOT EXISTS settings (
            user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            key TEXT NOT NULL,
            value TEXT NOT NULL,
            PRIMARY KEY (user_id, key)
        )"""
    };

    /** process_name, title_keyword, category. A null process means "any app whose title contains keyword". */
    private static final String[][] DEFAULT_RULES = {
        // Productive apps
        {"idea64.exe", null, "PRODUCTIVE"}, {"code.exe", null, "PRODUCTIVE"},
        {"devenv.exe", null, "PRODUCTIVE"}, {"eclipse.exe", null, "PRODUCTIVE"},
        {"netbeans64.exe", null, "PRODUCTIVE"}, {"winword.exe", null, "PRODUCTIVE"},
        {"excel.exe", null, "PRODUCTIVE"}, {"powerpnt.exe", null, "PRODUCTIVE"},
        {"onenote.exe", null, "PRODUCTIVE"}, {"acrord32.exe", null, "PRODUCTIVE"},
        {"notion.exe", null, "PRODUCTIVE"}, {"obsidian.exe", null, "PRODUCTIVE"},
        {"java.exe", "Ekagra", "NEUTRAL"},
        // Productive websites (matched on browser window titles)
        {null, "Stack Overflow", "PRODUCTIVE"}, {null, "GitHub", "PRODUCTIVE"},
        {null, "Google Docs", "PRODUCTIVE"}, {null, "Classroom", "PRODUCTIVE"},
        {null, "Coursera", "PRODUCTIVE"}, {null, "Udemy", "PRODUCTIVE"},
        {null, "LeetCode", "PRODUCTIVE"}, {null, "GeeksforGeeks", "PRODUCTIVE"},
        {null, "W3Schools", "PRODUCTIVE"}, {null, "Wikipedia", "PRODUCTIVE"},
        {null, "Overleaf", "PRODUCTIVE"},
        // Distracting websites
        {null, "YouTube", "DISTRACTING"}, {null, "Netflix", "DISTRACTING"},
        {null, "Instagram", "DISTRACTING"}, {null, "Facebook", "DISTRACTING"},
        {null, "TikTok", "DISTRACTING"}, {null, "Reddit", "DISTRACTING"},
        {null, "Twitch", "DISTRACTING"}, {null, "/ X", "DISTRACTING"},
        // Distracting apps
        {"steam.exe", null, "DISTRACTING"}, {"discord.exe", null, "DISTRACTING"},
        {"whatsapp.exe", null, "DISTRACTING"}, {"epicgameslauncher.exe", null, "DISTRACTING"},
        {"valorant.exe", null, "DISTRACTING"}
    };

    private static final String[][] DEFAULT_BADGES = {
        {"FIRST_FOCUS", "First Focus", "Complete your first focus session", "fth-zap"},
        {"FOCUS_10", "In the Zone", "Complete 10 focus sessions", "fth-crosshair"},
        {"FOCUS_50", "Focus Master", "Complete 50 focus sessions", "fth-star"},
        {"STREAK_7", "7-Day Streak", "Hit your study target 7 days in a row", "fth-trending-up"},
        {"DEADLINE_CRUSHER", "Deadline Crusher", "Finish 5 tasks before their deadline", "fth-check-circle"},
        {"TASK_MASTER", "Task Master", "Complete 25 tasks", "fth-award"},
        {"DIGITAL_DETOX", "Digital Detox", "A full day under 30 min of distracting apps", "fth-feather"},
        {"EARLY_BIRD", "Early Bird", "Start a focus session before 8 AM", "fth-sunrise"},
        {"NIGHT_OWL", "Night Owl", "Finish a focus session after 11 PM", "fth-moon"},
        {"HABIT_HERO", "Habit Hero", "Keep a habit going for 7 days", "fth-repeat"},
        {"ZEN", "Zen Mode", "Log your mood 7 days in a row", "fth-smile"}
    };

    private final Connection connection;

    public SchemaInitializer(Connection connection) {
        this.connection = connection;
    }

    public void initialize() throws SQLException {
        try (Statement st = connection.createStatement()) {
            for (String sql : TABLES) {
                st.execute(sql);
            }
        }
        if (isEmpty("app_rules")) {
            seedRules();
        }
        seedBadges();
    }

    private boolean isEmpty(String table) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.getInt(1) == 0;
        }
    }

    private void seedRules() throws SQLException {
        String sql = "INSERT INTO app_rules (process_name, title_keyword, category) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String[] r : DEFAULT_RULES) {
                ps.setString(1, r[0]);
                ps.setString(2, r[1]);
                ps.setString(3, r[2]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedBadges() throws SQLException {
        String sql = "INSERT OR IGNORE INTO badges (code, name, description, icon) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String[] b : DEFAULT_BADGES) {
                for (int i = 0; i < 4; i++) {
                    ps.setString(i + 1, b[i]);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
