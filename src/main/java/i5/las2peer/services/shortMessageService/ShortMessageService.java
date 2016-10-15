package i5.las2peer.services.shortMessageService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.Context;
import i5.las2peer.api.exceptions.ArtifactNotFoundException;
import i5.las2peer.api.exceptions.StorageException;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.AgentNotKnownException;
import i5.las2peer.persistency.Envelope;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;
import i5.las2peer.security.Agent;
import i5.las2peer.security.L2pSecurityException;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.shortMessageService.ShortMessage.ShortMessageTimeComparator;
import i5.las2peer.tools.CryptoException;
import i5.las2peer.tools.SerializationException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * This is a middleware service for las2peer that provides methods to send short messages via a las2peer network. It is
 * stateless, so there exist no session dependent values and it uses the las2peer shared storage for persistence. This
 * makes it possible to run and use the service either at each node that joins a las2peer network or to just call the
 * service from a las2peer instance that joined a network that contains at least one node hosting this service.<br>
 * 
 * <h3>Usage Hints</h3>
 * 
 * If you are new to las2peer and only want to start an instance (or ring) hosting this service, you can make use of the
 * start-script from the bin/ directory that comes with this project.
 * <p>
 * Since there currently exists no user manager application, you will have to add each user as an XML-file to the
 * "config/startup" directory. This directory will be uploaded when you execute the start scripts. To produce agent
 * XML-files, you will have to make use of the las2peer ServiceAgentGenerator. At GitHub, there exists a script to use
 * this tool in the las2peer-Template-Project of the RWTH-ACIS group.
 * 
 */
@ServicePath("/sms-service")
public class ShortMessageService extends RESTService {

	private static final L2pLogger logger = L2pLogger.getInstance(ShortMessageService.class.getName());

	private static final long MAXIMUM_MESSAGE_LENGTH = 140;
	private static final String MESSAGE_IDENTIFIER_PREFIX = "shortmessage";

	private static final String RESOURCE_MESSAGES_BASENAME = "/messages";
	private static final String RESOURCE_PROPERTIES_BASENAME = "/properties";

	/**
	 * Constructor: Loads the properties file and sets the values.
	 */
	public ShortMessageService() {
		// enable service monitoring
		this.monitor = true;
	}

	/**
	 * Sends a {@link ShortMessage} to a recipient specified by login or email. This method is intended to be used with
	 * RMI calls.
	 * 
	 * WARNING: THIS METHOD IS UNSAFE, SINCE THE AGENT ID MAY BE LINKED TO ANYONE!
	 * 
	 * @param recipient The login name or email address representing the recipient.
	 * @param message The actual message text as {@link String}.
	 * @throws AgentNotKnownException If the given recipient can not be identified.
	 * @throws StorageException If an issue with the storage occurs.
	 * @throws CryptoException If an cryptographic issue occurs.
	 * @throws L2pSecurityException If a security issue occurs.
	 * @throws SerializationException If a serialization issue occurs.
	 */
	public void sendShortMessage(String recipient, String message) throws IllegalArgumentException,
			AgentNotKnownException, StorageException, CryptoException, L2pSecurityException, SerializationException {
		if (recipient == null || recipient.isEmpty()) {
			throw new IllegalArgumentException("No recipient specified!");
		}
		Agent receiver = null;
		try {
			receiver = getContext().getAgent(Long.parseLong(recipient));
		} catch (AgentNotKnownException | NumberFormatException e) {
			try {
				long receiverId = getContext().getLocalNode().getAgentIdForEmail(recipient);
				receiver = getContext().getAgent(receiverId);
			} catch (AgentNotKnownException | L2pSecurityException e2) {
				try {
					long receiverId = getContext().getLocalNode().getAgentIdForLogin(recipient);
					receiver = getContext().getAgent(receiverId);
				} catch (AgentNotKnownException | L2pSecurityException e3) {
					throw new AgentNotKnownException("There exists no agent for '" + recipient + "'! Email: "
							+ e.getMessage() + " Login: " + e2.getMessage());
				}
			}
		}
		sendShortMessage(receiver, message);
	}

	/**
	 * Sends a {@link ShortMessage} to a recipient specified by his agent id.
	 * 
	 * @param receivingAgent The recipients Agent with read permission.
	 * @param message The actual message text as {@link String}.
	 * @throws StorageException If an issue with the storage occurs.
	 * @throws CryptoException If an cryptographic issue occurs.
	 * @throws L2pSecurityException If a security issue occurs.
	 * @throws SerializationException If a serialization issue occurs.
	 */
	public void sendShortMessage(Agent receivingAgent, String message)
			throws StorageException, CryptoException, L2pSecurityException, SerializationException {
		// validate receiving agent
		if (receivingAgent == null) {
			throw new IllegalArgumentException("Receiving agent must not be null!");
		}
		// validate message
		if (message == null || message.isEmpty()) {
			throw new IllegalArgumentException("Message must not be empty!");
		}
		if (message.length() > MAXIMUM_MESSAGE_LENGTH) {
			throw new IllegalArgumentException("Message too long! (Maximum: " + MAXIMUM_MESSAGE_LENGTH + ")");
		}
		UserAgent sendingAgent = (UserAgent) getContext().getMainAgent();
		// persist message to shared storage
		// TODO cache last index in ServiceAgent protected storage
		long nextIndex = findLastMessageIndex(Long.toString(sendingAgent.getId()),
				Long.toString(receivingAgent.getId()), 0) + 1;
		String msgId = getMessageIdentifier(Long.toString(sendingAgent.getId()), Long.toString(receivingAgent.getId()),
				nextIndex);
		ShortMessage msg = new ShortMessage(Long.toString(sendingAgent.getId()), Long.toString(receivingAgent.getId()),
				nextIndex, message);
		Envelope env = getContext().createEnvelope(msgId, msg, sendingAgent, receivingAgent);
		getContext().storeEnvelope(env);
	}

	private long findLastMessageIndex(String sendingAgentId, String receivingAgentId, long startIndex) {
		if (startIndex < 0) {
			throw new IllegalArgumentException("startIndex must be non negative");
		}
		long c;
		// TODO implement this as binary search
		// after number overflow 'c' will be smaller than zero
		for (c = startIndex; c >= 0 && c < Long.MAX_VALUE; c++) {
			try {
				getContext().fetchEnvelope(getMessageIdentifier(sendingAgentId, receivingAgentId, c));
			} catch (ArtifactNotFoundException e) {
				return c - 1;
			} catch (StorageException e) {
				// XXX do we have to care about this? maybe just log it
			}
		}
		return c;
	}

	private String getMessageIdentifier(String sendingAgentId, String receivingAgentId, long index) {
		if (index < 0) {
			throw new IllegalArgumentException("index must be non negative");
		}
		String firstId = sendingAgentId;
		String secondId = receivingAgentId;
		if (sendingAgentId.compareTo(receivingAgentId) > 0) {
			firstId = receivingAgentId;
			secondId = sendingAgentId;
		}
		return MESSAGE_IDENTIFIER_PREFIX + firstId + "->" + secondId + "#" + index;
	}

	public ArrayList<HashMap<String, Serializable>> getShortMessages(String contactId, long startIndex, long limit) {
		ArrayList<HashMap<String, Serializable>> result = new ArrayList<>();
		ArrayList<ShortMessage> messages = getShortMessagesReal(contactId, startIndex, limit);
		for (ShortMessage msg : messages) {
			result.add(msg.toMap());
		}
		return result;
	}

	private ArrayList<ShortMessage> getShortMessagesReal(String contactId, long startIndex, long limit) {
		if (startIndex < 0) {
			throw new IllegalArgumentException("Bad parameter: startIndex must be non negative");
		}
		ShortMessageService service = (ShortMessageService) Context.getCurrent().getService();
		Agent activeAgent = service.getContext().getMainAgent();
		ArrayList<ShortMessage> fetchedMessages = new ArrayList<>();
		if (limit < 0) { // last x messages requested
			// fetch last index for messages received from the given contact
			startIndex = service.findLastMessageIndex(contactId, Long.toString(activeAgent.getId()), startIndex);
			if (startIndex < 0) { // no message at all
				startIndex = 0;
			}
		}
		// fetch messages, till we reach the desired limit
		while (fetchedMessages.size() < Math.abs(limit) || limit == 0) {
			long nextIndex = startIndex;
			if (limit < 0) {
				nextIndex -= fetchedMessages.size();
			} else {
				nextIndex += fetchedMessages.size();
			}
			if (nextIndex < 0) {
				// no more messages, OK
				break;
			}
			try {
				// fetch message with given index
				String msgId = service.getMessageIdentifier(contactId, Long.toString(activeAgent.getId()), nextIndex);
				Envelope env = service.getContext().fetchEnvelope(msgId);
				ShortMessage stored = (ShortMessage) env.getContent(activeAgent);
				fetchedMessages.add(stored);
			} catch (ArtifactNotFoundException e) {
				// no more messages, OK
				break;
			} catch (StorageException | SerializationException | L2pSecurityException | CryptoException e) {
				fetchedMessages.add(new ShortMessage(null, null, nextIndex, e.toString()));
			}
		}
		// sort all message by timestamp
		fetchedMessages.sort(ShortMessageTimeComparator.INSTANCE);
		return fetchedMessages;
	}

	@Override
	protected void initResources() {
		getResourceConfig().register(ResourceMessages.class);
		getResourceConfig().register(ResourceProperties.class);
	}

	@Path(RESOURCE_MESSAGES_BASENAME)
	public static class ResourceMessages {

		@POST
		@Path("/{contactId}")
		@Produces(MediaType.TEXT_PLAIN)
		public Response sendShortMessageWeb(@PathParam("contactId") String contactId, String message) {
			try {
				if (contactId == null || contactId.isEmpty()) {
					return Response.status(Status.BAD_REQUEST).entity("Missing parameter recipientId").build();
				}
				ShortMessageService service = (ShortMessageService) Context.getCurrent().getService();
				Agent recipient = service.getContext().getAgent(Long.valueOf(contactId));
				service.sendShortMessage(recipient, message);
				return Response.ok("MESSAGE_SEND_SUCCESSFULLY").build();
			} catch (Exception e) {
				String msg = "Could not send message!";
				logger.log(Level.SEVERE, msg, e);
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(msg + " See log for details.\nReason: " + e.getMessage()).build();
			}
		}

		/**
		 * Limit &lt; 0 means load last x messages Limit &gt; 0 load x messages Limit = 0 load infinite messages
		 * 
		 * @param contactId The conversation partners agent id.
		 * @param startIndex The first message to be retrieved.
		 * @param limit The maximum number of messages that should be retrieved. If negative, the latest number of
		 *            messages in this conversation is retrieved. e. g. limit = 20 returns the first 20 messages
		 *            starting from startIndex and limit = -7 returns the last 7 messages of the conversation.
		 * @return Returns the messages formatted as JSON String, wrapped in an HTML response object.
		 */
		@GET
		@Path("/{contactId}")
		@Produces(MediaType.APPLICATION_JSON)
		public Response getShortMessagesWeb(@PathParam("contactId") String contactId,
				@HeaderParam("startIndex") @DefaultValue("0") long startIndex,
				@HeaderParam("limit") @DefaultValue("-3") long limit) {
			try {
				ShortMessageService service = (ShortMessageService) Context.getCurrent().getService();
				ArrayList<ShortMessage> fetchedMessages = service.getShortMessagesReal(contactId, startIndex, limit);
				Agent activeAgent = service.getContext().getMainAgent();
				// transform messages into JSON
				JSONArray jsonMessages = new JSONArray();
				for (ShortMessage msg : fetchedMessages) {
					JSONObject jsonMsg = msg.toJsonObject();
					boolean isAuthor = false;
					if (msg.getSenderId().equals(Long.toString(activeAgent.getId()))) {
						isAuthor = true;
					}
					jsonMsg.put("isAuthor", isAuthor);
					jsonMessages.add(jsonMsg);
				}
				ResponseBuilder responseBuilder = Response.ok(jsonMessages.toJSONString());
				responseBuilder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				return responseBuilder.build();
			} catch (Exception e) {
				String msg = "Could not read messages!";
				logger.log(Level.SEVERE, msg, e);
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(msg + " See log for details.").build();
			}
		}

	}

	@Path(RESOURCE_PROPERTIES_BASENAME)
	public static class ResourceProperties {

		@GET
		@Path("/maximumMessageLength")
		@Produces(MediaType.TEXT_PLAIN)
		public Response getMaximumMessageLength() {
			return Response.ok(Long.toString(MAXIMUM_MESSAGE_LENGTH)).build();
		}

	}

}
