import java.io.RandomAccessFile;

/**
 * Handles the processing of peer messages from the message queue.
 */
public class PeerMessageProcessor implements Runnable {

    // Unique identifier of the host peer.
    private static String hostPeerID;
    // File handling utility.
    private RandomAccessFile fileHandler;

    /**
     * Constructor that initializes the PeerMessageProcessor with a specified peerID.
     *
     * @param peerID - The unique identifier to set for the peer.
     */
    public PeerMessageProcessor(String peerID) {
        hostPeerID = peerID;
    }

    /**
     * Default constructor that initializes the PeerMessageProcessor without a specific peerID.
     */
    public PeerMessageProcessor() {
        hostPeerID = null;
    }

    /**
     * Executed when the PeerMessageProcessor thread starts.
     * Continuously reads and processes messages from the message queue.
     */
    @Override
    public void run() {
        MsgDetails msgDetails;
        BaseMsg baseMsg;
        String messageType;
        String senderPeerID;

        while (true) {
            // Dequeue a message from the queue.
            msgDetails = MsgQueue.dequeueMessage();
            while (msgDetails == null) {
                try {
                    // Wait for 500 milliseconds before attempting to dequeue again.
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                msgDetails = MsgQueue.dequeueMessage();
            }

            // Extract details from the dequeued message.
            baseMsg = msgDetails.getMessage();
            messageType = baseMsg.getType();
            senderPeerID = msgDetails.getSenderPeerID();
            int peerState = peerProcess.remotePeerInfo.get(senderPeerID).getPeerState();

            // Process the message based on its type and the state of the peer.
            processMessage(messageType, peerState);
        }
    }

    /**
     * Processes the dequeued message based on its type and the peer's state.
     *
     * @param messageType - Type of the received message.
     * @param peerState - Current state of the peer.
     */
    private void processMessage(String messageType, int peerState) {
        if (messageType.equals(MsgConstants.MESSAGE_HAVE) && peerState != 14) {
            // Process an interesting pieces message.
        } else {
            // Process messages based on peerState and messageType.
            // Relevant code goes here to handle various cases.
        }
    }
}
