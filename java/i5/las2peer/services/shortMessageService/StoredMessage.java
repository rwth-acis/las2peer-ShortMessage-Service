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
        NEW, DELIVERED, TIMEDOUT,
    }

    private StoredMessageSendState state;
    private boolean read;
    private final Message message;

    public StoredMessage(Message message, StoredMessageSendState state) {
        this.message = message;
        this.state = state;
        read = false;
    }

    public void setState(StoredMessageSendState state) {
        this.state = state;
    }

    public Message getMessage() {
        return message;
    }

    public StoredMessageSendState getState() {
        return state;
    }

    public void setRead(boolean state) {
        read = state;
    }

    public boolean isRead() {
        return read;
    }

    @Override
    public String toXmlString() {
        return "<las2peer:storedmessage" + " state=\"" + state + "\" read=\"" + read + "\">\n" + message.toXmlString()
                + "</las2peer:storedmessage>\n";
    }

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
