import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Responsible for custom formatting of log messages.
 */
public class LoggingFormatter extends Formatter {

    // Defines the date and time format to be used in log messages
    private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

    /**
     * Formats the log record's message.
     *
     * @param record the log record to be formatted
     * @return a string representing the formatted log message
     */
    @Override
    public String format(LogRecord record) {
        return formatLogMessage(record.getMessage());
    }

    /**
     * Constructs a formatted log message string. The message includes the
     * current date and time, and a custom prefix.
     *
     * @param message the raw log message
     * @return a string representing the formatted log message
     */
    public static String formatLogMessage(String message) {
        StringBuilder formattedMessage = new StringBuilder();

        // Append current date and time to the message
        formattedMessage.append(dateTimeFormatter.format(LocalDateTime.now()));
        formattedMessage.append(": ");

        // Append a custom prefix to identify the message source
        formattedMessage.append("Peer ");

        // Append the actual log message
        formattedMessage.append(message);
        formattedMessage.append("\n");

        return formattedMessage.toString();
    }
}
