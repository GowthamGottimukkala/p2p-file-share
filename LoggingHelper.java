import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * A utility class to facilitate logging. It initializes and configures log files,
 * enabling messages to be written into them.
 */
public class LoggingHelper {
    // A handler that directs log messages to a specified file.
    public FileHandler fileHandler;

    // A Logger instance used to log messages for this class.
    public static Logger log = Logger.getLogger(LoggingHelper.class.getName());

    /**
     * Initializes the logging configuration by creating a new log file with a
     * handler set to write messages into it.
     *
     * @param peerID - The unique identifier for the peer, used in naming the log file.
     */
    public void initializeLogger(String peerID) {
        try {
            fileHandler = new FileHandler("log_peer_" + peerID + ".log");
            fileHandler.setFormatter(new LoggingFormatter());
            log.addHandler(fileHandler);
            log.setUseParentHandlers(false);
        } catch (IOException e) {
            // Consider handling the exception, possibly logging it.
        }
    }

    /**
     * Logs a message to the log file and also displays it on the console.
     * The log message is formatted for consistency.
     *
     * @param message - The message to be logged and displayed on the console.
     */
    public static void logAndDisplay(String message) {
        log.info(message);
        System.out.println(LoggingFormatter.formatLogMessage(message));
    }
}
