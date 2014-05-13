package i5.las2peer.services.shortMessageService;

import i5.las2peer.communication.Message;
import i5.las2peer.persistency.MalformedXMLException;

import java.io.Serializable;
import java.util.ArrayList;

public class ShortMessageBox implements Serializable {

    private static final long serialVersionUID = -300617519857096303L;
    private final ArrayList<String> messages;

    public ShortMessageBox(int initialCapacity) {
        messages = new ArrayList<>(initialCapacity);
    }

    public void addMessage(Message msg) {
        messages.add(msg.toXmlString());
    }

    public Message[] getMessages() {
        ArrayList<Message> result = new ArrayList<Message>(messages.size());
        for (String xml : messages) {
            try {
                Message msg = Message.createFromXml(xml);
                result.add(msg);
            } catch (MalformedXMLException e) {
                // XXX logging
                e.printStackTrace();
            }
        }
        Message[] array = result.toArray(new Message[0]);
        return array;
    }
    
    public int size() {
        return messages.size();
    }
    
    public void clear() {
        messages.clear();
    }

}
