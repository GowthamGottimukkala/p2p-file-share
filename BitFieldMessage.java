import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Manages the bitfield message of peers.
 */
public class BitFieldMessage {

    private FilePiece[] filePieces; // Array of file pieces.
    private int numPieces; // Total number of file pieces.

    /**
     * Initializes the bitfield message based on configuration values.
     */
    public BitFieldMessage() {
        double fileSize = Double.parseDouble(String.valueOf(ConfigSettings.fileSize));
        double pieceSize = Double.parseDouble(String.valueOf(ConfigSettings.pieceSize));
        numPieces = (int) Math.ceil(fileSize / pieceSize);
        filePieces = new FilePiece[numPieces];

        for (int i = 0; i < numPieces; i++) {
            filePieces[i] = new FilePiece();
        }
    }

    /**
     * @return The array of file pieces.
     */
    public FilePiece[] getFilePieces() {
        return filePieces;
    }

    /**
     * Sets the array of file pieces.
     *
     * @param filePieces Array of file pieces.
     */
    public void setFilePieces(FilePiece[] filePieces) {
        this.filePieces = filePieces;
    }

    /**
     * @return The total number of file pieces.
     */
    public int getNumPieces() {
        return numPieces;
    }

    /**
     * Sets the total number of file pieces.
     *
     * @param numPieces Total number of file pieces.
     */
    public void setNumPieces(int numPieces) {
        this.numPieces = numPieces;
    }

    /**
     * Initializes the details of file pieces.
     *
     * @param peerId ID of the peer where the piece is found.
     * @param hasFile Indicates whether the file piece is present or not.
     */
    public void setPieceDetails(String peerId, int hasFile) {
        for (FilePiece piece : filePieces) {
            piece.setPresenceFlag(hasFile == 1 ? 1 : 0);
            piece.setRetrievedFromPeerId(peerId);
        }
    }

    /**
     * Converts the bitfield message to a byte array.
     *
     * @return Byte array representing the bitfield message.
     */
    public byte[] getBytes() {
        int s = numPieces / 8;
        if (numPieces % 8 != 0)
            s = s + 1;
        byte[] iP = new byte[s];
        int tempInt = 0;
        int count = 0;
        int Cnt;
        for (Cnt = 1; Cnt <= numPieces; Cnt++) {
            int tempP = filePieces[Cnt - 1].getPresenceFlag();
            tempInt = tempInt << 1;
            if (tempP == 1) {
                tempInt = tempInt + 1;
            } else
                tempInt = tempInt + 0;

            if (Cnt % 8 == 0 && Cnt != 0) {
                iP[count] = (byte) tempInt;
                count++;
                tempInt = 0;
            }

        }
        if ((Cnt - 1) % 8 != 0) {
            int tempShift = ((numPieces) - (numPieces / 8) * 8);
            tempInt = tempInt << (8 - tempShift);
            iP[count] = (byte) tempInt;
        }
        return iP;
    }

    /**
     * Decodes a byte array to create a BitFieldMessage object.
     *
     * @param bitField Byte array representing the bitfield message.
     * @return Decoded BitFieldMessage object.
     */
    public static BitFieldMessage decodeMessage(byte[] bitField) {
        BitFieldMessage bitFieldMessage = new BitFieldMessage();
        for (int i = 0; i < bitField.length; i++) {
            int count = 7;
            while (count >= 0) {
                int test = 1 << count;
                if (i * 8 + (8 - count - 1) < bitFieldMessage.getNumPieces()) {
                    if ((bitField[i] & (test)) != 0)
                        bitFieldMessage.getFilePieces()[i * 8 + (8 - count - 1)].setPresenceFlag(1);
                    else
                        bitFieldMessage.getFilePieces()[i * 8 + (8 - count - 1)].setPresenceFlag(0);
                }
                count--;
            }
        }

        return bitFieldMessage;
    }

    /**
     * @return The number of pieces currently present.
     */
    public int getNumPiecesPresent() {
        int presentCount = 0;
        for (FilePiece piece : filePieces) {
            if (piece.getPresenceFlag() == 1) {
                presentCount++;
            }
        }

        return presentCount;
    }

    /**
     * Checks whether all pieces of the file have been downloaded.
     *
     * @return true if download is complete; otherwise, false.
     */
    public boolean isFileDownloadComplete() {
        boolean isFileDownloaded = true;
        for (FilePiece filePiece : filePieces) {
            if (filePiece.getPresenceFlag() == 0) {
                isFileDownloaded = false;
                break;
            }
        }

        return isFileDownloaded;
    }

    /**
     * Finds the index of the first interesting piece in a remote peer.
     *
     * @param remoteBitField Bitfield of the remote peer.
     * @return Index of the first interesting piece.
     */
    public synchronized int findInterestingPieceIndex(BitFieldMessage remoteBitField) {
        int numPieces = remoteBitField.getNumPieces();
        int interestingPiece = -1;

        for (int i = 0; i < numPieces; i++) {
            if (remoteBitField.getFilePieces()[i].getPresenceFlag() == 1
                    && this.getFilePieces()[i].getPresenceFlag() == 0) {
                interestingPiece = i;
                break;
            }
        }

        return interestingPiece;
    }

    /**
     * Finds the index of the first differing piece in a remote peer.
     *
     * @param remoteBitField Bitfield of the remote peer.
     * @return Index of the first differing piece.
     */
    public synchronized int findFirstDiffPieceIndex(BitFieldMessage remoteBitField) {
        int firstPieces = numPieces;
        int secondPieces = remoteBitField.getNumPieces();
        int pieceIndex = -1;

        if (secondPieces >= firstPieces) {
            for (int i = 0; i < firstPieces; i++) {
                if (filePieces[i].getPresenceFlag() == 0 && remoteBitField.getFilePieces()[i].getPresenceFlag() == 1) {
                    pieceIndex = i;
                    break;
                }
            }
        } else {
            for (int i = 0; i < secondPieces; i++) {
                if (filePieces[i].getPresenceFlag() == 0 && remoteBitField.getFilePieces()[i].getPresenceFlag() == 1) {
                    pieceIndex = i;
                    break;
                }
            }
        }

        return pieceIndex;
    }

    /**
     * Updates bitfield information based on received file pieces.
     *
     * @param peerID ID of the peer from which the piece is received.
     * @param receivedPiece Received file piece.
     */
    public void updateBitFieldInfo(String peerID, FilePiece receivedPiece) {
        int pieceIndex = receivedPiece.getIndex();
        try {
            if (isPieceAlreadyPresent(pieceIndex)) {
                logAndDisplay(peerID + " Piece already received");
            } else {
                String fileName = ConfigSettings.fileName;

                File file = new File(peerProcess.currentPeerID, fileName);
                int offSet = pieceIndex * ConfigSettings.pieceSize;
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                byte[] pieceToWrite = receivedPiece.getData();
                randomAccessFile.seek(offSet);
                randomAccessFile.write(pieceToWrite);

                filePieces[pieceIndex].setPresenceFlag(1);
                filePieces[pieceIndex].setRetrievedFromPeerId(peerID);
                randomAccessFile.close();
                logAndDisplay(peerProcess.currentPeerID + " has downloaded the PIECE " + pieceIndex
                        + " from Peer " + peerID + ". Now the number of pieces it has is "
                        + peerProcess.bitFieldMsg.getNumPiecesPresent());

                if (peerProcess.bitFieldMsg.isFileDownloadComplete()) {
                    //update file download details
                    peerProcess.remotePeerInfo.get(peerID).setIsInterested(0);
                    peerProcess.remotePeerInfo.get(peerID).setIsComplete(1);
                    peerProcess.remotePeerInfo.get(peerID).setIsChoked(0);
                    peerProcess.remotePeerInfo.get(peerID).updatePeerDetails(peerProcess.currentPeerID, 1);
                    logAndDisplay(peerProcess.currentPeerID + " has DOWNLOADED the complete file.");
                }
            }
        } catch (IOException e) {
            logAndDisplay(peerProcess.currentPeerID + " EROR in updating bitfield " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Checks whether a specific piece is already present.
     *
     * @param pieceIndex Index of the piece to check.
     * @return true if the piece is present; otherwise, false.
     */
    private boolean isPieceAlreadyPresent(int pieceIndex) {
        return peerProcess.bitFieldMsg.getFilePieces()[pieceIndex].getPresenceFlag() == 1;
    }

    /**
     * Logs a message and displays it in the console.
     *
     * @param message Message to log and display.
     */
    private static void logAndDisplay(String message) {
        LoggingHelper.logAndDisplay(message);
    }
}
