import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Class for selecting an optimistically unchoked neighbor from a set of choked neighbors.
 */
public class OptimisticNeighborSelector extends TimerTask {

    /**
     * Executes at regular intervals to choose an optimistically unchoked neighbor from choked ones.
     */
    @Override
    public void run() {
        peerProcess.updateOtherPeerDetails();
        if (!peerProcess.optimisticUnchokedNeighbors.isEmpty()) {
            peerProcess.optimisticUnchokedNeighbors.clear();
        }

        // Collect all peers showing interest
        Set<String> peerKeys = peerProcess.remotePeerInfoMap.keySet();
        Vector<RemotePeerInfo> interestedPeers = new Vector<>();
        for (String key : peerKeys) {
            RemotePeerInfo peerInfo = peerProcess.remotePeerInfoMap.get(key);
            if (!key.equals(peerProcess.currentPeerID) && isPeerInterested(peerInfo)) {
                interestedPeers.add(peerInfo);
            }
        }

        if (!interestedPeers.isEmpty()) {
            // Choose a random peer from the interested ones
            Collections.shuffle(interestedPeers);
            RemotePeerInfo chosenPeer = interestedPeers.firstElement();
            chosenPeer.setIsOptimisticallyUnchockedNeighbor(1);
            peerProcess.optimisticUnchokedNeighbors.put(chosenPeer.getPeerId(), chosenPeer);
            displayLog(peerProcess.currentPeerID + " optimistically unchoked neighbor " + chosenPeer.getPeerId());

            if (chosenPeer.getIsChoked() == 1) {
                // Send unchoke and have messages if the peer is choked
                peerProcess.remotePeerInfoMap.get(chosenPeer.getPeerId()).setIsChoked(0);
                sendUnchokeMsg(peerProcess.peerToSocketMap.get(chosenPeer.getPeerId()), chosenPeer.getPeerId());
                sendHaveMsg(peerProcess.peerToSocketMap.get(chosenPeer.getPeerId()), chosenPeer.getPeerId());
                peerProcess.remotePeerInfoMap.get(chosenPeer.getPeerId()).setPeerState(3);
            }
        }
    }

    /**
     * Checks if a peer is interested.
     * @param peerInfo The peer to check.
     * @return True if interested, false otherwise.
     */
    private boolean isPeerInterested(RemotePeerInfo peerInfo) {
        return peerInfo.getIsComplete() == 0 &&
                peerInfo.getIsChoked() == 1 && peerInfo.getIsInterested() == 1;
    }

    /**
     * Sends an unchoke message.
     * @param socket The socket to send the message through.
     * @param peerId The ID of the peer to send to.
     */
    private void sendUnchokeMsg(Socket socket, String peerId) {
        displayLog(peerProcess.currentPeerID + " sending UNCHOKE to Peer " + peerId);
        BaseMsg msg = new BaseMsg(MsgConstants.MESSAGE_UNCHOKE);
        sendMessage(socket, BaseMsg.convertMessageToByteArray(msg));
    }

    /**
     * Sends a have message.
     * @param socket The socket to send the message through.
     * @param peerId The ID of the peer to send to.
     */
    private void sendHaveMsg(Socket socket, String peerId) {
        displayLog(peerProcess.currentPeerID + " sending HAVE to Peer " + peerId);
        byte[] bitFieldBytes = peerProcess.bitFieldMsg.getBytes();
        BaseMsg msg = new BaseMsg(MsgConstants.MESSAGE_HAVE, bitFieldBytes);
        sendMessage(socket, BaseMsg.convertMessageToByteArray(msg));
    }

    /**
     * Writes a message to the socket.
     * @param socket The socket to use.
     * @param msgBytes The message in byte form.
     */
    private void sendMessage(Socket socket, byte[] msgBytes) {
        try (OutputStream out = socket.getOutputStream()) {
            out.write(msgBytes);
        } catch (IOException e) {
            // Exception handling
        }
    }

    /**
     * Logs and displays a message.
     * @param msg The message to log and display.
     */
    private static void displayLog(String msg) {
        LoggingHelper.logAndDisplay(msg);
    }
}
