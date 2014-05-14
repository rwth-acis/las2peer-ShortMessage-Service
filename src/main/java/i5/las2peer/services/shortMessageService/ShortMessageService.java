package i5.las2peer.services.shortMessageService;

import i5.las2peer.api.Service;
import i5.las2peer.communication.Message;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.UserAgent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

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
 * start-script from the bin/ directory that comes with this project.<br/>
 * 
 * Since there currently exists no user manager application, you will have to add each user as an XML-file to the
 * "config/startup" directory. This directory will be uploaded when you execute the start scripts. To produce agent
 * XML-files, you will have to make use of the LAS2peer ServiceAgentGenerator. At GitHub, there exists a script to use
 * this tool in the LAS2peer-Template-Project of the RWTH-ACIS group.
 * 
 * @author Thomas Cujé
 * 
 */
public class ShortMessageService extends Service {

    /**
     * service properties with default values, can be overwritten with properties file
     * config/ShortMessageService.properties
     */
    private long maxMessageLength = 140;
    private String storageBaseName = "shortmessagestorage";

    /**
     * Constructor: Loads the properties file and sets the values.
     */
    public ShortMessageService() {
        setFieldValues();
    }

    /**
     * Sends a {@link ShortMessage} to an agent.
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
        // create short message
        UserAgent sendingAgent = (UserAgent) getActiveAgent();
        ShortMessage msg = new ShortMessage(sendingAgent.getId(), receivingAgent.getId(), message);
        msg.setSendTimestamp(new GregorianCalendar());
        // persist message to recipient storage
        try {
            Message lasMsg = new Message(sendingAgent, receivingAgent, msg);
            Envelope env = null;
            try {
                env = getContext().getStoredObject(ShortMessageBox.class, storageBaseName + receivingAgent.getId());
            } catch (Exception e) {
                Context.logMessage(this, "Network storage not found. Creating new one. " + e);
                env = Envelope.createClassIdEnvelope(new ShortMessageBox(1), storageBaseName + receivingAgent.getId(),
                        getAgent());
            }
            env.open(getAgent());
            // get messages from storage
            ShortMessageBox stored = env.getContent(ShortMessageBox.class);
            // add new message
            stored.addMessage(lasMsg);
            env.updateContent(stored);
            // close envelope
            env.addSignature(getAgent());
            env.store();
            env.close();
            Context.logMessage(this, "stored " + stored.size() + " messages in network storage");
            return "Message send successfully";
        } catch (Exception e) {
            Context.logError(this, "Can't persist short messages to network storage! " + e);
        }
        return "Failure sending message";
    }

    /**
     * Sends a {@link ShortMessage} to a recipient specified by login or email.
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
            return "There exists no agent with id '" + receiverId + "'!";
        }
    }

    /**
     * Uses the active agent to get all new {@link ShortMessage}'s.
     * 
     * @return array with all new messages
     */
    public ShortMessage[] getNewMessages() {
        try {
            // load messages from network
            Agent owner = getActiveAgent();
            Envelope env = getContext().getStoredObject(ShortMessageBox.class, storageBaseName + owner.getId());
            env.open(getAgent());
            ShortMessageBox stored = env.getContent(ShortMessageBox.class);
            // TODO clear network storage, message persistence should be done by clients
            env.close();
            Context.logMessage(this, "Loaded " + stored.size() + " messages from network storage");
            Message[] messages = stored.getMessages();
            ShortMessage[] result = new ShortMessage[stored.size()];
            for (int n = 0; n < messages.length; n++) {
                messages[n].open(owner, getActiveNode());
                result[n] = (ShortMessage) messages[n].getContent();
            }
            return result;
        } catch (Exception e) {
            Context.logError(this, "Can't read messages from network storage! " + e);
        }
        return new ShortMessage[0];
    }

    /**
     * used in the web frontend to show new messages as Strings
     * 
     * @return An array with new messages formated as Strings
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
                sb.append(sdf.format(sms.getSendTimestamp().getTime()) + " from " + sms.getSenderId() + " to "
                        + sms.getRecipientId() + " : " + new String(sms.getMessage()) + "<br/>\n");
                msgList.add(sb.toString());
            }
            String[] txtMessages = msgList.toArray(new String[0]);
            return txtMessages;
        }
    }

}
