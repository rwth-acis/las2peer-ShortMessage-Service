package i5.las2peer.services.shortMessageService;

import i5.las2peer.api.Service;
import i5.las2peer.communication.Message;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.restMapper.RESTMapper;
import i5.las2peer.restMapper.annotations.GET;
import i5.las2peer.restMapper.annotations.Path;
import i5.las2peer.restMapper.annotations.PathParam;
import i5.las2peer.security.Agent;
import i5.las2peer.security.Context;
import i5.las2peer.security.GroupAgent;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
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
 */
public class ShortMessageService extends Service {

	/**
	 * service properties with default values, can be overwritten with properties file
	 * config/ShortMessageService.properties
	 */
	private long DEFAULT_MAXIMUM_MESSAGE_LENGTH = 140;
	protected long maxMessageLength = DEFAULT_MAXIMUM_MESSAGE_LENGTH;

	private final String STORAGE_BASENAME = "shortmessagestorage";

	/**
	 * Constructor: Loads the properties file and sets the values.
	 */
	public ShortMessageService() {
		setFieldValues();
	}

	/**
	 * Sends a {@link ShortMessage} to a recipient specified by his agent.
	 * 
	 * @param receivingAgent
	 *            the agent representing the recipient
	 * @param message
	 *            the actual message text as {@link String}
	 * @return success or error message
	 */
	public String sendShortMessage(UserAgent receivingAgent, String message) {
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
				env = getContext().getStoredObject(ShortMessageBox.class, STORAGE_BASENAME + receivingAgent.getId());
			} catch (Exception e) {
				Context.logMessage(this, "Network storage not found. Creating new one. " + e);
				env = Envelope.createClassIdEnvelope(new ShortMessageBox(1), STORAGE_BASENAME + receivingAgent.getId(),
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
	 *            the actual message text as {@link String}
	 * @param recipient
	 *            the login name or email address representing the recipient
	 * @return success or error message
	 */
	@GET
	@Path("sendShortMessage/{recipient}/{message}")
	public String sendShortMessage(@PathParam("recipient") String recipient, @PathParam("message") String message) {
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
			return sendShortMessage(receivingAgent, message);
		} catch (AgentNotKnownException e) {
			return "There exists no agent with id '" + receiverId + "'!";
		}
	}

	/**
	 * Gets all {@link ShortMessage}'s for the active agent.
	 * 
	 * @return array with all messages
	 */
	public ShortMessage[] getShortMessages() {
		try {
			// load messages from network
			Agent owner = getActiveAgent();
			Envelope env = getContext().getStoredObject(ShortMessageBox.class, STORAGE_BASENAME + owner.getId());
			env.open(getAgent());
			ShortMessageBox stored = env.getContent(ShortMessageBox.class);
			Context.logMessage(this, "Loaded " + stored.size() + " messages from network storage");
			Message[] messages = stored.getMessages();
			ShortMessage[] result = new ShortMessage[stored.size()];
			for (int n = 0; n < messages.length; n++) {
				messages[n].open(owner, getActiveNode());
				result[n] = (ShortMessage) messages[n].getContent();
			}
			env.close();
			return result;
		} catch (Exception e) {
			Context.logError(this, "Can't read messages from network storage! " + e);
		}
		return new ShortMessage[0];
	}

	/**
	 * Gets messages separated by newline (\n) for the requesting agent. Sender and recipient agent ids are replaced
	 * with their names.
	 * 
	 * @return A String with messages or "No messages"
	 */
	@GET
	@Path("getShortMessagesAsString")
	public String getShortMessagesAsString() {
		ShortMessage[] messages = getShortMessages();
		if (messages == null || messages.length == 0) {
			return "No messages";
		} else {
			SimpleDateFormat sdf = new SimpleDateFormat();
			StringBuilder sb = new StringBuilder();
			for (ShortMessage sms : messages) {
				sb.append(sdf.format(sms.getSendTimestamp().getTime()) + " from " + getAgentName(sms.getSenderId())
						+ " to " + getAgentName(sms.getRecipientId()) + " : " + new String(sms.getContent()) + "\n");
			}
			return sb.toString();
		}
	}

	/**
	 * Gets the name for a specified agent id. For UserAgent's the login name, for ServiceAgent's the class name and for
	 * GroupAgent's the group name is retrieved.
	 * 
	 * @param agentId
	 *            The agent id that name should be retrieved
	 * @return Returns the agent name for the given agentId or the agentId as String if an error occurred.
	 */
	protected String getAgentName(long agentId) {
		String result = Long.toString(agentId);
		try {
			Agent agent = this.getActiveNode().getAgent(agentId);
			if (agent != null) {
				if (agent instanceof UserAgent) {
					result = ((UserAgent) agent).getLoginName();
				} else if (agent instanceof ServiceAgent) {
					result = ((ServiceAgent) agent).getServiceClassName();
				} else if (agent instanceof GroupAgent) {
					// TODO return group name
				}
			}
		} catch (Exception e) {
			Context.logMessage(this, "Could not resolve agent id " + agentId);
		}
		return result;
	}

	/**
	 * Used by the RESTMapper
	 * 
	 * @return
	 */
	public String getRESTMapping() {
		String result = "";
		try {
			result = RESTMapper.getMethodsAsXML(this.getClass());
		} catch (Exception e) {
			Context.logError(this, "Couldn't get REST mapping for this class " + e);
		}
		return result;
	}

}