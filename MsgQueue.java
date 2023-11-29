import java.util.LinkedList;
import java.util.Queue;

/**
 * Represents a queue that manages and processes messages received from a socket.
 */
public class MsgQueue {

    /**
     * A queue that holds and manages the details of the messages received from a socket.
     */
    private static Queue<MsgDetails> messagesQueue = new LinkedList<>();

    /**
     * Adds a message to the queue.
     * @param msgDetails - the message details to be queued.
     */
    public static synchronized void enqueueMessage(MsgDetails msgDetails) {
        messagesQueue.add(msgDetails);
    }

    /**
     * Retrieves and removes a message from the queue.
     * @return the message details removed from the queue, or null if the queue is empty.
     */
    public static synchronized MsgDetails dequeueMessage() {
        return !messagesQueue.isEmpty() ? messagesQueue.remove() : null;
    }
}

