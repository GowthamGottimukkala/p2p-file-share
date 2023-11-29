import java.nio.ByteBuffer;

/**
 * A utility class that provides common functionalities.
 */
public class PeerUtils {

    /**
     * Converts an integer value to a byte array.
     *
     * @param number the integer to be converted.
     * @return a byte array representing the integer.
     */
    public static byte[] integerToBytes(int number) {
        // Allocates a ByteBuffer of size 4, puts the integer into it, and then returns the array.
        return ByteBuffer.allocate(4).putInt(number).array();
    }

    /**
     * Converts a byte array to an integer.
     *
     * @param byteArray the byte array to be converted.
     * @return an integer representing the byte array.
     */
    public static int bytesToInteger(byte[] byteArray) {
        // Wraps the byte array into a ByteBuffer and gets the integer.
        return ByteBuffer.wrap(byteArray).getInt();
    }
}

