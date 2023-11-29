/**
 * This class holds the peer-specific configuration settings.
 */
public class ConfigSettings {
    // Count of preferred neighbors for a peer
    public static int numberOfPreferredNeighbours;
    // Frequency of selecting preferred neighbors
    public static int unchokingInterval;
    // Frequency of optimistically unchoking neighbors
    public static int optimisticUnchokingInterval;
    // File name to be shared/transferred
    public static String fileName;
    // Total file size to be shared/transferred
    public static int fileSize;
    // Size of individual pieces the file is split into for transfer
    public static int pieceSize;
}
