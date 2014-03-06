package i5.las2peer.services.shortMessageService;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * 
 * <p>
 * Data class that is used by the
 * {@link i5.las2peer.services.shortMessageService.ShortMessageService} to
 * transport messages.<br>
 * It contains the message itself as well as some meta-data that will be used to
 * categorize this message.
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

    /**
     * Constructor for a
     * {@link i5.las2peer.services.shortMessageService.ShortMessage}. Will be
     * called by the
     * {@link i5.las2peer.services.shortMessageService.ShortMessageService}
     * before a message is sent.
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
     * Get the id of the user this
     * {@link i5.las2peer.service.shortMessageService.ShortMessage} was sent
     * from
     * 
     * @return the user id
     */
    public long getSenderId() {
	return sender;
    }

    /**
     * Get the id of the user this
     * {@link i5.las2peer.service.shortMessageService.ShortMessage} should be
     * delivered to
     * 
     * @return the user id
     */
    public long getReceiverId() {
	return receiver;
    }

    /**
     * Gets the content of this
     * {@link i5.las2peer.service.shortMessageService.ShortMessage}
     * 
     * @return A String containing the content.
     */
    public String getMessage() {
	return content;
    }

    /**
     * Gets the time this
     * {@link i5.las2peer.services.shortMessageService.ShortMessage} was
     * created.
     * 
     * @return A {@link java.util.GregorianCalendar} containing the full time,
     *         date and timezone of creation.
     */
    public Calendar getCreateTimestamp() {
	return timeCreated;
    }

    public void setSendTimestamp(Calendar timestamp) {
	timeSend = timestamp;
    }

    public Calendar getSendTimestamp() {
	return timeSend;
    }

}
