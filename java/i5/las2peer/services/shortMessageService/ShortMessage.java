package i5.las2peer.services.shortMessageService;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * 
 * <p>
 * Data class that is used by the {@link i5.las2peer.services.shortMessageService.ShortMessageService} to transport
 * messages.<br>
 * It contains the message itself as well as some meta-data that will be used to categorize this message.
 * 
 * @author Thomas Cujé
 * 
 */
public class ShortMessage implements Serializable {

    private static final long serialVersionUID = -6063641602984306828L;

    private final long sender;
    private final long receiver;
    private final String content;
    private final Calendar timeCreated;
    private Calendar timeSend;
    private Calendar timeReceive;
    private boolean read;

    /**
     * Constructor for a {@link i5.las2peer.services.shortMessageService.ShortMessage}. Will be called by the
     * {@link i5.las2peer.services.shortMessageService.ShortMessageService} before a message is sent.
     * 
     * @param from
     *            id of the user this message was sent from
     * @param to
     *            id of the user this message should be delivered to
     * @param message
     *            the message string itself
     */
    public ShortMessage(long from, long to, String message) {
        sender = from;
        receiver = to;
        content = message;
        timeCreated = new GregorianCalendar();
    }

    /**
     * Gets the id of the sender user agent this {@link i5.las2peer.services.shortMessageService.ShortMessage} was sent
     * from.
     * 
     * @return the user agent id
     */
    public long getSenderId() {
        return sender;
    }

    /**
     * Gets the id of the user agent this {@link i5.las2peer.services.shortMessageService.ShortMessage} should be
     * delivered to.
     * 
     * @return the user agent id
     */
    public long getReceiverId() {
        return receiver;
    }

    /**
     * Gets the content of this {@link i5.las2peer.services.shortMessageService.ShortMessage}
     * 
     * @return A String containing the actual message as String.
     */
    public String getMessage() {
        return content;
    }

    /**
     * Gets the timestamp this {@link i5.las2peer.services.shortMessageService.ShortMessage} was created.
     * 
     * @return Returns a {@link java.util.Calendar} containing the full time, date and timezone of creation.
     */
    public Calendar getCreateTimestamp() {
        return timeCreated;
    }

    /**
     * Sets the timestamp this {@link i5.las2peer.services.shortMessageService.ShortMessage} was sent. This function
     * should be called immediately before sending the message.
     * 
     * @param timestamp
     *            A {@link java.util.Calendar} with the timestamp when this message was send.
     */
    public void setSendTimestamp(Calendar timestamp) {
        timeSend = timestamp;
    }

    /**
     * Gets the sent timestamp for this {@link i5.las2peer.services.shortMessageService.ShortMessage}
     * 
     * @return Returns a {@link java.util.Calendar} with the sent timestamp.
     */
    public Calendar getSendTimestamp() {
        return timeSend;
    }

    /**
     * Sets the timestamp this {@link i5.las2peer.services.shortMessageService.ShortMessage} was received. This function
     * should be called immediately after receiving the message.
     * 
     * @param timestamp
     *            A {@link java.util.Calendar} with the timestamp when this message was received.
     */
    public void setReceiveTimestamp(Calendar timestamp) {
        timeReceive = timestamp;
    }

    /**
     * Gets the receive timestamp for this {@link i5.las2peer.services.shortMessageService.ShortMessage}
     * 
     * @return Returns a {@link java.util.Calendar} with the receive timestamp.
     */
    public Calendar getReceiveTimestamp() {
        return timeReceive;
    }

    /**
     * Gets the message read state as boolean
     * 
     * @return Returns a boolean representing the read state for this message
     */
    public boolean isRead() {
        return read;
    }

    /**
     * Sets the message read state
     * 
     * @param state
     *            true if the message was read, false otherwise
     */
    public void setRead(boolean read) {
        this.read = read;
    }

}
