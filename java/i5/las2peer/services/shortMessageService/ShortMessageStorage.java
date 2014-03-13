package i5.las2peer.services.shortMessageService;

import i5.las2peer.communication.Message;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.security.Context;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.shortMessageService.StoredMessage.StoredMessageSendState;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class ShortMessageStorage {

    private final Context context;
    private final ServiceAgent agent;
    private final String storageIdentifier;
    private final String storageFilename;
    private ConcurrentMap<StoredMessageSendState, ConcurrentLinkedQueue<StoredMessage>> buffer;

    public ShortMessageStorage(Context context, ServiceAgent agent, String storageIdentifier, String storageFilename) {
        this.context = context;
        this.agent = agent;
        this.storageIdentifier = storageIdentifier;
        this.storageFilename = storageFilename;
        // initialize buffer
        buffer = new ConcurrentHashMap<>();
        for (StoredMessageSendState state : StoredMessageSendState.values()) {
            buffer.put(state, new ConcurrentLinkedQueue<StoredMessage>());
        }
        // load messages from network storage
        try {
            Envelope env = context.getStoredObject(this.getClass(), storageIdentifier);
            env.open(agent);
            String[] messages = env.getContent(String[].class);
            env.addSignature(agent);
            env.close();
            long counter = 0;
            for (String str : messages) {
                StoredMessage msg = StoredMessage.createFromXml(str.toString());
                if (msg != null) {
                    ConcurrentLinkedQueue<StoredMessage> set = buffer.get(msg.getState());
                    set.add(msg);
                    counter++;
                } else {
                    Context.logError(this, "Failed parsing message " + counter + " of " + messages.length);
                }
            }
            Context.logMessage(this, "Restored " + counter + " of " + messages.length
                    + " messages from network storage");
        } catch (Exception e) {
            Context.logError(this, "Can't open network storage " + e);
        }
        // add messages from local xml file to buffer
        try {
            // XXX how about database support e. g. postgres, mysql or at least sqlite?
            Element root = Parser.parse(new File(storageFilename), false);
            int counter = 0;
            int childCount = root.getChildCount();
            for (int n = 0; n < childCount; n++) {
                try {
                    Element e = root.getChild(n);
                    StoredMessage msg = StoredMessage.createFromXml(e.toString());
                    if (msg != null) {
                        ConcurrentLinkedQueue<StoredMessage> set = buffer.get(msg.getState());
                        set.add(msg);
                        counter++;
                    } else {
                        Context.logError(this, "Failed parsing message " + n + " of " + childCount);
                    }
                } catch (XMLSyntaxException | MalformedXMLException e1) {
                    Context.logError(this, "Failed parsing stored message from xml string " + e1);
                }
            }
            Context.logMessage(this, "Restored " + counter + " of " + childCount + " messages from local file storage");
        } catch (XMLSyntaxException | IOException e) {
            Context.logError(this, "Failure parsing xml file " + e);
        }
    }

    /**
     * Persist the given message inside the buffer
     * 
     * @param message
     */
    public void addMessage(StoredMessage message) {
        ConcurrentLinkedQueue<StoredMessage> set = buffer.get(message.getState());
        set.add(message);
        persistBuffer();
    }

    private void persistBuffer() {
        // persist buffer to envelope
        HashSet<StoredMessage> allMessages = new HashSet<>();
        for (ConcurrentLinkedQueue<StoredMessage> message : buffer.values()) {
            allMessages.addAll(message);
        }
        List<String> xmlMessages = new ArrayList<String>(allMessages.size());
        for (StoredMessage msg : allMessages) {
            xmlMessages.add(msg.toXmlString());
        }
        String[] messageArray = xmlMessages.toArray(new String[0]);
        try {
            Envelope env = null;
            try {
                env = context.getStoredObject(messageArray.getClass(), storageIdentifier);
            } catch (Exception e) {
                Context.logMessage(this, "Network storage not found. Creating new one");
                env = Envelope.createClassIdEnvelope(messageArray, storageIdentifier, agent);
            }
            env.open(agent);
            env.updateContent(messageArray);
            env.addSignature(agent);
            env.store();
            Context.logMessage(this, "stored " + messageArray.length + " messages in network storage");
        } catch (Exception e) {
            Context.logError(this, "Can't persist short messages to network storage " + e);
        }
        // persist buffer to xml file
        // XXX how about database support e. g. postgres, mysql or at least sqlite?
        BufferedWriter writer = null;
        long counter = 0;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(storageFilename)));
            writer.write("<las2peer:ShortMessageStorage>\n");
            for (ConcurrentLinkedQueue<StoredMessage> queue : buffer.values()) {
                for (StoredMessage msg : queue) {
                    writer.write(msg.toXmlString());
                    counter++;
                }
            }
            writer.write("</las2peer:ShortMessageStorage>\n");
        } catch (FileNotFoundException e) {
            Context.logError(this, "Can't create local persistence file " + e);
        } catch (IOException e) {
            Context.logError(this, "Failure saving to local persistence file " + e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                Context.logError(this, "Can't close writer " + e);
            }
        }
        Context.logMessage(this, "stored " + counter + " messages in local file storage");
    }

    public List<Message> getUnreadMessages(UserAgent requestingAgent) {
        List<Message> result = new LinkedList<>();
        ConcurrentLinkedQueue<StoredMessage> delivered = buffer.get(StoredMessageSendState.DELIVERED);
        for (StoredMessage msg : delivered) {
            if (msg.getMessage().getRecipientId() == requestingAgent.getId() && msg.isRead() == false) {
                result.add(msg.getMessage());
                msg.setRead(true);
            }
        }
        return result;
    }

    public void storeMessage(StoredMessage message) {
        // first remove the message from all buffers
        for (Entry<StoredMessageSendState, ConcurrentLinkedQueue<StoredMessage>> entry : buffer.entrySet()) {
            entry.getValue().remove(message);
        }
        // add new message
        addMessage(message);
    }

}
