package i5.las2peer.services.shortMessageService.storage;

import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.services.shortMessageService.StoredMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NetworkStorage implements IStorage {

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayList<StoredMessage> getMessages(Context context, String storageId, Agent owner) {
        // load old messages from network
        try {
            Envelope env = context.getStoredObject(ArrayList.class, storageId);
            env.open(owner);
            String[] old = env.getContent(String[].class);
            env.close();
            Context.logMessage(this, "Loaded " + old.length + " from network storage");
            ArrayList<StoredMessage> result = new ArrayList<>(old.length);
            for (String o : old) {
                result.add(StoredMessage.createFromXml(o));
            }
            return result;
        } catch (Exception e) {
            Context.logError(this, "Can't open network storage " + e);
        }
        return new ArrayList<StoredMessage>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveMessages(Iterable<StoredMessage> messages, Context context, String storageId, Agent owner) {
        // persist to network shared objects
        try {
            Envelope env = null;
            try {
                env = context.getStoredObject(String[].class, storageId);
            } catch (Exception e) {
                // XXX logging
                e.printStackTrace();
                Context.logMessage(this, "Network storage not found. Creating new one");
                env = Envelope.createClassIdEnvelope(String[].class, storageId, owner);
            }
            env.open(owner);
            // convert messages to xml strings
            List<String> xmlMessages = new LinkedList<>();
            for (StoredMessage msg : messages) {
                xmlMessages.add(msg.toXmlString());
            }
            env.updateContent(xmlMessages.toArray(new String[0]));
            // close envelope
            env.addSignature(owner);
            env.store();
            Context.logMessage(this, "stored " + xmlMessages.size() + " messages in network storage");
        } catch (Exception e) {
            // XXX logging
            e.printStackTrace();
            Context.logError(this, "Can't persist short messages to network storage " + e);
        }
    }

}
