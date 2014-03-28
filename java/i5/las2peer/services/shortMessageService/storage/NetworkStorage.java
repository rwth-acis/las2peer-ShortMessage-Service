package i5.las2peer.services.shortMessageService.storage;

import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.services.shortMessageService.StoredMessage;

import java.util.ArrayList;
import java.util.Arrays;

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
            StoredMessage[] old = env.getContent(StoredMessage[].class);
            env.close();
            Context.logMessage(this, "Loaded " + old.length + " from network storage");
            ArrayList<StoredMessage> result = new ArrayList<>(old.length);
            result.addAll(Arrays.asList(old));
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
    public void saveMessages(ArrayList<StoredMessage> messages, Context context, String storageId, Agent owner) {
        // persist to network shared objects
        try {
            Envelope env = null;
            try {
                env = context.getStoredObject(ArrayList.class, storageId);
            } catch (Exception e) {
                Context.logMessage(this, "Network storage not found. Creating new one");
                env = Envelope.createClassIdEnvelope(ArrayList.class, storageId, owner);
            }
            env.open(owner);
            env.updateContent(messages);
            env.addSignature(owner);
            env.store();
            Context.logMessage(this, "stored " + messages.size() + " messages in network storage");
        } catch (Exception e) {
            Context.logError(this, "Can't persist short messages to network storage " + e);
        }
    }

}
