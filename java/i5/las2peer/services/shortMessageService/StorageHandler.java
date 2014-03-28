package i5.las2peer.services.shortMessageService;

import i5.las2peer.communication.Message;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.shortMessageService.StoredMessage.StoredMessageSendState;
import i5.las2peer.services.shortMessageService.storage.IStorage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class StorageHandler {

    private final String storageIdentifier;
    private final List<IStorage> storages;

    public StorageHandler(String storageIdentifier) {
        this.storageIdentifier = storageIdentifier;
        this.storages = new LinkedList<>();
    }

    /**
     * Register a new storage to be used to persist messages
     * 
     * @param storage
     *            An object implementing the IStorage interface
     */
    public void registerStorage(IStorage storage) {
        storages.add(storage);
    }

    /**
     * Unregister a storage that was used to persist messages
     * 
     * @param storage
     *            The previously added storage object that should be removed
     */
    public void unregisterStorage(IStorage storage) {
        storages.remove(storage);
    }

    /**
     * Gets all messages with the specified state from all registered storages for the requesting agent
     * 
     * @param context
     * @param requestingAgent
     * @param state
     * @return Returns an ArrayList containing the messages
     */
    public List<Message> getMessages(Context context, UserAgent requestingAgent, StoredMessageSendState state) {
        Set<Message> set = new HashSet<>();
        // load and merge messages from all registered storages
        for (IStorage storage : storages) {
            List<StoredMessage> list = storage.getMessages(context, storageIdentifier + requestingAgent.getId(),
                    requestingAgent);
            for (StoredMessage msg : list) {
                if (msg.getState().equals(state)) {
                    // TODO add versioning instead of overwrite and replace
                    set.remove(msg.getMessage());
                    set.add(msg.getMessage());
                }
            }
        }
        ArrayList<Message> result = new ArrayList<>(set);
        return result;
    }

    /**
     * Persists a message in all registered storages
     * 
     * @param stored
     *            the message that should be persistet
     * @param owner
     *            the agent of the owner of this message
     */
    public void persistMessage(Context context, StoredMessage stored, Agent owner) {
        ArrayList<StoredMessage> messages = new ArrayList<>();
        // load and merge messages from all registered storages
        for (IStorage storage : storages) {
            List<StoredMessage> list = storage.getMessages(context, storageIdentifier + owner.getId(), owner);
            // TODO add versioning instead of overwrite and replace
            messages.removeAll(list);
            messages.addAll(list);
        }
        // replace or add message
        messages.remove(stored);
        messages.add(stored);
        // persist messages to all registered storages
        for (IStorage storage : storages) {
            storage.saveMessages(messages, context, storageIdentifier + owner.getId(), owner);
        }
    }

}
