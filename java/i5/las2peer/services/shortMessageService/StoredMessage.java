package i5.las2peer.services.shortMessageService;

import i5.las2peer.communication.Message;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;

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
        return "<las2peer:storedmessage" + " state=\"" + state.ordinal() + "\" read=\"" + read + "\">\n"
                + message.toXmlString() + "</las2peer:storedmessage>\n";
    }

    public static StoredMessage createFromXml(String xml) throws MalformedXMLException {
        // TODO
        return null;
    }

}
