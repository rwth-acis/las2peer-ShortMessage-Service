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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import i5.las2peer.api.Context;
import i5.las2peer.api.persistency.Envelope;
import i5.las2peer.api.persistency.EnvelopeAccessDeniedException;
import i5.las2peer.api.persistency.EnvelopeException;
import i5.las2peer.api.persistency.EnvelopeNotFoundException;
import i5.las2peer.api.persistency.EnvelopeOperationFailedException;
import i5.las2peer.api.security.Agent;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AgentOperationFailedException;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.services.shortMessageService.ShortMessage.ShortMessageTimeComparator;
import io.swagger.annotations.Api;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
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
@Api()
@SwaggerDefinition(
		info = @Info(
				title = "las2peer ShortMessage Service",
				version = ShortMessageService.API_VERSION,
				description = "A las2peer messaging service for demonstration purposes.",
				contact = @Contact(
						name = "ACIS Group",
						url = "https://las2peer.org/",
						email = "cuje@dbis.rwth-aachen.de"),
				license = @License(
						name = "ACIS License (BSD3)",
						url = "https://github.com/rwth-acis/las2peer-ShortMessage-Service/blob/master/LICENSE")))
public class ShortMessageService extends RESTService {

	public static final String API_VERSION = "1.0";

	private static final L2pLogger logger = L2pLogger.getInstance(ShortMessageService.class.getName());

	private static final long MAXIMUM_MESSAGE_LENGTH = 140;
	private static final String MESSAGE_IDENTIFIER_PREFIX = "shortmessage";

	/**
	 * Constructor: Loads the properties file and sets the values.
	 */
	public ShortMessageService() {
	}

	/**
	 * Sends a {@link ShortMessage} to a recipient specified by login or email. This method is intended to be used with
	 * RMI calls.
	 * 
	 * WARNING: THIS METHOD IS UNSAFE, SINCE THE AGENT ID MAY BE LINKED TO ANYONE!
	 * 
	 * @param recipient The login name or email address representing the recipient.
	 * @param message The actual message text as {@link String}.
	 * @throws AgentNotFoundException If the given recipient can not be identified.
	 */
	public void sendShortMessage(String recipient, String message)
			throws IllegalArgumentException, AgentNotFoundException, AgentOperationFailedException, EnvelopeException {
		if (recipient == null || recipient.isEmpty()) {
			throw new IllegalArgumentException("No recipient specified!");
		}
		Agent receiver = null;
		try {
			receiver = Context.get().fetchAgent(recipient);
		} catch (AgentNotFoundException | NumberFormatException e) {
			try {
				String receiverId = Context.get().getUserAgentIdentifierByEmail(recipient);
				receiver = Context.get().fetchAgent(receiverId);
			} catch (AgentNotFoundException e2) {
				try {
					String receiverId = Context.get().getUserAgentIdentifierByLoginName(recipient);
					receiver = Context.get().fetchAgent(receiverId);
				} catch (AgentNotFoundException e3) {
					throw new AgentNotFoundException("There exists no agent for '" + recipient + "'! Email: "
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
	 */
	public void sendShortMessage(Agent receivingAgent, String message) throws EnvelopeException {
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
		UserAgent sendingAgent = (UserAgent) Context.get().getMainAgent();
		// persist message to shared storage
		// TODO cache last index in ServiceAgent protected storage
		long nextIndex = findLastMessageIndex(sendingAgent.getIdentifier(), receivingAgent.getIdentifier(), 0) + 1;
		String msgId = getMessageIdentifier(sendingAgent.getIdentifier(), receivingAgent.getIdentifier(), nextIndex);
		ShortMessage msg = new ShortMessage(sendingAgent.getIdentifier(), receivingAgent.getIdentifier(), nextIndex,
				message);
		Envelope env = Context.get().createEnvelope(msgId, sendingAgent);
		env.addReader(receivingAgent);
		env.setContent(msg);
		Context.get().storeEnvelope(env);
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
				Context.get().requestEnvelope(getMessageIdentifier(sendingAgentId, receivingAgentId, c));
			} catch (EnvelopeNotFoundException e) {
				return c - 1;
			} catch (EnvelopeException e) {
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
		Agent activeAgent = Context.get().getMainAgent();
		ArrayList<ShortMessage> fetchedMessages = new ArrayList<>();
		if (limit < 0) { // last x messages requested
			// fetch last index for messages received from the given contact
			startIndex = service.findLastMessageIndex(contactId, activeAgent.getIdentifier(), startIndex);
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
				String msgId = service.getMessageIdentifier(contactId, activeAgent.getIdentifier(), nextIndex);
				Envelope env = Context.get().requestEnvelope(msgId);
				ShortMessage stored = (ShortMessage) env.getContent();
				fetchedMessages.add(stored);
			} catch (EnvelopeNotFoundException e) {
				// no more messages, OK
				break;
			} catch (EnvelopeAccessDeniedException | EnvelopeOperationFailedException e) {
				fetchedMessages.add(new ShortMessage(null, null, nextIndex, e.toString()));
			}
		}
		// sort all message by timestamp
		fetchedMessages.sort(ShortMessageTimeComparator.INSTANCE);
		return fetchedMessages;
	}

	@POST
	@Path("/messages/{contactId}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response sendShortMessageWeb(@PathParam("contactId") String contactId, String message) {
		try {
			if (contactId == null || contactId.isEmpty()) {
				return Response.status(Status.BAD_REQUEST).entity("Missing parameter recipientId").build();
			}
			ShortMessageService service = (ShortMessageService) Context.getCurrent().getService();
			Agent recipient = Context.get().fetchAgent(contactId);
			service.sendShortMessage(recipient, message);
			return Response.ok("MESSAGE_SEND_SUCCESSFULLY").build();
		} catch (IllegalArgumentException e) {
			logger.log(Level.INFO, e.getMessage());
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
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
	 * @param limit The maximum number of messages that should be retrieved. If negative, the latest number of messages
	 *            in this conversation is retrieved. e. g. limit = 20 returns the first 20 messages starting from
	 *            startIndex and limit = -7 returns the last 7 messages of the conversation.
	 * @return Returns the messages formatted as JSON String, wrapped in an HTML response object.
	 */
	@GET
	@Path("/messages/{contactId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getShortMessagesWeb(@PathParam("contactId") String contactId,
			@HeaderParam("startIndex") @DefaultValue("0") long startIndex,
			@HeaderParam("limit") @DefaultValue("-3") long limit) {
		try {
			ShortMessageService service = (ShortMessageService) Context.getCurrent().getService();
			ArrayList<ShortMessage> fetchedMessages = service.getShortMessagesReal(contactId, startIndex, limit);
			Agent activeAgent = Context.get().getMainAgent();
			// transform messages into JSON
			JSONArray jsonMessages = new JSONArray();
			for (ShortMessage msg : fetchedMessages) {
				JSONObject jsonMsg = msg.toJsonObject();
				boolean isAuthor = false;
				if (msg.getSenderId().equals(activeAgent.getIdentifier())) {
					isAuthor = true;
				}
				jsonMsg.put("isAuthor", isAuthor);
				jsonMessages.add(jsonMsg);
			}
			return Response.ok(jsonMessages.toJSONString()).build();
		} catch (IllegalArgumentException e) {
			logger.log(Level.WARNING, e.getMessage());
			return buildJSONResponse(Status.BAD_REQUEST, e.getMessage());
		} catch (Exception e) {
			String msg = "Could not read messages!";
			logger.log(Level.SEVERE, msg, e);
			return buildJSONResponse(Status.INTERNAL_SERVER_ERROR, msg + " See log for details.");
		}
	}

	private static Response buildJSONResponse(Status status, String message) {
		JSONArray jsonMessage = new JSONArray();
		jsonMessage.add(message);
		return Response.status(status).entity(jsonMessage.toJSONString()).build();
	}

	@GET
	@Path("/properties/maximumMessageLength")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getMaximumMessageLength() {
		return Response.ok(Long.toString(MAXIMUM_MESSAGE_LENGTH)).build();
	}

}
