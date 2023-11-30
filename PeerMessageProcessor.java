import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Date;
import java.util.Set;

/**
 * This class is used to process messages from message queue.
 */
public class PeerMessageProcessor implements Runnable {

    //PeerID of the host
    private static String currentPeerID;
    //File to handle a piece
    private RandomAccessFile randomAccessFile;

    /**
     * Constructor to initialize PeerMessageProcessingHandler object with peerID from arguments
     *
     * @param peerID - peerID to be set
     */
    public PeerMessageProcessor(String peerID) {
        currentPeerID = peerID;
    }

    /**
     * Empty constructor to initialize PeerMessageProcessingHandler object
     */
    public PeerMessageProcessor() {
        currentPeerID = null;
    }

    /**
     * This method runs everytime PeerMessageProcessingHandler thread is started.
     * It reads messages from message queue and processes them. It sends the appropriate messages based on the type of message received.
     */
    @Override
    public void run() {
        MsgDetails messageDetails;
        BaseMsg message;
        String messageType;
        String remotePeerID;

        while (true) {
            //Read message from queue
            messageDetails = MsgQueue.dequeueMessage();
            while (messageDetails == null) {
                Thread.currentThread();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                messageDetails = MsgQueue.dequeueMessage();
            }
            message = messageDetails.getMessage();
            messageType = message.getType();
            remotePeerID = messageDetails.getSenderPeerID();
            int peerState = peerProcess.remotePeerInfoMap.get(remotePeerID).getPeerState();

            if (messageType.equals(MsgConstants.MESSAGE_HAVE) && peerState != 14) {
                //Received a interesting pieces message
                logAndShowInConsole(currentPeerID + " contains interesting pieces from Peer " + remotePeerID);
                if (isPeerInterested(message, remotePeerID)) {
                    sendInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                    peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(9);
                } else {
                    sendNotInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                    peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(13);
                }
            } else {
                if (peerState == 2) {
                    if (messageType.equals(MsgConstants.MESSAGE_BITFIELD)) {
                        //Received bitfield message
                        logAndShowInConsole(currentPeerID + " received a BITFIELD message from Peer " + remotePeerID);
                        sendBitFieldMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(3);
                    }
                } else if (peerState == 3) {
                    if (messageType.equals(MsgConstants.MESSAGE_INTERESTED)) {
                        //Received interested message
                        logAndShowInConsole(currentPeerID + " receieved an INTERESTED message from Peer " + remotePeerID);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setIsInterested(1);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setIsHandShaked(1);
                        //check if the neighbor is in unchoked neighbors or optimistically unchoked neighbors list
                        if (isNotPreferredAndUnchokedNeighbour(remotePeerID)) {
                            sendChokedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setIsChoked(1);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(6);
                        } else {
                            sendUnChokedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setIsChoked(0);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(4);
                        }
                    } else if (messageType.equals(MsgConstants.MESSAGE_NOT_INTERESTED)) {
                        //Received not interested message
                        logAndShowInConsole(currentPeerID + " receieved an NOT INTERESTED message from Peer " + remotePeerID);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setIsInterested(0);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setIsHandShaked(1);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(5);
                    }
                } else if (peerState == 4) {
                    if (messageType.equals(MsgConstants.MESSAGE_REQUEST)) {
                        //Received request message
                        //send file piece to the requestor
                        sendFilePiece(peerProcess.peerToSocketMap.get(remotePeerID), message, remotePeerID);

                        Set<String> remotePeerDetailsKeys = peerProcess.remotePeerInfoMap.keySet();
                        if (!peerProcess.initialFilePeer && peerProcess.bitFieldMsg.isFileDownloadComplete()) {
                            for (String key : remotePeerDetailsKeys) {
                                if (!key.equals(peerProcess.currentPeerID)) {
                                    Socket socket = peerProcess.peerToSocketMap.get(key);
                                    if (socket != null) {
                                        sendDownloadCompleteMessage(socket, key);
                                    }
                                }
                            }
                        }
                        if (isNotPreferredAndUnchokedNeighbour(remotePeerID)) {
                            //sending choked message if the neighbor is not in unchoked neighbors or optimistically unchoked neighbors list
                            sendChokedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setIsChoked(1);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(6);
                        }
                    }
                } else if (peerState == 8) {
                    if (messageType.equals(MsgConstants.MESSAGE_BITFIELD)) {
                        //Received bifield message
                        if (isPeerInterested(message, remotePeerID)) {
                            sendInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(9);
                        } else {
                            sendNotInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(13);
                        }
                    }
                } else if (peerState == 9) {
                    if (messageType.equals(MsgConstants.MESSAGE_CHOKE)) {
                        //Received choke message
                        logAndShowInConsole(currentPeerID + " is CHOKED by Peer " + remotePeerID);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setIsChoked(1);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(14);
                    } else if (messageType.equals(MsgConstants.MESSAGE_UNCHOKE)) {
                        //Received unchoke message
                        logAndShowInConsole(currentPeerID + " is UNCHOKED by Peer " + remotePeerID);
                        //get the piece index which is present in remote peer but not in current peer and send a request message
                        int firstDifferentPieceIndex = getFirstDifferentPieceIndex(remotePeerID);
                        if (firstDifferentPieceIndex == -1) {
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(13);
                        } else {
                            sendRequestMessage(peerProcess.peerToSocketMap.get(remotePeerID), firstDifferentPieceIndex, remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(11);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setStartTime(new Date());
                        }
                    }
                } else if (peerState == 11) {
                    if (messageType.equals(MsgConstants.MESSAGE_CHOKE)) {
                        //Received choke message
                        logAndShowInConsole(currentPeerID + " is CHOKED by Peer " + remotePeerID);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setIsChoked(1);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(14);
                    } else if (messageType.equals(MsgConstants.MESSAGE_PIECE)) {
                        //Received piece message
                        byte[] payloadInBytes = message.getPayload();
                        //compute data downloading rate of the peer
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setEndTime(new Date());
                        long totalTime = peerProcess.remotePeerInfoMap.get(remotePeerID).getEndTime().getTime()
                                - peerProcess.remotePeerInfoMap.get(remotePeerID).getStartTime().getTime();
                        double dataRate = ((double) (payloadInBytes.length + MsgConstants.MESSAGE_LENGTH + MsgConstants.MESSAGE_TYPE) / (double) totalTime) * 100;
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setDataRate(dataRate);
                        FilePiece filePiece = FilePiece.fromByteArray(payloadInBytes);
                        //update the piece information in current peer bitfield
                        peerProcess.bitFieldMsg.updateBitFieldInfo(remotePeerID, filePiece);
                        int firstDifferentPieceIndex = getFirstDifferentPieceIndex(remotePeerID);
                        if (firstDifferentPieceIndex == -1) {
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(13);
                        } else {
                            sendRequestMessage(peerProcess.peerToSocketMap.get(remotePeerID), firstDifferentPieceIndex, remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(11);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setStartTime(new Date());
                        }

                        peerProcess.updateOtherPeerDetails();
                        Set<String> remotePeerDetailsKeys = peerProcess.remotePeerInfoMap.keySet();
                        for (String key : remotePeerDetailsKeys) {
                            RemotePeerInfo peerDetails = peerProcess.remotePeerInfoMap.get(key);
                            //send have message to peer if its interested
                            if (!key.equals(peerProcess.currentPeerID) && hasPeerInterested(peerDetails)) {
                                sendHaveMessage(peerProcess.peerToSocketMap.get(key), key);
                                peerProcess.remotePeerInfoMap.get(key).setPeerState(3);
                            }
                        }

                        payloadInBytes = null;
                        message = null;
                        if (!peerProcess.initialFilePeer && peerProcess.bitFieldMsg.isFileDownloadComplete()) {
                            for (String key : remotePeerDetailsKeys) {
                                RemotePeerInfo peerDetails = peerProcess.remotePeerInfoMap.get(key);
                                if (!key.equals(peerProcess.currentPeerID)) {
                                    Socket socket = peerProcess.peerToSocketMap.get(key);
                                    if (socket != null) {
                                        sendDownloadCompleteMessage(socket, key);
                                    }
                                }
                            }
                        }
                    }
                } else if (peerState == 14) {
                    if (messageType.equals(MsgConstants.MESSAGE_HAVE)) {
                        //Received contains interesting pieces
                        logAndShowInConsole(currentPeerID + " contains interesting pieces from Peer " + remotePeerID);
                        if (isPeerInterested(message, remotePeerID)) {
                            sendInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(9);
                        } else {
                            sendNotInterestedMessage(peerProcess.peerToSocketMap.get(remotePeerID), remotePeerID);
                            peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(13);
                        }
                    } else if (messageType.equals(MsgConstants.MESSAGE_UNCHOKE)) {
                        //Received unchoked message
                        logAndShowInConsole(currentPeerID + " is UNCHOKED by Peer " + remotePeerID);
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(14);
                    }
                } else if (peerState == 15) {
                    try {
                        //update neighbor details after it gets file completely
                        peerProcess.remotePeerInfoMap.get(peerProcess.currentPeerID).updatePeerDetails(remotePeerID, 1);
                        logAndShowInConsole(remotePeerID + " has downloaded the complete file");
                        int previousState = peerProcess.remotePeerInfoMap.get(remotePeerID).getPreviousPeerState();
                        peerProcess.remotePeerInfoMap.get(remotePeerID).setPeerState(previousState);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * This method is used to send DOWNLOAD COMPLETE message to socket
     *
     * @param socket - socket in which the message to be sent
     * @param peerID - peerID to which the message should be sent
     */
    private void sendDownloadCompleteMessage(Socket socket, String peerID) {
        logAndShowInConsole(currentPeerID + " sending a DOWNLOAD COMPLETE message to Peer " + peerID);
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_DOWNLOADED);
        byte[] messageInBytes = BaseMsg.convertMessageToByteArray(message);
        SendMessageToSocket(socket, messageInBytes);
    }

    /**
     * This method is used to send HAVE message to socket
     *
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
     * This method is used to check remote peer is interested to receive messages
     *
     * @param remotePeerDetails - Peer to be checked
     * @return true - peer interested; false peer not interested
     */
    private boolean hasPeerInterested(RemotePeerInfo remotePeerDetails) {
        return remotePeerDetails.getIsComplete() == 0 &&
                remotePeerDetails.getIsChoked() == 0 && remotePeerDetails.getIsInterested() == 1;
    }

    /**
     * This method is used to get the index of first piece different from current piece
     *
     * @param peerID - peerID of the remote host
     * @return index of the first different piece
     */
    private int getFirstDifferentPieceIndex(String peerID) {
        return peerProcess.bitFieldMsg.findInterestingPieceIndex(peerProcess.remotePeerInfoMap.get(peerID).getBitFieldMessage());
    }

    /**
     * This method is used to send REQUEST message to socket
     *
     * @param socket       - socket in which the message to be sent
     * @param pieceIndex   - index of the piece to be requested
     * @param remotePeerID - peerID to which the message should be sent
     */
    private void sendRequestMessage(Socket socket, int pieceIndex, String remotePeerID) {
        logAndShowInConsole(peerProcess.currentPeerID + " sending REQUEST message to Peer " + remotePeerID + " for piece " + pieceIndex);
        int pieceIndexLength = MsgConstants.PIECE_INDEX_LENGTH;
        byte[] pieceInBytes = new byte[pieceIndexLength];
        for (int i = 0; i < pieceIndexLength; i++) {
            pieceInBytes[i] = 0;
        }

        byte[] pieceIndexInBytes = PeerUtils.integerToBytes(pieceIndex);
        System.arraycopy(pieceIndexInBytes, 0, pieceInBytes, 0, pieceIndexInBytes.length);
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_REQUEST, pieceIndexInBytes);
        SendMessageToSocket(socket, BaseMsg.convertMessageToByteArray(message));

        pieceInBytes = null;
        pieceIndexInBytes = null;
        message = null;
    }

    /**
     * This method is used to send File piece to socket
     *
     * @param socket       - socket in which the message to be sent
     * @param message      - message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private void sendFilePiece(Socket socket, BaseMsg message, String remotePeerID) {
        byte[] pieceIndexInBytes = message.getPayload();
        int pieceIndex = PeerUtils.bytesToInteger(pieceIndexInBytes);
        int pieceSize = ConfigSettings.pieceSize;
        logAndShowInConsole(currentPeerID + " sending a PIECE message for piece " + pieceIndex + " to peer " + remotePeerID);

        byte[] bytesRead = new byte[pieceSize];
        int numberOfBytesRead = 0;
        File file = new File(currentPeerID, ConfigSettings.fileName);
        try {
            randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(pieceIndex * pieceSize);
            numberOfBytesRead = randomAccessFile.read(bytesRead, 0, pieceSize);

            byte[] buffer = new byte[numberOfBytesRead + MsgConstants.PIECE_INDEX_LENGTH];
            System.arraycopy(pieceIndexInBytes, 0, buffer, 0, MsgConstants.PIECE_INDEX_LENGTH);
            System.arraycopy(bytesRead, 0, buffer, MsgConstants.PIECE_INDEX_LENGTH, numberOfBytesRead);

            BaseMsg messageToBeSent = new BaseMsg(MsgConstants.MESSAGE_PIECE, buffer);
            SendMessageToSocket(socket, BaseMsg.convertMessageToByteArray(messageToBeSent));
            randomAccessFile.close();

            buffer = null;
            bytesRead = null;
            pieceIndexInBytes = null;
            messageToBeSent = null;
        } catch (IOException e) {

        }
    }

    /**
     * This method is used if remote peer is not a preferred neighbor or optimistically unchoked neighbor.
     *
     * @param remotePeerId - peerID to be checked
     * @return true - remote peer is not preferred neighbor or optimistically unchoked neighbor;
     * false - remote peer is preferred neighbor or optimistically unchoked neighbor
     */
    private boolean isNotPreferredAndUnchokedNeighbour(String remotePeerId) {
        return !peerProcess.preferredNeighboursMap.containsKey(remotePeerId) && !peerProcess.optimisticUnchokedNeighbors.containsKey(remotePeerId);
    }

    /**
     * This method is used to send CHOKE message to socket
     *
     * @param socket       - socket in which the message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private void sendChokedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending a CHOKE message to Peer " + remotePeerID);
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_CHOKE);
        byte[] messageInBytes = BaseMsg.convertMessageToByteArray(message);
        SendMessageToSocket(socket, messageInBytes);
    }

    /**
     * This method is used to send UNCHOKE message to socket
     *
     * @param socket       - socket in which the message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private void sendUnChokedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending a UNCHOKE message to Peer " + remotePeerID);
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_UNCHOKE);
        byte[] messageInBytes = BaseMsg.convertMessageToByteArray(message);
        SendMessageToSocket(socket, messageInBytes);
    }

    /**
     * This method is used to send NOT INTERESTED message to socket
     *
     * @param socket       - socket in which the message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private void sendNotInterestedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending a NOT INTERESTED message to Peer " + remotePeerID);
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_NOT_INTERESTED);
        byte[] messageInBytes = BaseMsg.convertMessageToByteArray(message);
        SendMessageToSocket(socket, messageInBytes);
    }

    /**
     * This method is used to send INTERESTED message to socket
     *
     * @param socket       - socket in which the message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private void sendInterestedMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending an INTERESTED message to Peer " + remotePeerID);
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_INTERESTED);
        byte[] messageInBytes = BaseMsg.convertMessageToByteArray(message);
        SendMessageToSocket(socket, messageInBytes);
    }

    /**
     * This method is used to send BITFIELD message to socket
     *
     * @param socket       - socket in which the message to be sent
     * @param remotePeerID - peerID to which the message should be sent
     */
    private void sendBitFieldMessage(Socket socket, String remotePeerID) {
        logAndShowInConsole(currentPeerID + " sending a BITFIELD message to Peer " + remotePeerID);
        byte[] bitFieldMessageInByteArray = peerProcess.bitFieldMsg.getBytes();
        BaseMsg message = new BaseMsg(MsgConstants.MESSAGE_BITFIELD, bitFieldMessageInByteArray);
        byte[] messageInBytes = BaseMsg.convertMessageToByteArray(message);
        SendMessageToSocket(socket, messageInBytes);

        bitFieldMessageInByteArray = null;
    }

    /**
     * This method is used to check if a peer is interested to receive messages.
     *
     * @param message      - message to be checked
     * @param remotePeerID - peerID to which the message should be sent
     * @return true - peer interested; false - peer not interested
     */
    private boolean isPeerInterested(BaseMsg message, String remotePeerID) {
        boolean peerInterested = false;
        BitFieldMessage bitField = BitFieldMessage.decodeMessage(message.getPayload());
        peerProcess.remotePeerInfoMap.get(remotePeerID).setBitFieldMessage(bitField);
        int pieceIndex = peerProcess.bitFieldMsg.findInterestingPieceIndex(bitField);
        if (pieceIndex != -1) {
            if (message.getType().equals(MsgConstants.MESSAGE_HAVE))
                logAndShowInConsole(currentPeerID + " received HAVE message from Peer " + remotePeerID + " for piece " + pieceIndex);
            peerInterested = true;
        }

        return peerInterested;
    }

    /**
     * This method is used to write a message to socket
     *
     * @param socket         - socket in which the message to be sent
     * @param messageInBytes - message to be sent
     */
    private void SendMessageToSocket(Socket socket, byte[] messageInBytes) {
        try {
            OutputStream out = socket.getOutputStream();
            out.write(messageInBytes);
        } catch (IOException e) {
        }
    }

    /**
     * This method is used to log a message in a log file and show it in console
     *
     * @param message - message to be logged and showed in console
     */
    private static void logAndShowInConsole(String message) {
        LoggingHelper.logAndDisplay(message);
    }
}
