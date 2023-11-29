import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the peer-to-peer process for file transfer.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class peerProcess {
    private Thread serverThread; // Thread handling file server
    private ServerSocket serverSocket = null;
    public static String currentPeerID;
    private static int peerIdx;
    private static boolean initialFilePeer = false;
    private static int peerPort;
    private static int hasFile;
    public static BitFieldMessage bitFieldMsg = null;
    private static Thread messageHandlerThread;
    private static boolean downloadFinished = false;
    private static Vector<Thread> receiverThreads = new Vector<>();
    private static Vector<Thread> fileServerThreads = new Vector<>();
    private static volatile Timer preferredNeighborTimer;
    private static volatile Timer optimisticNeighborTimer;
    public static volatile ConcurrentHashMap<String, RemotePeerInfo> remotePeerInfo = new ConcurrentHashMap<>();

    public Thread getServerThread() {
        return serverThread;
    }

    public void setServerThread(Thread serverThread) {
        this.serverThread = serverThread;
    }

    public static void main(String[] args) {
        peerProcess process = new peerProcess();
        currentPeerID = args[0];

        try {
            LoggingHelper logger = new LoggingHelper();
            logger.initializeLogger(currentPeerID);
            logAndDisplay(currentPeerID + " started.");

            initializeConfigurations();
            setPeerDetails();
            initializeBitField();
            startMessageHandler(process);
            endAllPeers(process);
        } catch (Exception e) {
        } finally {
            logAndDisplay(currentPeerID + " Exiting.");
            System.exit(0);
        }
    }

    private static void endAllPeers(peerProcess process) {
        while (true) {
            downloadFinished = checkDownloadCompletion();

            if (downloadFinished) {
                logAndDisplay("All peers have completed file download.");
                preferredNeighborTimer.cancel();
                optimisticNeighborTimer.cancel();

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }

                stopThreads(process);
                break;
            } else {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static void stopThreads(peerProcess process) {
        terminateThread(process.getServerThread());
        terminateThread(messageHandlerThread);

        for (Thread thread : receiverThreads) {
            terminateThread(thread);
        }

        for (Thread thread : fileServerThreads) {
            terminateThread(thread);
        }
    }

    @SuppressWarnings("removal")
    private static void terminateThread(Thread thread) {
        if (thread.isAlive()) {
            thread.stop();
        }
    }

    private static void initializeBitField() {
        bitFieldMsg = new BitFieldMessage();
        bitFieldMsg.setPieceDetails(currentPeerID, hasFile);
    }

    private static void setPeerDetails() {
        Set<String> peerIds = remotePeerInfo.keySet();
        for (String id : peerIds) {
            RemotePeerInfo details = remotePeerInfo.get(id);
            if (details.getPeerId().equals(currentPeerID)) {
                peerPort = Integer.parseInt(details.getPort());
                peerIdx = details.getIndex();
                if (details.getHasFile() == 1) {
                    initialFilePeer = true;
                    hasFile = details.getHasFile();
                    break;
                }
            }
        }
    }

    private static void initializeConfigurations() throws Exception {
        loadPeerDetails();
    }

    private static void startMessageHandler(peerProcess process) {
        messageHandlerThread = new Thread(new PeerMessageProcessor(currentPeerID));
        messageHandlerThread.start();
    }

    private static void loadPeerDetails() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (int i = 0; i < lines.size(); i++) {
                String[] properties = lines.get(i).split("\\s+");
                remotePeerInfo.put(properties[0], new RemotePeerInfo(properties[0], properties[1], properties[2],
                        Integer.parseInt(properties[3]), i));
            }
        } catch (IOException e) {
            throw e;
        }
    }

    private static boolean checkDownloadCompletion() {
        boolean downloadStatus = true;
        try {
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (String line : lines) {
                String[] properties = line.split("\\s+");
                if (Integer.parseInt(properties[3]) == 0) {
                    downloadStatus = false;
                    break;
                }
            }
        } catch (IOException e) {
            downloadStatus = false;
        }
        return downloadStatus;
    }


    private static void logAndDisplay(String message) {
        LoggingHelper.logAndDisplay(message);
    }
}
