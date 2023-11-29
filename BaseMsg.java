import java.io.UnsupportedEncodingException;

/**
 * Class which handles messages except handshake, bitfield and piece messages
 */
public class BaseMsg {
    //Type of message
    private String type;
    //Length of the message
    private String length;
    //The length of data in the message
    private int dataLength = MsgConstants.MESSAGE_TYPE;
    //Type of message in bytes
    private byte[] typeInBytes = null;
    //Length of the message in bytes
    private byte[] lengthInBytes = null;
    //The content of the message
    private byte[] payload = null;

    /**
     * Empty constructor to create message object
     */
    public BaseMsg() {
    }

    /**
     * Constructor to create message object based on message type
     * @param messageType - type of the message
     */
    public BaseMsg(String messageType) {
        try {
            if (messageType == MsgConstants.MESSAGE_INTERESTED || messageType == MsgConstants.MESSAGE_NOT_INTERESTED ||
                    messageType == MsgConstants.MESSAGE_CHOKE || messageType == MsgConstants.MESSAGE_UNCHOKE
                    || messageType == MsgConstants.MESSAGE_DOWNLOADED) {
                setMessageLength(1);
                setMessageType(messageType);
                this.payload = null;
            } else {
                logAndShowInConsole("Error Occurred while initialzing Message constructor");
                throw new Exception("Message Constructor - Wrong constructor selected");
            }
        } catch (Exception e) {
            logAndShowInConsole(e.getMessage());
        }
    }

    /**
     * Constructor to create message object based on message type and payload
     * @param messageType - type of the message
     * @param payload - message payload
     */
    public BaseMsg(String messageType, byte[] payload) {
        try {
            if (payload != null) {
                setMessageLength(payload.length + 1);
                if (lengthInBytes.length > MsgConstants.MESSAGE_LENGTH) {
                    logAndShowInConsole("Error Occurred while initialzing Message constructor");
                    throw new Exception("Message Constructor - Message Length is too large");
                }
                setPayload(payload);
            } else {
                if (messageType == MsgConstants.MESSAGE_INTERESTED || messageType == MsgConstants.MESSAGE_NOT_INTERESTED
                        || messageType == MsgConstants.MESSAGE_CHOKE || messageType == MsgConstants.MESSAGE_UNCHOKE
                        || messageType == MsgConstants.MESSAGE_DOWNLOADED) {
                    setMessageLength(1);
                    this.payload = null;
                } else {
                    logAndShowInConsole("Error Occurred while initialzing Message constructor");
                    throw new Exception("Message Constructor - Message Payload should not be null");
                }
            }
            setMessageType(messageType);
            if (typeInBytes.length > MsgConstants.MESSAGE_TYPE) {
                logAndShowInConsole("Error Occurred while initialzing Message constructor");
                throw new Exception("Message Constructor - Message Type length is too large");
            }
        } catch (Exception e) {
            logAndShowInConsole("Error Occurred while initialzing Message constructor - " + e.getMessage());
        }
    }

    /**
     * This method is used to set message type and message type in bytes with message type received in params
     * @param messageType - type of message to be set
     */
    public void setMessageType(String messageType) {
        type = messageType.trim();
        try {
            typeInBytes = messageType.getBytes(MsgConstants.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            logAndShowInConsole(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is used to set message length, data length and message length in bytes with message length received in params
     * @param messageLength - length of message to be set
     */
    public void setMessageLength(int messageLength) {
        dataLength = messageLength;
        length = ((Integer) messageLength).toString();
        lengthInBytes = PeerUtils.integerToBytes(messageLength);
    }

    /**
     * This method is used to set message length, data length and message length in bytes with message length received in params
     * @param len - length of message to be set
     */
    public void setMessageLength(byte[] len) {

        Integer l = PeerUtils.bytesToInteger(len);
        this.length = l.toString();
        this.lengthInBytes = len;
        this.dataLength = l;
    }

    /**
     * This method is used to set message type and message type in bytes with message type received in params
     * @param type - type of message to be set
     */
    public void setMessageType(byte[] type) {
        try {
            this.type = new String(type, MsgConstants.DEFAULT_CHARSET);
            this.typeInBytes = type;
        } catch (UnsupportedEncodingException e) {
            logAndShowInConsole(e.toString());
        }
    }

    /**
     * This method is used to return message data length
     * @return message data length
     */
    public int getMessageLengthAsInteger() {
        return this.dataLength;
    }

    /**
     * This method is used to convert message to byte array
     * @param baseMsg - Message instance to be converted
     * @return byte array of the message
     */
    public static byte[] convertMessageToByteArray(BaseMsg baseMsg) {
        byte[] messageInByteArray = null;
        try {
            int messageType = Integer.parseInt(baseMsg.getType());
            if (baseMsg.getLengthInBytes().length > MsgConstants.MESSAGE_LENGTH)
                throw new Exception("Message Length is Invalid.");
            else if (messageType < 0 || messageType > 8)
                throw new Exception("Message Type is Invalid.");
            else if (baseMsg.getTypeInBytes() == null)
                throw new Exception("Message Type is Invalid.");
            else if (baseMsg.getLengthInBytes() == null)
                throw new Exception("Message Length is Invalid.");

            if (baseMsg.getPayload() != null) {
                messageInByteArray = new byte[MsgConstants.MESSAGE_LENGTH + MsgConstants.MESSAGE_TYPE + baseMsg.getPayload().length];
                System.arraycopy(baseMsg.getLengthInBytes(), 0, messageInByteArray, 0, baseMsg.getLengthInBytes().length);
                System.arraycopy(baseMsg.getTypeInBytes(), 0, messageInByteArray, MsgConstants.MESSAGE_LENGTH, MsgConstants.MESSAGE_TYPE);
                System.arraycopy(baseMsg.getPayload(), 0, messageInByteArray,
                        MsgConstants.MESSAGE_LENGTH + MsgConstants.MESSAGE_TYPE, baseMsg.getPayload().length);
            } else {
                messageInByteArray = new byte[MsgConstants.MESSAGE_LENGTH + MsgConstants.MESSAGE_TYPE];
                System.arraycopy(baseMsg.getLengthInBytes(), 0, messageInByteArray, 0, baseMsg.getLengthInBytes().length);
                System.arraycopy(baseMsg.getTypeInBytes(), 0, messageInByteArray, MsgConstants.MESSAGE_LENGTH, MsgConstants.MESSAGE_TYPE);
            }
        } catch (Exception e) {
        }

        return messageInByteArray;
    }

    /**
     * This method is used to convert byte array into message object
     * @param message - byte array to be converted
     * @return message instance
     */
    public static BaseMsg convertByteArrayToMessage(byte[] message) {

        BaseMsg msg = new BaseMsg();
        byte[] msgLength = new byte[MsgConstants.MESSAGE_LENGTH];
        byte[] msgType = new byte[MsgConstants.MESSAGE_TYPE];
        byte[] payLoad = null;
        int len;

        try {
            if (message == null)
                throw new Exception("Invalid data.");
            else if (message.length < MsgConstants.MESSAGE_LENGTH + MsgConstants.MESSAGE_TYPE)
                throw new Exception("Byte array length is too small...");


            System.arraycopy(message, 0, msgLength, 0, MsgConstants.MESSAGE_LENGTH);
            System.arraycopy(message, MsgConstants.MESSAGE_LENGTH, msgType, 0, MsgConstants.MESSAGE_TYPE);

            msg.setMessageLength(msgLength);
            msg.setMessageType(msgType);

            len = PeerUtils.bytesToInteger(msgLength);

            if (len > 1) {
                payLoad = new byte[len - 1];
                System.arraycopy(message, MsgConstants.MESSAGE_LENGTH + MsgConstants.MESSAGE_TYPE, payLoad, 0, message.length - MsgConstants.MESSAGE_LENGTH - MsgConstants.MESSAGE_TYPE);
                msg.setPayload(payLoad);
            }

            payLoad = null;
        } catch (Exception e) {
            LoggingHelper.logAndDisplay(e.toString());
            msg = null;
        }
        return msg;
    }

    /**
     * This method is used to get the type of message
     * @return type of message
     */
    public String getType() {
        return type;
    }

    /**
     * This method is used to set the type of message
     * @param type - type of message
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * This method is used to get the length of message
     * @return length of message
     */
    public String getLength() {
        return length;
    }

    /**
     * This method is used to set the length of message
     * @param length - length of the message
     */
    public void setLength(String length) {
        this.length = length;
    }

    /**
     * This method is used to get the type of message in bytes
     * @return type in bytes
     */
    public byte[] getTypeInBytes() {
        return typeInBytes;
    }

    /**
     * This method is used to set the type of message in bytes
     * @param typeInBytes - type in bytes
     */
    public void setTypeInBytes(byte[] typeInBytes) {
        this.typeInBytes = typeInBytes;
    }

    /**
     * This method is used to get the length of message in bytes
     * @return length of the message in bytes
     */
    public byte[] getLengthInBytes() {
        return lengthInBytes;
    }

    /**
     * This method is used to set the length of message in bytes
     * @param lengthInBytes - length of the message in bytes
     */
    public void setLengthInBytes(byte[] lengthInBytes) {
        this.lengthInBytes = lengthInBytes;
    }

    /**
     * This method is used to get the content of message
     * @return content of message
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * This method is used to set the content of message
     * @param payload - content of message
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    /**
     * This method is used to log a message in a log file and show it in console
     * @param message - message to be logged and showed in console
     */
    private static void logAndShowInConsole(String message) {
        LoggingHelper.logAndDisplay(message);
    }

}
