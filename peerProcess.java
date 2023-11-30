import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.Remote;
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
    public static boolean initialFilePeer = false;
    private static int peerPort;
    private static int hasFile;
    public static BitFieldMessage bitFieldMsg = null;
    private static Thread messageHandlerThread;
    private static boolean downloadFinished = false;
    private static Vector<Thread> receiverThreads = new Vector<>();
    public static Vector<Thread> fileServerThreads = new Vector<>();
    private static volatile Timer preferredNeighborTimer;
    private static volatile Timer optimisticNeighborTimer;
    public static volatile ConcurrentHashMap<String, RemotePeerInfo> remotePeerInfoMap = new ConcurrentHashMap<>();
    public static volatile ConcurrentHashMap<String, RemotePeerInfo> preferredNeighboursMap = new ConcurrentHashMap();
    //Map to store peer sockets
    public static volatile ConcurrentHashMap<String, Socket> peerToSocketMap = new ConcurrentHashMap();
    //Map to store optimistically unchoked neighbors
    public static volatile ConcurrentHashMap<String, RemotePeerInfo> optimisticUnchokedNeighbors = new ConcurrentHashMap();

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

            //starting the file server thread and file threads
            startFileServerReceiverThreads(process);
            //update preferred neighbors list
            determinePreferredNeighbors();
            //update optimistically unchoked neighbor list
            determineOptimisticallyUnchockedNeighbours();

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
                    Thread.currentThread();
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }

                stopThreads(process);
                break;
            } else {
                try {
                    Thread.currentThread();
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

    /**
     * This method is used to start file server and file receiver threads
     * @param process - peerprrocess to start threads into
     */
    public static void startFileServerReceiverThreads(peerProcess process) {
        if (initialFilePeer) {
            //Peer having file initially. starting server thread
            startFileServerThread(process);
        } else {
            //if not a peer which has file initially. Create an empty new file. Starting serving and listening threads
            createNewFile();
            startFileReceiverThreads(process);
            startFileServerThread(process);
        }
    }

    /**
     * This method is used to start file receiver threads
     * @param process - peerprrocess to start threads into
     */
    public static void startFileReceiverThreads(peerProcess process) {
        Set<String> remotePeerDetailsKeys = remotePeerInfoMap.keySet();
        for (String peerID : remotePeerDetailsKeys) {
            RemotePeerInfo remotePeerDetails = remotePeerInfoMap.get(peerID);

            if (process.peerIdx > remotePeerDetails.getIndex()) {
                Thread tempThread = new Thread(new PeerMessageHandler(
                        remotePeerDetails.getHostAddress(), Integer
                        .parseInt(remotePeerDetails.getPort()), 1,
                        currentPeerID));
                receiverThreads.add(tempThread);
                tempThread.start();
            }
        }
    }

    /**
     * This method is used to start file server thread
     * @param process - peerprrocess to start thread into
     */
    public static void startFileServerThread(peerProcess process) {
        try {
            //Start a new file server thread
            process.serverSocket = new ServerSocket(peerPort);
            process.serverThread = new Thread(new PeerServerHandler(process.serverSocket, currentPeerID));
            process.serverThread.start();
        } catch (SocketTimeoutException e) {
            logAndDisplay(currentPeerID + " Socket Gets Timed out Error - " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void setPeerDetails() {
        Set<String> peerIds = remotePeerInfoMap.keySet();
        for (String id : peerIds) {
            RemotePeerInfo details = remotePeerInfoMap.get(id);
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
        //read Common.cfg
        initializePeerConfiguration();
        loadPeerDetails();
        //initialize preferred neighbours
        setPreferredNeighbours();
    }

    /**
     * This method creates a timer task to determine preferred neighbors
     */
    public static void determinePreferredNeighbors() {
        preferredNeighborTimer = new Timer();
        preferredNeighborTimer.schedule(new PrefNeighbors(),
                ConfigSettings.unchokingInterval * 1000 * 0,
                ConfigSettings.unchokingInterval * 1000);
    }

    /**
     * This method creates a timer task to determine optimistically unchoked neighbors
     */
    public static void determineOptimisticallyUnchockedNeighbours() {
        optimisticNeighborTimer = new Timer();
        optimisticNeighborTimer.schedule(new OptimisticNeighborSelector(),
                ConfigSettings.optimisticUnchokingInterval * 1000 * 0,
                ConfigSettings.optimisticUnchokingInterval * 1000
        );
    }

    private static void startMessageHandler(peerProcess process) {
        messageHandlerThread = new Thread(new PeerMessageProcessor(currentPeerID));
        messageHandlerThread.start();
    }

    /**
     * This method is used to create empty file with size 'CommonConfiguration.fileSize' and set zero bits into it
     */
    public static void createNewFile() {
        try {
            File dir = new File(currentPeerID);
            dir.mkdir();

            File newfile = new File(currentPeerID, ConfigSettings.fileName);
            OutputStream os = new FileOutputStream(newfile, true);
            byte b = 0;

            for (int i = 0; i < ConfigSettings.fileSize; i++)
                os.write(b);
            os.close();
        } catch (Exception e) {
            logAndDisplay(currentPeerID + " ERROR in creating the file : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is used to set preferred neighbors of a peer
     */
    public static void setPreferredNeighbours() {
        Set<String> remotePeerIDs = remotePeerInfoMap.keySet();
        for (String peerID : remotePeerIDs) {
            RemotePeerInfo remotePeerDetails = remotePeerInfoMap.get(peerID);
            if (remotePeerDetails != null && !peerID.equals(currentPeerID)) {
                preferredNeighboursMap.put(peerID, remotePeerDetails);
            }
        }
    }

    public static void loadPeerDetails() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (int i = 0; i < lines.size(); i++) {
                String[] properties = lines.get(i).split("\\s+");
                remotePeerInfoMap.put(properties[0], new RemotePeerInfo(properties[0], properties[1], properties[2],
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

    /**
     * This method reads Common.cfg and initializes the properties in CommonConfiguration class
     * @throws IOException
     */
    public static void initializePeerConfiguration() throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get("Common.cfg"));
            for (String line : lines) {
                String[] properties = line.split("\\s+");
                if (properties[0].equalsIgnoreCase("NumberOfPreferredNeighbors")) {
                    ConfigSettings.numberOfPreferredNeighbours = Integer.parseInt(properties[1]);
                } else if (properties[0].equalsIgnoreCase("UnchokingInterval")) {
                    ConfigSettings.unchokingInterval = Integer.parseInt(properties[1]);
                } else if (properties[0].equalsIgnoreCase("OptimisticUnchokingInterval")) {
                    ConfigSettings.optimisticUnchokingInterval = Integer.parseInt(properties[1]);
                } else if (properties[0].equalsIgnoreCase("FileName")) {
                    ConfigSettings.fileName = properties[1];
                } else if (properties[0].equalsIgnoreCase("FileSize")) {
                    ConfigSettings.fileSize = Integer.parseInt(properties[1]);
                } else if (properties[0].equalsIgnoreCase("PieceSize")) {
                    ConfigSettings.pieceSize = Integer.parseInt(properties[1]);
                }
            }
        } catch (IOException e) {
            throw e;
        }
    }


    private static void logAndDisplay(String message) {
        LoggingHelper.logAndDisplay(message);
    }

    /**
     * This method reads PeerInfo.cfg file and updates peers in remotePeerDetailsMap
     */
    public static void updateOtherPeerDetails() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("PeerInfo.cfg"));
            for (int i = 0; i < lines.size(); i++) {
                String[] properties = lines.get(i).split("\\s+");
                String peerID = properties[0];
                int isCompleted = Integer.parseInt(properties[3]);
                if (isCompleted == 1) {
                    remotePeerInfoMap.get(peerID).setIsComplete(1);
                    remotePeerInfoMap.get(peerID).setIsInterested(0);
                    remotePeerInfoMap.get(peerID).setIsChoked(0);
                }
            }
        } catch (IOException e) {
        }
    }
}
