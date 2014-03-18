package i5.las2peer.services.shortMessageService;

import i5.las2peer.api.Service;
import i5.las2peer.communication.Message;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.Node;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.Context;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.shortMessageService.StoredMessage.StoredMessageSendState;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * <p>
 * This is a middleware service for LAS2peer that provides methods to send short messages via a LAS2peer network. It is
 * stateless, so there exist no session dependent values and it uses the LAS2peer shared storage for persistence. This
 * makes it possible to run and use the service either at each node that joins a LAS2peer network or to just call the
 * service from a LAS2peer instance that joined a network that contains at least one node hosting this service.<br>
 * 
 * <h3>Usage Hints</h3>
 * 
 * If you are new to LAS2peer and only want to start an instance (or ring) hosting this service, you can make use of the
 * start-script from the scripts directory that come with this project.<br/>
 * 
 * Since there currently exists no user manager application, you will have to add each user as an XML-file to the
 * "startup" directory. This directory will be uploaded when you execute the start scripts. To produce agent XML-files,
 * you will have to make use of the LAS2peer ServiceAgentGenerator. At GitHub, there exists a start-script to use this
 * tool in the LAS2peer-Sample-Project of the RWTH-ACIS organization.
 * 
 * @author Thomas Cujé
 * 
 */
public class ShortMessageService extends Service {

    /**
     * service properties with default values, can be overwritten by properties file from config
     */
    private long sendTimeout = 2000;
    private long maxMessageLength = 140;
    private String storageIdentifier = "network-storage";
    private String storageFile = "shortMessage-storage.xml";
    private long redeliverDelay = 10000;

    private ShortMessageStorage storage;
    private Timer redeliver;

    /**
     * Constructor: Loads the property file.
     */
    public ShortMessageService() {
        setFieldValues();
        // TODO load the message storage on startup - but how?
//        try {
//            storage = new ShortMessageStorage(getContext(), getAgent(), storageIdentifier, storageFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//            logMessage("Can't initialize persistent storage " + e);
//        }
        // TODO init redelivery on startup - but how?
//        Timer redeliver = new Timer();
//        redeliver.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                Iterable<StoredMessage> messages = storage.getAllTimedoutMessages();
//                for (StoredMessage msg : messages) {
//                    new ShortMessageDeliverer(getAgent(), getContext(), storage, getActiveNode(), msg, sendTimeout);
//                }
//            }
//        }, redeliverDelay, redeliverDelay);
    }

    /**
     * Sends a {@link i5.las2peer.services.shortMessageService.ShortMessage} to an agent.
     * 
     * @param message
     *            a simple text message
     * @param receivingAgent
     *            the agent representing the recipient
     * @return success or error message
     */
    public String sendMessage(UserAgent receivingAgent, String message) {
        // validate message
        if (message == null || message.isEmpty()) {
            return "Message can not be empty!";
        }
        if (message.length() > maxMessageLength) {
            return "Message too long! (Maximum: " + maxMessageLength + ")";
        }
        // create las2peer message
        UserAgent sendingAgent = (UserAgent) getActiveAgent();
        ShortMessage msg = new ShortMessage(sendingAgent.getId(), receivingAgent.getId(), message);
        msg.setSendTimestamp(new GregorianCalendar());
        Message toSend;
        try {
            toSend = new Message(sendingAgent, receivingAgent, msg);
        } catch (Exception e) {
            logMessage("Failure sending message " + e);
            return "Message can't be send";
        }
        // init service
        initStorage();
        initRedelivery();
        // persist message
        StoredMessage stored = new StoredMessage(toSend, StoredMessageSendState.NEW);
        storage.addMessage(stored);
        try {
            // start delivery thread
            ShortMessageDeliverer deliverer = new ShortMessageDeliverer(getAgent(), getContext(), storage,
                    getActiveNode(), stored, sendTimeout);
            deliverer.start();
            return "Message scheduled for delivery";
        } catch (AgentNotKnownException e) {
            return "Could not start message delivery " + e;
        }
    }

    /**
     * Sends a {@link i5.las2peer.services.shortMessageService.ShortMessage} to an recipient specified by login or
     * email. Has a build in wait mechanism to prevent floating the network with new messages.
     * 
     * @param message
     *            a simple text message
     * @param recipient
     *            the login name or email address representing the recipient
     * @return success or error message
     */
    public String sendMessage(String recipient, String message) {
        if (recipient == null || recipient.isEmpty()) {
            return "No recipient specified!";
        }
        long receiverId;
        try {
            receiverId = getActiveNode().getAgentIdForEmail(recipient);
        } catch (AgentNotKnownException e) {
            try {
                receiverId = getActiveNode().getAgentIdForLogin(recipient);
            } catch (AgentNotKnownException e2) {
                return "There exists no agent for '" + recipient + "'! Email: " + e.getMessage() + " Login: "
                        + e2.getMessage();
            }
        }
        try {
            UserAgent receivingAgent = (UserAgent) getActiveNode().getAgent(receiverId);
            return sendMessage(receivingAgent, message);
        } catch (AgentNotKnownException e) {
            e.printStackTrace();
            return "There exists no agent with id '" + receiverId + "'!";
        }
    }

    /**
     * Uses the active agent to get all new {@link i5.las2peer.services.shortMessageService.ShortMessage}'s.
     * 
     * @return array with all new messages
     */
    public ShortMessage[] getNewMessages() {
        // init service
        initStorage();
        initRedelivery();
        UserAgent requestingAgent = (UserAgent) getActiveAgent();
        // retrieve incoming messages from node and persist them
        try {
            Mediator mediator = getActiveNode().getOrRegisterLocalMediator(requestingAgent);
            if (mediator.hasMessages()) {
                Message get = null;
                while ((get = mediator.getNextMessage()) != null) {
                    storage.addMessage(new StoredMessage(get, StoredMessageSendState.DELIVERED));
                }
            }
        } catch (L2pSecurityException | AgentException e) {
            e.printStackTrace();
            logMessage("Error receiving message! Exception: " + e.toString());
            return null;
        }
        // retrieve all new messages from storage
        List<Message> messages = storage.getUnreadMessages(requestingAgent);
        // decrypt messages for retrieving agent
        List<ShortMessage> returnMessages = new ArrayList<>();
        for (Message msg : messages) {
            try {
                msg.open(getActiveNode());
                ShortMessage message = (ShortMessage) msg.getContent();
                returnMessages.add(message);
            } catch (AgentNotKnownException | L2pSecurityException e) {
                logMessage("Can't open message " + e);
            }
        }
        if (!returnMessages.isEmpty()) {
            return returnMessages.toArray(new ShortMessage[0]);
        } else {
            return null;
        }
    }

    /**
     * used in the JS frontend to show new messages as strings
     * 
     * @return An array with new messages formated as strings
     */
    public String[] getNewMessagesAsString() {
        ShortMessage[] messages = getNewMessages();
        if (messages == null) {
            return new String[] { "No new messages" };
        } else {
            List<String> msgList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat();
            for (ShortMessage sms : messages) {
                StringBuilder sb = new StringBuilder();
                sb.append(sdf.format(sms.getCreateTimestamp().getTime()) + "--->"
                        + sdf.format(sms.getSendTimestamp().getTime()) + " from " + sms.getSenderId() + " to "
                        + sms.getReceiverId() + " : " + sms.getMessage());
                msgList.add(sb.toString());
            }
            String[] txtMessages = msgList.toArray(new String[0]);
            return txtMessages;
        }
    }

    /**
     * used in the JS Class to show all methods of this class
     * 
     * @return A list with all method names of this class
     */
    public String[] getMethods() {
        List<String> allMethods = new ArrayList<String>();
        for (Method m : ShortMessageService.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
                allMethods.add(m.getName());
            }
        }
        return allMethods.toArray(new String[allMethods.size()]);
    }

    /**
     * used in the JS Class to show the parameters of the given method
     * 
     * @param methodIndex
     *            the method index as in the method name array
     * @return A list with all parameters of the given input method
     */
    public String[] getParameterTypesOfMethod(int methodIndex) {
        for (Method m : ShortMessageService.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers()) && methodIndex == 0) {
                Class<?>[] parameterTypesClasses = m.getParameterTypes();
                String[] parameterTypes = new String[parameterTypesClasses.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    parameterTypes[i] = parameterTypesClasses[i].getSimpleName();
                }

                if (parameterTypesClasses.length != 0) {
                    return parameterTypes;
                }

                return null;
            } else {
                methodIndex--;
            }
        }
        return new String[] { "No such method declared in the service " + ShortMessageService.class.getName() + "." };
    }

    public void initStorage() {
        if (storage == null) {
            // load the persistent message storage
            try {
                storage = new ShortMessageStorage(getContext(), getAgent(), storageIdentifier, storageFile);
            } catch (Exception e) {
                logError("Can't initialize persistent storage " + e);
            }
        }
    }

    public void initRedelivery() {
        if (redeliver == null) {
            try {
                final ServiceAgent agent = getAgent();
                final Context context = getContext();
                final Node node = getActiveNode();
                redeliver = new Timer();
                redeliver.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Iterable<StoredMessage> messages = storage.getAllTimedoutMessages();
                        for (StoredMessage msg : messages) {
                            ShortMessageDeliverer t = new ShortMessageDeliverer(agent, context, storage, node, msg,
                                    sendTimeout);
                            t.start();
                            // XXX logging
                            System.out.println("redelivery attempt started");
//                            context.logMessage(this, "redelivery attempt started");
                        }
                    }
                }, redeliverDelay, redeliverDelay);
                logMessage("Redelivery thread initialized");
            } catch (AgentNotKnownException e) {
                logError("Can't schedule messages for redelivery " + e);
            }
        }
    }

}
