package com.ekagra.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SINGLETON pattern: the whole application shares exactly one SQLite connection.
 * The database file is created (with all tables) on first launch.
 */
public final class DatabaseManager {

    public static final String DEFAULT_PATH = "data/ekagra.db";
    public static final String IN_MEMORY = ":memory:";

    private static DatabaseManager instance;
    private static String dbPath = defaultPath();

    /**
     * Development: data/ekagra.db in the project folder.
     * Packaged Ekagra.exe: a data folder next to the exe (jpackage sets "jpackage.app-path"),
     * so it works no matter where it is started from.
     */
    private static String defaultPath() {
        String exe = System.getProperty("jpackage.app-path");
        if (exe != null) {
            Path dir = Path.of(exe).toAbsolutePath().getParent();
            if (dir != null) {
                return dir.resolve("data").resolve("ekagra.db").toString();
            }
        }
        return DEFAULT_PATH;
    }

    private final Connection connection;

    private DatabaseManager() {
        try {
            String url;
            if (IN_MEMORY.equals(dbPath)) {
                url = "jdbc:sqlite::memory:";
            } else {
                Path file = Path.of(dbPath).toAbsolutePath();
                if (file.getParent() != null) {
                    Files.createDirectories(file.getParent());
                }
                url = "jdbc:sqlite:" + file;
            }
            connection = DriverManager.getConnection(url);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
                st.execute("PRAGMA busy_timeout = 5000");
                if (!IN_MEMORY.equals(dbPath)) {
                    st.execute("PRAGMA journal_mode = WAL");
                }
            }
            new SchemaInitializer(connection).initialize();
        } catch (SQLException | IOException e) {
            throw new DataAccessException("Could not open database at " + dbPath, e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /** Choose a different database file (used by tests). Must be called before first use. */
    public static synchronized void configure(String path) {
        if (instance != null) {
            throw new IllegalStateException("Database already opened; call shutdown() first");
        }
        dbPath = path;
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            try {
                instance.connection.close();
            } catch (SQLException ignored) {
                // closing on exit; nothing useful to do
            }
            instance = null;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Runs many writes as ONE transaction (much faster for bulk inserts like demo data,
     * and all-or-nothing if something fails).
     */
    public void inTransaction(Runnable work) {
        synchronized (connection) {
            try {
                connection.setAutoCommit(false);
                try {
                    work.run();
                    connection.commit();
                } catch (RuntimeException e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new DataAccessException("Transaction failed", e);
            }
        }
    }
}
