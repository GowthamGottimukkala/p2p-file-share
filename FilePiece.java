/**
 * Represents a piece of a file with associated metadata.
 */
public class FilePiece {

    private int presenceFlag; // Indicates whether the piece is present or not.
    private String retrievedFromPeerId; // ID of the peer from which the piece was retrieved.
    private byte[] data; // Content of the file piece.
    private int index; // Position of the piece in the sequence.

    /**
     * Initializes a new FilePiece with default values.
     */
    public FilePiece() {
        data = new byte[ConfigSettings.pieceSize];
        index = -1;
        presenceFlag = 0;
        retrievedFromPeerId = null;
    }

    /**
     * Retrieves the presence status of the piece.
     * @return 0 if the piece is not present, 1 if it is present.
     */
    public int getPresenceFlag() {
        return presenceFlag;
    }

    /**
     * Sets the presence status of the piece.
     * @param presenceFlag The presence status (0 or 1).
     */
    public void setPresenceFlag(int presenceFlag) {
        this.presenceFlag = presenceFlag;
    }

    /**
     * Gets the ID of the peer from which the piece was retrieved.
     * @return The peer ID.
     */
    public String getRetrievedFromPeerId() {
        return retrievedFromPeerId;
    }

    /**
     * Sets the ID of the peer from which the piece was retrieved.
     * @param retrievedFromPeerId The peer ID.
     */
    public void setRetrievedFromPeerId(String retrievedFromPeerId) {
        this.retrievedFromPeerId = retrievedFromPeerId;
    }

    /**
     * Gets the content of the file piece.
     * @return The content as a byte array.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Sets the content of the file piece.
     * @param data The content as a byte array.
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Retrieves the position of the piece in the sequence.
     * @return The index of the piece.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the position of the piece in the sequence.
     * @param index The index of the piece.
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Converts a byte array representation of a file piece to a FilePiece object.
     * @param bytesPayload Byte array representation of the file piece.
     * @return Corresponding FilePiece object.
     */
    public static FilePiece fromByteArray(byte[] bytesPayload) {
        byte[] indexBytes = new byte[MsgConstants.PIECE_INDEX_LENGTH];
        FilePiece piece = new FilePiece();

        System.arraycopy(bytesPayload, 0, indexBytes, 0, MsgConstants.PIECE_INDEX_LENGTH);
        piece.setIndex(PeerUtils.bytesToInteger(indexBytes));

        byte[] content = new byte[bytesPayload.length - MsgConstants.PIECE_INDEX_LENGTH];
        System.arraycopy(bytesPayload, MsgConstants.PIECE_INDEX_LENGTH, content, 0, bytesPayload.length - MsgConstants.PIECE_INDEX_LENGTH);
        piece.setData(content);

        return piece;
    }
}
