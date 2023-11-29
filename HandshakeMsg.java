import java.io.UnsupportedEncodingException;

/**
 * This class manages the information in a handshake message.
 */
public class HandshakeMsg {

    private byte[] headerBytes = new byte[MsgConstants.HANDSHAKE_HEADER_LENGTH];
    private byte[] peerIDBytes = new byte[MsgConstants.HANDSHAKE_PEERID_LENGTH];
    private byte[] zeroBits = new byte[MsgConstants.HANDSHAKE_ZEROBITS_LENGTH];
    private String header;
    private String peerID;

    /**
     * Default constructor to instantiate a handshake object.
     */
    public HandshakeMsg() {}

    /**
     * Initializes a handshake object with a header and peerID.
     *
     * @param header - The handshake header.
     * @param peerID - The sending peer's ID.
     */
    public HandshakeMsg(String header, String peerID) {
        try {
            this.header = header;
            this.headerBytes = header.getBytes(MsgConstants.DEFAULT_CHARSET);
            if (this.headerBytes.length > MsgConstants.HANDSHAKE_HEADER_LENGTH)
                throw new Exception("Handshake Header is too large");

            this.peerID = peerID;
            this.peerIDBytes = peerID.getBytes(MsgConstants.DEFAULT_CHARSET);
            if (this.peerIDBytes.length > MsgConstants.HANDSHAKE_PEERID_LENGTH)
                throw new Exception("Handshake PeerID is too large");

            this.zeroBits = "0000000000".getBytes(MsgConstants.DEFAULT_CHARSET);
        } catch (Exception e) {
            // Handle the exception appropriately, e.g., logging it.
        }
    }

    /**
     * Converts a handshake message to a byte array.
     *
     * @param handshakeMsg - The handshake message to convert.
     * @return The handshake message in byte array format, or null if an error occurs.
     */
    public static byte[] toByteArray(HandshakeMsg handshakeMsg) {
        byte[] handshakeMessageInBytes = new byte[MsgConstants.HANDSHAKE_MESSAGE_LENGTH];
        try {
            if (handshakeMsg.getHeaderInBytes() == null ||
                    (handshakeMsg.getHeaderInBytes().length > MsgConstants.HANDSHAKE_HEADER_LENGTH || handshakeMsg.getHeaderInBytes().length == 0))
                throw new Exception("Handshake Message Header is Invalid");
            else
                System.arraycopy(handshakeMsg.getHeaderInBytes(), 0,
                        handshakeMessageInBytes, 0, handshakeMsg.getHeaderInBytes().length);

            if (handshakeMsg.getZeroBits() == null ||
                    (handshakeMsg.getZeroBits().length > MsgConstants.HANDSHAKE_ZEROBITS_LENGTH || handshakeMsg.getZeroBits().length == 0))
                throw new Exception("Handshake Message Zero Bits are Invalid");
            else
                System.arraycopy(handshakeMsg.getZeroBits(), 0,
                        handshakeMessageInBytes, MsgConstants.HANDSHAKE_HEADER_LENGTH, MsgConstants.HANDSHAKE_ZEROBITS_LENGTH - 1);

            if (handshakeMsg.getPeerIDInBytes() == null ||
                    (handshakeMsg.getPeerIDInBytes().length > MsgConstants.HANDSHAKE_PEERID_LENGTH || handshakeMsg.getPeerIDInBytes().length == 0))
                throw new Exception("Handshake Message Peer ID is Invalid");
            else
                System.arraycopy(handshakeMsg.getPeerIDInBytes(), 0, handshakeMessageInBytes,
                        MsgConstants.HANDSHAKE_HEADER_LENGTH + MsgConstants.HANDSHAKE_ZEROBITS_LENGTH,
                        handshakeMsg.getPeerIDInBytes().length);
        } catch (Exception e) {
            handshakeMessageInBytes = null;

        }

        return handshakeMessageInBytes;
    }

    /**
     * Converts a byte array to a handshake message.
     *
     * @param byteArray - The byte array to convert.
     * @return A handshake message object, or null if an error occurs.
     */
    public static HandshakeMsg fromByteArray(byte[] byteArray) {
        HandshakeMsg message = null;

        try {
            if (byteArray.length != MsgConstants.HANDSHAKE_MESSAGE_LENGTH)
                throw new Exception("While Decoding Handshake message length is invalid");
            message = new HandshakeMsg();
            byte[] messageHeader = new byte[MsgConstants.HANDSHAKE_HEADER_LENGTH];
            byte[] messagePeerID = new byte[MsgConstants.HANDSHAKE_PEERID_LENGTH];

            System.arraycopy(byteArray, 0, messageHeader, 0,
                    MsgConstants.HANDSHAKE_HEADER_LENGTH);
            System.arraycopy(byteArray, MsgConstants.HANDSHAKE_HEADER_LENGTH
                            + MsgConstants.HANDSHAKE_ZEROBITS_LENGTH, messagePeerID, 0,
                    MsgConstants.HANDSHAKE_PEERID_LENGTH);

            message.setHeaderFromBytes(messageHeader);
            message.setPeerIDFromBytes(messagePeerID);

        } catch (Exception e) {

        }
        return message;
    }

    /**
     * This method is used to set peerID from byte array
     * @param messagePeerID - byte array of peerID
     */
    public void setPeerIDFromBytes(byte[] messagePeerID) {
        try {
            peerID = (new String(messagePeerID, MsgConstants.DEFAULT_CHARSET)).trim();
            peerIDBytes = messagePeerID;
        } catch (UnsupportedEncodingException e) {
            logAndShowInConsole(e.getMessage());
        }
    }

    /**
     * This message is used to set handshake header from byte array
     * @param messageHeader - handshake header in bytes
     */
    public void setHeaderFromBytes(byte[] messageHeader) {
        try {
            header = (new String(messageHeader, MsgConstants.DEFAULT_CHARSET)).trim();
            headerBytes = messageHeader;
        } catch (UnsupportedEncodingException e) {
            logAndShowInConsole(e.getMessage());
        }
    }

    /**
     * This method is used to get handshake header value in bytes
     * @return - headerInBytes
     */
    public byte[] getHeaderInBytes() {
        return headerBytes;
    }

    /**
     * This method is used to set handshake header value in bytes
     * @param headerInBytes
     */
    public void setHeaderInBytes(byte[] headerInBytes) {
        this.headerBytes = headerInBytes;
    }

    /**
     * This method is used to get handshake peerID in bytes
     * @return peerIDInBytes
     */
    public byte[] getPeerIDInBytes() {
        return peerIDBytes;
    }

    /**
     * This method is used to set handshake peerID in bytes
     * @param peerIDInBytes
     */
    public void setPeerIDInBytes(byte[] peerIDInBytes) {
        this.peerIDBytes = peerIDInBytes;
    }

    /**
     * This method is used to get handshake zero bits
     * @return zeroBits
     */
    public byte[] getZeroBits() {
        return zeroBits;
    }

    /**
     * This method is used to set handshake zero bits
     * @param zeroBits
     */
    public void setZeroBits(byte[] zeroBits) {
        this.zeroBits = zeroBits;
    }

    /**
     * This method is used to get handshake header
     * @return header
     */
    public String getHeader() {
        return header;
    }

    /**
     * This method is used to set handshake header
     * @param header
     */
    public void setHeader(String header) {
        this.header = header;
    }

    /**
     * This method is used to get handshake peerID
     * @return peerID
     */
    public String getPeerID() {
        return peerID;
    }

    /**
     * This method is used to set handshake peerID
     * @param peerID
     */
    public void setPeerID(String peerID) {
        this.peerID = peerID;
    }

    /**
     * This method is used to log a message in a log file and show it in console
     * @param message - message to be logged and showed in console
     */
    private static void logAndShowInConsole(String message) {
        LoggingHelper.logAndDisplay(message);
    }
}
