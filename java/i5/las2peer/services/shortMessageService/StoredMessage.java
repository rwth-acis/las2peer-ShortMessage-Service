package i5.las2peer.services.shortMessageService;

import i5.las2peer.communication.Message;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.Context;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

/**
 * 
 * <p>
 * Data class that is used by the {@link i5.las2peer.services.shortMessageService.ShortMessageStorage} to persist
 * messages.<br>
 * It contains the las2peer message as well as some meta-data that will be used to categorize this message.
 * 
 * @author Thomas Cujé
 * 
 */
public class StoredMessage implements XmlAble {

    public enum StoredMessageSendState {
        NEW, // the message was just created and should be send to recipient now
        TIMEDOUT, // the message timed out while sending and should be resend or droped
        DELIVERED, // the message was delivered to the recipient agent (used by sending node)
        RECEIVED, // the message was received by the recipient agent (used by receiving node)
    }

    private StoredMessageSendState state;
    private boolean read;
    private final Message message;

    public StoredMessage(Message message, StoredMessageSendState state) {
        this.message = message;
        this.state = state;
        read = false;
    }

    /**
     * Sets the message state
     * 
     * @param state
     *            to set
     */
    public void setState(StoredMessageSendState state) {
        this.state = state;
    }

    /**
     * Gets the stored message object
     * 
     * @return Returns the message object
     */
    public Message getMessage() {
        return message;
    }

    /**
     * Gets the message state
     * 
     * @return Returns the message state
     */
    public StoredMessageSendState getState() {
        return state;
    }

    /**
     * Sets the message read state
     * 
     * @param state
     *            true if the message was read, false otherwise
     */
    public void setRead(boolean state) {
        read = state;
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
     * {@inheritDoc}
     */
    @Override
    public String toXmlString() {
        return "<las2peer:" + this.getClass().getSimpleName() + " state=\"" + state + "\" read=\"" + read + "\">\n"
                + message.toXmlString() + "</las2peer:" + this.getClass().getSimpleName() + ">\n";
    }

    /**
     * Create a {@link i5.las2peer.services.shortMessageService.StoredMessage} from a xml string.
     * 
     * @param xml
     *            String representing the xml encoded object
     * @return Returns a {@link i5.las2peer.services.shortMessageService.StoredMessage} object
     * @throws MalformedXMLException
     *             when the String could not be parsed. See exception message for details.
     */
    public static StoredMessage createFromXml(String xml) throws MalformedXMLException {
        try {
            Element root = Parser.parse(xml, false);
            int childCount = root.getChildCount();
            if (childCount != 1) {
                throw new MalformedXMLException("Stored messages must have one message. Found " + childCount);
            }
            // read message
            Message msg = Message.createFromXml(root.getChild(0).toString());
            // read state
            String stateStr = root.getAttribute("state");
            StoredMessageSendState msgState = StoredMessageSendState.valueOf(stateStr);
            return new StoredMessage(msg, msgState);
        } catch (XMLSyntaxException e) {
            Context.logError(StoredMessage.class, "Can't create stored message from xml string " + e);
        }
        return null;
    }

}
