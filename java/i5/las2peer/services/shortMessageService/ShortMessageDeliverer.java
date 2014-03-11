package i5.las2peer.services.shortMessageService;

import i5.las2peer.p2p.MessageResultListener;
import i5.las2peer.p2p.Node;
import i5.las2peer.services.shortMessageService.StoredMessage.StoredMessageSendState;

public class ShortMessageDeliverer implements Runnable {

    private final ShortMessageStorage storage;
    private final Node sender;
    private final StoredMessage toDeliver;
    private final long timeout;

    public ShortMessageDeliverer(ShortMessageStorage storage, Node sendingNode, StoredMessage toDeliver, long timeout) {
        this.storage = storage;
        this.sender = sendingNode;
        this.toDeliver = toDeliver;
        this.timeout = timeout;
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
            // TODO logging
            System.out.println("Message successfully send");
            toDeliver.setState(StoredMessageSendState.DELIVERED);
            storage.storeMessage(toDeliver);
        } else {
            if (resultListener.isTimedOut() == true) {
                // TODO logging
                System.out.println("Message timed out");
            } else {
                // TODO logging
            }
            toDeliver.setState(StoredMessageSendState.TIMEDOUT);
            storage.storeMessage(toDeliver);
        }
    }
}
