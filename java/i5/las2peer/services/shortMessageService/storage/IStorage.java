package i5.las2peer.services.shortMessageService.storage;

import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.services.shortMessageService.StoredMessage;

import java.util.ArrayList;
import java.util.List;

public interface IStorage {

    /**
     * 
     * Gets all messages from the container with the specified storageId and opened by the owner's agent 
     * 
     * @param context just the l2p context
     * @param storageId identifier for the storage that should be used to get the messages
     * @param owner the agent of the owner for these messages
     * @return
     */
    public List<StoredMessage> getMessages(Context context, String storageId, Agent owner);
    
    /**
     * Saves the given message list inside this container under the specified storageId and owned by the given agent
     * 
     * @param messages the list of messages that should be persistet
     * @param context just the l2p context
     * @param storageId identifier for the storage that should be used to persist the messages
     * @param owner the agent of the owner for these messages
     */
    public void saveMessages(ArrayList<StoredMessage> messages, Context context, String storageId, Agent owner);
    
}
