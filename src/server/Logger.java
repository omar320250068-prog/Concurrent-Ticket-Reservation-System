package server;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ═══════════════════════════════════════════════════════════════════
 *  Logger Class (Real-time Server Event Logging)
 * ═══════════════════════════════════════════════════════════════════
 *  BONUS FEATURE: Real-time Logging
 *
 *  This singleton class provides thread-safe logging of all server
 *  events including:
 *  - Client connections and disconnections
 *  - Booking confirmations and cancellations
 *  - Errors and exceptions
 *  - Server startup/shutdown events
 *
 *  All events are written to 'server.log' with timestamps.
 *  The class uses synchronized methods to ensure thread safety
 *  when multiple ClientHandler threads log simultaneously.
 *
 *  Design Pattern: Singleton (only one Logger instance exists)
 * ═══════════════════════════════════════════════════════════════════
 */
public class Logger {

    // Singleton instance
    private static Logger instance;

    // Log file path
    private static final String LOG_FILE = "data/server.log";

    // Date-time formatter for log entries
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Log severity levels for categorizing events.
     */
    public enum Level {
        INFO,       // General information (connections, bookings)
        WARNING,    // Potential issues (seat conflicts, invalid requests)
        ERROR,      // Errors and exceptions
        SUCCESS     // Successful operations (bookings confirmed)
    }

    // ── Private Constructor (Singleton) ─────────────────────────

    private Logger() {
        // Ensure the data directory exists
        File logFile = new File(LOG_FILE);
        logFile.getParentFile().mkdirs();
    }

    // ── Singleton Access ────────────────────────────────────────

    /**
     * Returns the singleton Logger instance.
     * Double-checked locking for thread-safe lazy initialization.
     *
     * @return the Logger instance
     */
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    // ── Logging Methods ─────────────────────────────────────────

    /**
     * Logs a message with the specified severity level.
     * This method is synchronized to prevent interleaved writes
     * from multiple threads.
     *
     * @param level   the severity level
     * @param message the log message
     */
    public synchronized void log(Level level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format("[%s] [%-7s] %s", timestamp, level, message);

        // Print to console (server terminal)
        System.out.println(logEntry);

        // Append to log file
        try (FileWriter fw = new FileWriter(LOG_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(logEntry);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to write to log file: " + e.getMessage());
        }
    }

    // ── Convenience Methods ─────────────────────────────────────

    /**
     * Logs an informational message.
     */
    public void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Logs a warning message.
     */
    public void warning(String message) {
        log(Level.WARNING, message);
    }

    /**
     * Logs an error message.
     */
    public void error(String message) {
        log(Level.ERROR, message);
    }

    /**
     * Logs a success message.
     */
    public void success(String message) {
        log(Level.SUCCESS, message);
    }

    /**
     * Logs an error with exception details.
     *
     * @param message the error context
     * @param e       the exception
     */
    public void error(String message, Exception e) {
        log(Level.ERROR, message + " | Exception: " + e.getMessage());
    }

    /**
     * Logs a formatted separator line for visual clarity.
     */
    public void logSeparator() {
        log(Level.INFO, "════════════════════════════════════════════════════════");
    }
}
