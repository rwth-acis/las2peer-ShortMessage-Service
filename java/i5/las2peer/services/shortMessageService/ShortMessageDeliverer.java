package i5.las2peer.services.shortMessageService;

import i5.las2peer.execution.L2pThread;
import i5.las2peer.p2p.MessageResultListener;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.services.shortMessageService.StoredMessage.StoredMessageSendState;

/**
 * 
 * @author Thomas Cujé
 * 
 */
public class ShortMessageDeliverer extends L2pThread {

    private final StorageHandler storage;
    private final Node sender;
    private final StoredMessage toDeliver;
    private final long timeout;
    private final Agent owner;

    public ShortMessageDeliverer(ServiceAgent agent, Context context, StorageHandler storage, Node sendingNode,
            StoredMessage toDeliver, long timeout, Agent sendingAgent) {
        super(agent, null, context);
        this.storage = storage;
        this.sender = sendingNode;
        this.toDeliver = toDeliver;
        this.timeout = timeout;
        this.owner = sendingAgent;
    }

    @Override
    public void run() {
        // send message
        MessageResultListener resultListener = new MessageResultListener(timeout);
        sender.sendMessage(toDeliver.getMessage(), resultListener);
        // wait for result
        try {
            resultListener.waitForAllAnswers();
        } catch (InterruptedException e) {
        }
        if (resultListener.isSuccess() == true) {
            Context.logMessage(this, "Message successfully send");
            toDeliver.setState(StoredMessageSendState.DELIVERED);
            storage.persistMessage(getContext(), toDeliver, toDeliver.getMessage().getSender());
        } else {
            if (resultListener.isTimedOut() == true) {
                Context.logMessage(this, "Message timed out");
            } else {
                Context.logError(this,
                        "Something weird happened while sending a message. Neither success nor timed out. Message is scheduled for redelivery.");
            }
            toDeliver.setState(StoredMessageSendState.TIMEDOUT);
            storage.persistMessage(getContext(), toDeliver, owner);
        }
    }

}
