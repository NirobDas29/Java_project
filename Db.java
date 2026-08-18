import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/*
 * Db.java - Central SQLite connection point and schema setup.
 * All persistence (movies, bookings, payments, users, reviews, waitlist)
 * lives in one file: cinema.db, created automatically on first run.
 *
 * The only thing that stays as a plain text file, on request, is the
 * running booking_history.txt append-only log (see BookingService).
 */
public class Db {
    private static final String DB_URL = "jdbc:sqlite:cinema.db";
    private static Connection connection;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found on classpath. "
                    + "Add sqlite-jdbc.jar to the classpath when compiling/running.", e);
        }
    }

    public static synchronized Connection get() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                connection.createStatement().execute("PRAGMA foreign_keys = ON");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not open database connection", e);
        }
        return connection;
    }

    public static void initSchema() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS movies (" +
                "movie_id INTEGER PRIMARY KEY," +
                "movie_name TEXT NOT NULL," +
                "ticket_price REAL NOT NULL," +
                "image_path TEXT," +
                "blockbuster INTEGER NOT NULL DEFAULT 0," +
                "category TEXT NOT NULL," +
                "show_times TEXT NOT NULL" + // comma-separated
            ")",

            "CREATE TABLE IF NOT EXISTS users (" +
                "user_name TEXT PRIMARY KEY," +
                "password_hash TEXT NOT NULL," +
                "email TEXT," +
                "phone_number TEXT," +
                "is_admin INTEGER NOT NULL DEFAULT 0" +
            ")",

            "CREATE TABLE IF NOT EXISTS bookings (" +
                "booking_id INTEGER PRIMARY KEY," +
                "group_id INTEGER NOT NULL," +
                "user_name TEXT NOT NULL," +
                "movie_name TEXT NOT NULL," +
                "hall_name TEXT NOT NULL," +
                "seat_number TEXT NOT NULL," +
                "seat_type TEXT NOT NULL," +
                "show_time TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "status TEXT NOT NULL," +
                "booking_date TEXT NOT NULL" +
            ")",

            "CREATE TABLE IF NOT EXISTS payments (" +
                "payment_id INTEGER PRIMARY KEY," +
                "group_id INTEGER NOT NULL," +
                "user_name TEXT NOT NULL," +
                "movie_name TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "seat_count INTEGER NOT NULL," +
                "seat_list TEXT," +
                "method TEXT NOT NULL," +
                "mobile_number TEXT," +
                "transaction_id TEXT," +
                "payment_date TEXT NOT NULL," +
                "status TEXT NOT NULL" +
            ")",

            "CREATE TABLE IF NOT EXISTS reviews (" +
                "review_id INTEGER PRIMARY KEY," +
                "movie_name TEXT NOT NULL," +
                "user_name TEXT NOT NULL," +
                "rating INTEGER NOT NULL," +
                "comment TEXT," +
                "review_date TEXT NOT NULL" +
            ")",

            "CREATE TABLE IF NOT EXISTS waitlist (" +
                "waitlist_id INTEGER PRIMARY KEY," +
                "user_name TEXT NOT NULL," +
                "movie_name TEXT NOT NULL," +
                "hall_name TEXT NOT NULL," +
                "show_time TEXT NOT NULL," +
                "show_date TEXT NOT NULL," +
                "seats_wanted INTEGER NOT NULL," +
                "notified INTEGER NOT NULL DEFAULT 0," +
                "request_date TEXT NOT NULL" +
            ")"
        };

        try (Statement st = get().createStatement()) {
            for (String sql : ddl) st.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}