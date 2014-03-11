package i5.las2peer.services.shortMessageService;

import i5.las2peer.communication.Message;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Context;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.shortMessageService.StoredMessage.StoredMessageSendState;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
    private final String storageFilename;
    private ConcurrentMap<StoredMessageSendState, ConcurrentLinkedQueue<StoredMessage>> buffer;

    public ShortMessageStorage(Context context, ServiceAgent agent, String storageFilename) {
        this.context = context;
        this.agent = agent;
        this.storageFilename = storageFilename;
        // initialize buffer
        buffer = new ConcurrentHashMap<>();
        for (StoredMessageSendState state : StoredMessageSendState.values()) {
            buffer.put(state, new ConcurrentLinkedQueue<StoredMessage>());
        }
//        // TODO load messages from network envelope
//        try {
//            Envelope env = context.getStoredObject(this.getClass(), "shortMessage-storage");
//            env.open(agent);
//            StoredMessage[] messages = env.getContent(StoredMessage[].class);
//            env.addSignature(agent);
//            env.close();
//            for (StoredMessage msg : messages) {
//                addMessage(msg);
//            }
//        } catch (Exception e) {
//            Context.logError(this, "Can't open network storage " + e);
//        }
        // TODO add messages from local file to buffer
//        try {
//            FileInputStream fis = new FileInputStream(storageFilename);
//            ObjectInputStream ois = new ObjectInputStream(fis);
//            StoredMessage msg = null;
//            while ((msg = (StoredMessage) ois.readObject()) != null) {
//                addMessage(msg);
//            }
//            fis.close();
//        } catch (IOException | ClassNotFoundException e) {
//            Context.logError(this, "Can't read file storage " + e);
//        }
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
        // TODO persist buffer to envelope
        try {
            HashSet<StoredMessage> allMessages = new HashSet<>();
            for (ConcurrentLinkedQueue<StoredMessage> message : buffer.values()) {
                allMessages.addAll(message);
            }
            StoredMessage[] messageArray = allMessages.toArray(new StoredMessage[0]);
            Envelope env = context.getStoredObject(this.getClass(), "network-storage");
            env.open(agent);
            env.updateContent(messageArray);
            env.addSignature(agent);
            env.store();
        } catch (Exception e) {
            // TODO logging
            e.printStackTrace();
        }
        // TODO persist buffer to file
        try {
            FileOutputStream fos = new FileOutputStream(storageFilename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            for (ConcurrentLinkedQueue<StoredMessage> set : buffer.values()) {
                for (StoredMessage msg : set) {
                    oos.writeObject(msg);
                }
            }
            fos.close();
        } catch (IOException e) {
            // TODO logging
            e.printStackTrace();
        }
    }

    public List<Message> getUnreadMessages(UserAgent requestingAgent) {
        List<Message> result = new LinkedList<>();
        ConcurrentLinkedQueue<StoredMessage> delivered = buffer.get(StoredMessageSendState.DELIVERED);
        for (StoredMessage msg : delivered) {
            if (msg.getMessage().getRecipientId() == requestingAgent.getId() && msg.isRead() == false) {
                result.add(msg.getMessage());
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
