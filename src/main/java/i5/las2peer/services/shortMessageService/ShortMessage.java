package i5.las2peer.services.shortMessageService;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.las2peer.security.Context;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;
import rice.p2p.util.Base64;

/**
 * Data class that is used by the {@link ShortMessageService} to transport messages.<br>
 * It contains the message itself as well as some meta-data that will be used to categorize this message.
 * 
 */
public class ShortMessage implements XmlAble, Serializable {

	private static final long serialVersionUID = 1L;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy;MM;dd;HH;mm;ss;Z");

	/** agent id of the agent who send the message **/
	private final long senderId;
	/** agent id of the agent this message should be delivered to **/
	private final long recipientId;
	/** actual message in base64 encoding **/
	private final byte[] content;
	/** timestamp when the message was handed to the network **/
	private Calendar timeSend;

	/**
	 * Constructor for a {@link ShortMessage}. Will be called by the {@link ShortMessageService} before a message is
	 * send. Since the message is base64 encoded even an image could be used as message.
	 * 
	 * @param agentIdFrom
	 *            user agent id who send the message
	 * @param agentIdto
	 *            user agent id who should receive the message
	 * @param message
	 *            the message itself as byte array
	 */
	public ShortMessage(long agentIdFrom, long agentIdTo, byte[] message) {
		senderId = agentIdFrom;
		recipientId = agentIdTo;
		content = message;
	}

	public ShortMessage(long agentIdFrom, long agentIdTo, String message) {
		this(agentIdFrom, agentIdTo, message.getBytes());
	}

	/**
	 * Gets the id of the sender user agent this {@link ShortMessage} was send from.
	 * 
	 * @return the user agent id
	 */
	public long getSenderId() {
		return senderId;
	}

	/**
	 * Gets the id of the user agent this {@link ShortMessage} should be delivered to.
	 * 
	 * @return the user agent id
	 */
	public long getRecipientId() {
		return recipientId;
	}

	/**
	 * Gets the content of this {@link ShortMessage}
	 * 
	 * @return A byte array containing the actual message.
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * Sets the timestamp this {@link ShortMessage} was send. This function should be called immediately before sending
	 * the message.
	 * 
	 * @param timestamp
	 *            A {@link java.util.Calendar} with the timestamp when this message was send.
	 */
	public void setSendTimestamp(Calendar timestamp) {
		timeSend = timestamp;
	}

	/**
	 * Gets the send timestamp for this {@link ShortMessage}
	 * 
	 * @return Returns a {@link java.util.Calendar} with the send timestamp.
	 */
	public Calendar getSendTimestamp() {
		return timeSend;
	}

	/**
	 * Transform this message object into a xml string. Which can be used to serialize and transfer the object. Use
	 * createFromXml() to get back the {@link ShortMessage} object.
	 * 
	 * @return Returns this object as xml String
	 */
	@Override
	public String toXmlString() {
		String strTime = sdf.format(timeSend.getTime());
		return "<las2peer:shortmessage from=\"" + senderId + "\" to=\"" + recipientId + "\"" + " send=\"" + strTime
				+ "\">" + Base64.encodeBytes(content) + "</las2peer:shortmessage>\n";
	}

	/**
	 * Creates a {@link ShortMessage} object from the given xml String.
	 * 
	 * @param xml
	 *            String that should be parsed
	 * @return Returns a {@link ShortMessage} object or null if an error occurs.
	 * @throws MalformedXMLException
	 *             if the xml String is not an {@link ShortMessage} object.
	 */
	public static ShortMessage createFromXml(String xml) throws MalformedXMLException {
		String attrSend = null;
		try {
			Element root = Parser.parse(xml, false);
			String rootName = root.getName();
			if (rootName.equals("shortmessage") == false) {
				throw new MalformedXMLException("shortmessage expected but " + rootName + " found");
			}
			long agentIdFrom = Long.parseLong(root.getAttribute("from"));
			long agentIdto = Long.parseLong(root.getAttribute("to"));
			Element child = root.getFirstChild();
			String text = child.getText();
			byte[] message = (text != null ? Base64.decode(text) : new byte[0]);
			ShortMessage msg = new ShortMessage(agentIdFrom, agentIdto, message);
			attrSend = root.getAttribute("send");
			Date d = sdf.parse(attrSend);
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(d);
			msg.setSendTimestamp(cal);
			return msg;
		} catch (XMLSyntaxException e) {
			Context.logError(ShortMessage.class, "XML syntax error in '" + xml + "' " + e);
		} catch (NumberFormatException e) {
			Context.logError(ShortMessage.class, "Can't parse timestamp '" + attrSend + "' " + e);
		} catch (ParseException e) {
			Context.logError(ShortMessage.class, "Parsing failed! " + e);
		}
		return null;
	}

}
