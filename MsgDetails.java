/**
 * Represents the details associated with a message, including its metadata.
 */
public class MsgDetails {

    // The actual message that has been sent or received.
    private BaseMsg baseMsg;

    // The unique identifier of the sender peer.
    private String senderPeerID;

    /**
     * Initializes a new instance of the MessageDetails class, setting the initial fields.
     */
    public MsgDetails() {
        this.baseMsg = new BaseMsg();
        this.senderPeerID = null;
    }

    /**
     * Retrieves the message.
     * @return The current message.
     */
    public BaseMsg getMessage() {
        return baseMsg;
    }

    /**
     * Assigns a new message.
     * @param baseMsg The message to be assigned.
     */
    public void setMessage(BaseMsg baseMsg) {
        this.baseMsg = baseMsg;
    }

    /**
     * Retrieves the unique identifier of the sender peer.
     * @return The sender's peer ID.
     */
    public String getSenderPeerID() {
        return senderPeerID;
    }

    /**
     * Assigns a new sender peer ID.
     * @param senderPeerID The sender peer ID to be assigned.
     */
    public void setSenderPeerID(String senderPeerID) {
        this.senderPeerID = senderPeerID;
    }
}
