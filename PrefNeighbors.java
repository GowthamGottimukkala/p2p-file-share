import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.*;

/**
 * This class is used to determine preferred neighbors from a list of choked neighbors
 */
public class PrefNeighbors extends TimerTask {
    /**
     * This method is run everytime PrefNeighbors timer task is invoked.
     * For a peer which has file it determines preferred neighbors randomly.
     * For a peer which doesnt have the file, It determines neighbors based to download rate of peers.
     */
    public void run() {
        int countInterested = 0;
        StringBuilder preferredNeighbors = new StringBuilder();
        //updates remotePeerInfo lost
        peerProcess.updateOtherPeerDetails();
        //
        Set<String> remotePeerIDs = peerProcess.remotePeerInfoMap.keySet();
        for (String key : remotePeerIDs) {
            RemotePeerInfo remotePeerDetails = peerProcess.remotePeerInfoMap.get(key);
            if (!key.equals(peerProcess.currentPeerID)) {
                if (remotePeerDetails.getIsComplete() == 0 && remotePeerDetails.getIsInterested() == 1) {
                    countInterested++;
                } else if (remotePeerDetails.getIsComplete() == 1) {
                    peerProcess.preferredNeighboursMap.remove(key);
                }
            }
        }

        if (countInterested > ConfigSettings.numberOfPreferredNeighbours) {
            //If there are more number of interested neighbors than needed, add the first 'CommonConfiguration.numberOfPreferredNeighbours'
            // number of interested neighbors to preferred neighbors to list
            if (!peerProcess.preferredNeighboursMap.isEmpty())
                peerProcess.preferredNeighboursMap.clear();
            List<RemotePeerInfo> pv = new ArrayList(peerProcess.remotePeerInfoMap.values());
            int isCompleteFilePresent = peerProcess.remotePeerInfoMap.get(peerProcess.currentPeerID).getIsComplete();
            if (isCompleteFilePresent == 1) {
                Collections.shuffle(pv);
            } else {
                Collections.sort(pv, new PeerDownloadComparator(false));
            }
            int count = 0;
            for (int i = 0; i < pv.size(); i++) {
                if (count > ConfigSettings.numberOfPreferredNeighbours - 1)
                    break;
                if (pv.get(i).getIsInterested() == 1 && !pv.get(i).getPeerId().equals(peerProcess.currentPeerID)
                        && peerProcess.remotePeerInfoMap.get(pv.get(i).getPeerId()).getIsComplete() == 0) {
                    peerProcess.remotePeerInfoMap.get(pv.get(i).getPeerId()).setIsPreferredNeighbor(1);
                    peerProcess.preferredNeighboursMap.put(pv.get(i).getPeerId(), peerProcess.remotePeerInfoMap.get(pv.get(i).getPeerId()));

                    count++;

                    preferredNeighbors.append(pv.get(i).getPeerId()).append(",");
                    if (peerProcess.remotePeerInfoMap.get(pv.get(i).getPeerId()).getIsChoked() == 1) {
                        sendUnChokedMessage(peerProcess.peerToSocketMap.get(pv.get(i).getPeerId()), pv.get(i).getPeerId());
                        peerProcess.remotePeerInfoMap.get(pv.get(i).getPeerId()).setIsChoked(0);
                        sendHaveMessage(peerProcess.peerToSocketMap.get(pv.get(i).getPeerId()), pv.get(i).getPeerId());
                        peerProcess.remotePeerInfoMap.get(pv.get(i).getPeerId()).setPeerState(3);
                    }
                }
            }
        } else {
            //add all the interested neighbors to list
            remotePeerIDs = peerProcess.remotePeerInfoMap.keySet();
            for (String key : remotePeerIDs) {
                RemotePeerInfo remotePeerDetails = peerProcess.remotePeerInfoMap.get(key);
                if (!key.equals(peerProcess.currentPeerID)) {
                    if (remotePeerDetails.getIsComplete() == 0 && remotePeerDetails.getIsInterested() == 1) {
                        if (!peerProcess.preferredNeighboursMap.containsKey(key)) {
                            preferredNeighbors.append(key).append(",");
                            peerProcess.preferredNeighboursMap.put(key, peerProcess.remotePeerInfoMap.get(key));
                            peerProcess.remotePeerInfoMap.get(key).setIsPreferredNeighbor(1);
                        }
                        if (remotePeerDetails.getIsChoked() == 1) {
                            sendUnChokedMessage(peerProcess.peerToSocketMap.get(key), key);
                            peerProcess.remotePeerInfoMap.get(key).setIsChoked(0);
                            sendHaveMessage(peerProcess.peerToSocketMap.get(key), key);
                            peerProcess.remotePeerInfoMap.get(key).setPeerState(3);
                        }
                    }
                }
            }
        }

        if (preferredNeighbors.length() != 0) {
            preferredNeighbors.deleteCharAt(preferredNeighbors.length() - 1);
            logAndShowInConsole(peerProcess.currentPeerID + " has selected the preferred neighbors - " + preferredNeighbors.toString());
        }
    }

    /**
     * This method is used to send UNCHOKE message to socket
     * @param socket - socket in which the message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private static void sendUnChokedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(peerProcess.currentPeerID + " sending a UNCHOKE message to Peer " + remotePeerID);
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_UNCHOKE);
        SendMessageToSocket(socket, BaseMsg.convertMessageToByteArray(message));
    }


    /**
     * This method is used to send HAVE message to socket
     * @param socket - socket in which the message to be sent
     * @param peerID - peerID to which the message should be sent
     */
    private void sendHaveMessage(Socket socket, String peerID) {
        //logAndShowInConsole(peerProcess.currentPeerID + " sending HAVE message to Peer " + peerID);
        byte[] bitFieldInBytes = peerProcess.bitFieldMsg.getBytes();
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_HAVE, bitFieldInBytes);
        SendMessageToSocket(socket, BaseMsg.convertMessageToByteArray(message));

        bitFieldInBytes = null;
    }

    /**
     * This method is used to write a message to socket
     * @param socket - socket in which the message to be sent
     * @param messageInBytes - message to be sent
     */
    private static void SendMessageToSocket(Socket socket, byte[] messageInBytes) {
        try {
            OutputStream out = socket.getOutputStream();
            out.write(messageInBytes);
        } catch (IOException e) {
        }
    }

    /**
     * This method is used to log a message in a log file and show it in console
     * @param message - message to be logged and showed in console
     */
    private static void logAndShowInConsole(String message) {
        LoggingHelper.logAndDisplay(message);
    }
}
