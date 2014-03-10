package i5.las2peer.services.shortMessageService;

import i5.las2peer.api.Service;
import i5.las2peer.communication.Message;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.p2p.MessageResultListener;
import i5.las2peer.security.AgentException;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.UserAgent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * 
 * <p>
 * This is a middleware service for LAS2peer that provides methods to send short
 * messages via a LAS2peer network. It is stateless, so there exist no session
 * dependent values and it uses the LAS2peer shared storage for persistence.
 * This makes it possible to run and use the service either at each node that
 * joins a LAS2peer network or to just call the service from a LAS2peer instance
 * that joined a network that contains at least one node hosting this service.<br>
 * 
 * <h3>Usage Hints</h3>
 * 
 * If you are new to LAS2peer and only want to start an instance (or ring)
 * hosting this service, you can make use of the start-script from the scripts
 * directory that come with this project.<br/>
 * 
 * Since there currently exists no user manager application, you will have to
 * add each user as an XML-file to the "startup" directory. This directory will
 * be uploaded when you execute the start scripts. To produce agent XML-files,
 * you will have to make use of the LAS2peer ServiceAgentGenerator. At GitHub,
 * there exists a start-script to use this tool in the LAS2peer-Sample-Project
 * of the RWTH-ACIS organization.
 * 
 * @author Thomas Cujé
 * 
 */
// FIXME use local and envelope persistent buffers to store messages
public class ShortMessageService extends Service {

    /**
     * service properties with default values, can be overwritten by properties
     * file from config
     */
    private long sendTimeout = 2000;
    private long maxMessageLength = 140;

    private MessageResultListener messageResultListener;

    /**
     * Constructor: Loads the property file.
     */
    public ShortMessageService() {
	setFieldValues();
	// TODO load the message buffer and start the delivery thread
    }

    /**
     * Sends a {@link i5.las2peer.services.shortMessageService.ShortMessage} to
     * an agent. Has a build in wait mechanism to prevent floating the network
     * with new messages.
     * 
     * @param message
     *            a simple text message
     * @param receivingAgent
     *            the agent representing the recipient
     * @return success or error message
     */
    public String sendMessage(String message, UserAgent receivingAgent) {
	// validate message
	if (message == null || message.isEmpty()) {
	    return "Message can not be empty!";
	}
	if (message.length() > sendTimeout) {
	    return "Message too long! (Maximum: " + maxMessageLength + ")";
	}
	// TODO add message to buffer
	UserAgent sendingAgent = (UserAgent) this.getActiveAgent();
	ShortMessage msg = new ShortMessage(sendingAgent.getId(),
		receivingAgent.getId(), message);
	try {
	    // FIXME handle message send timeout
	    if (messageResultListener == null
		    || messageResultListener.isFinished()) {
		msg.setSendTimestamp(new GregorianCalendar());
		Message toSend = new Message(sendingAgent, receivingAgent, msg);
		messageResultListener = new MessageResultListener(sendTimeout);
		getActiveNode().sendMessage(toSend, messageResultListener);
		return "Message sent";
	    } else {
		return "Please wait. This node is busy";
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    return "Error while sending message! Exception: " + e.toString();
	}
    }

    /**
     * Sends a {@link i5.las2peer.services.shortMessageService.ShortMessage} to
     * an recipient specified by login or email. Has a build in wait mechanism
     * to prevent floating the network with new messages.
     * 
     * @param message
     *            a simple text message
     * @param recipient
     *            the login name or email address representing the recipient
     * @return success or error message
     */
    public String sendMessage(String message, String recipient) {
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
		return "There exists no agent for '" + recipient + "'! Email: "
			+ e.getMessage() + " Login: " + e2.getMessage();
	    }
	}
	try {
	    UserAgent receivingAgent = (UserAgent) getActiveNode().getAgent(
		    receiverId);
	    return sendMessage(message, receivingAgent);
	} catch (AgentNotKnownException e) {
	    e.printStackTrace();
	    return "There exists no agent with id '" + receiverId + "'!";
	}
    }

    /**
     * Uses the active agent to get all new
     * {@link i5.las2peer.services.shortMessageService.ShortMessage}'s.
     * 
     * @return array with all new messages
     */
    public ShortMessage[] getNewMessages() {
	UserAgent requestingAgent = (UserAgent) getActiveAgent();
	try {
	    Mediator mediator = getActiveNode().getOrRegisterLocalMediator(
		    requestingAgent);
	    if (mediator.hasMessages()) {
		List<ShortMessage> returnMessages = new ArrayList<>();
		Message get = null;
		while ((get = mediator.getNextMessage()) != null) {
		    get.open(getActiveNode());
		    ShortMessage message = (ShortMessage) get.getContent();
		    returnMessages.add(message);
		}
		if (!returnMessages.isEmpty()) {
		    return returnMessages.toArray(new ShortMessage[0]);
		} else {
		    return null;
		}
	    } else {
		return null;
	    }
	} catch (L2pSecurityException | AgentException e) {
	    e.printStackTrace();
	    logMessage("Error receiving message! Exception: " + e.toString());
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
		sb.append(sdf.format(sms.getCreateTimestamp().getTime())
			+ "--->" + sdf.format(sms.getSendTimestamp().getTime())
			+ " from " + sms.getSenderId() + " to "
			+ sms.getReceiverId() + " : " + sms.getMessage());
		msgList.add(sb.toString());
	    }
	    String[] txtMessages = msgList.toArray(new String[0]);
	    return txtMessages;
	}
    }

    /**
     * used in the JS Class to show the parameters of the given method
     * 
     * @param methodName
     *            the method name
     * @return A list with all parameters of the given input method
     */
    public String[] getMethods() {
	List<String> allMethods = new ArrayList<String>();
	for (Method m : ShortMessageService.class.getDeclaredMethods()) {
	    if (Modifier.isPublic(m.getModifiers())
		    && !Modifier.isStatic(m.getModifiers())) {
		allMethods.add(m.getName());
	    }
	}
	return allMethods.toArray(new String[allMethods.size()]);
    }

    /**
     * used in the JS Class to show the parameters of the given method
     * 
     * @param methodName
     *            the method name
     * @return A list with all parameters of the given input method
     */
    // public String[] getParameterTypesOfMethod(String methodName) {
    // for (Method m : ShortMessageService.class.getDeclaredMethods()) {
    // if (Modifier.isPublic(m.getModifiers()) &&
    // !Modifier.isStatic(m.getModifiers())
    // && m.getName().equals(methodName)) {
    // Class[] parameterTypesClasses = m.getParameterTypes();
    // String[] parameterTypes = new String[parameterTypesClasses.length];
    // for (int i = 0; i < parameterTypes.length; i++) {
    // parameterTypes[i] = parameterTypesClasses[i].getSimpleName();
    // }
    //
    // if (parameterTypesClasses.length != 0) {
    // return parameterTypes;
    // }
    //
    // return null;
    // }
    // }
    // return new String[] { "No such method declared in the service " +
    // ShortMessageService.class.getName() + "." };
    // }

    public String[] getParameterTypesOfMethod(int methodIndex) {
	for (Method m : ShortMessageService.class.getDeclaredMethods()) {
	    if (Modifier.isPublic(m.getModifiers())
		    && !Modifier.isStatic(m.getModifiers()) && methodIndex == 0) {
		Class<?>[] parameterTypesClasses = m.getParameterTypes();
		String[] parameterTypes = new String[parameterTypesClasses.length];
		for (int i = 0; i < parameterTypes.length; i++) {
		    parameterTypes[i] = parameterTypesClasses[i]
			    .getSimpleName();
		}

		if (parameterTypesClasses.length != 0) {
		    return parameterTypes;
		}

		return null;
	    } else {
		methodIndex--;
	    }
	}
	return new String[] { "No such method declared in the service "
		+ ShortMessageService.class.getName() + "." };
    }
    
    // TODO add status function with node infos about how many messages sent, received and in queue or failed

}
