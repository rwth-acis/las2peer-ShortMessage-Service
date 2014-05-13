package i5.las2peer.services.shortMessageService;

import i5.las2peer.persistency.MalformedXMLException;
import i5.las2peer.persistency.XmlAble;
import i5.simpleXML.Element;
import i5.simpleXML.Parser;
import i5.simpleXML.XMLSyntaxException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 
 * <p>
 * Data class that is used by the {@link i5.las2peer.services.shortMessageService.ShortMessageService} to transport
 * messages.<br>
 * It contains the message itself as well as some meta-data that will be used to categorize this message.
 * 
 * @author Thomas Cujé
 * 
 */
public class ShortMessage implements XmlAble {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy;MM;dd;HH;mm;ss;Z");

    private final long senderId;
    private final long recipientId;
    private final String content;
    private Calendar timeSend;

    /**
     * Constructor for a {@link i5.las2peer.services.shortMessageService.ShortMessage}. Will be called by the
     * {@link i5.las2peer.services.shortMessageService.ShortMessageService} before a message is sent.
     * 
     * @param agentIdFrom
     *            user agent id who sent the message
     * @param agentIdto
     *            user agent id who should receive the message
     * @param message
     *            the message string itself
     */
    public ShortMessage(long agentIdFrom, long agentIdto, String message) {
        senderId = agentIdFrom;
        recipientId = agentIdto;
        content = message;
    }

    /**
     * Gets the id of the sender user agent this {@link i5.las2peer.services.shortMessageService.ShortMessage} was sent
     * from.
     * 
     * @return the user agent id
     */
    public long getSenderId() {
        return senderId;
    }

    /**
     * Gets the id of the user agent this {@link i5.las2peer.services.shortMessageService.ShortMessage} should be
     * delivered to.
     * 
     * @return the user agent id
     */
    public long getRecipientId() {
        return recipientId;
    }

    /**
     * Gets the content of this {@link i5.las2peer.services.shortMessageService.ShortMessage}
     * 
     * @return A String containing the actual message as String.
     */
    public String getMessage() {
        return content;
    }

    /**
     * Sets the timestamp this {@link i5.las2peer.services.shortMessageService.ShortMessage} was sent. This function
     * should be called immediately before sending the message.
     * 
     * @param timestamp
     *            A {@link java.util.Calendar} with the timestamp when this message was send.
     */
    public void setSendTimestamp(Calendar timestamp) {
        timeSend = timestamp;
    }

    /**
     * Gets the sent timestamp for this {@link i5.las2peer.services.shortMessageService.ShortMessage}
     * 
     * @return Returns a {@link java.util.Calendar} with the sent timestamp.
     */
    public Calendar getSendTimestamp() {
        return timeSend;
    }

    @Override
    public String toXmlString() {
        String strTime = sdf.format(timeSend.getTime());
        return "<las2peer:shortmessage from=\"" + senderId + "\" to=\"" + recipientId + "\"" + " send=\"" + strTime
                + "\">" + content + "</las2peer:shortmessage>\n";
    }

    public static ShortMessage createFromXml(String xml) throws MalformedXMLException {
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
            String message = (text != null ? text : "");
            ShortMessage msg = new ShortMessage(agentIdFrom, agentIdto, message);
            String attrSend = root.getAttribute("send");
            Date d = sdf.parse(attrSend);
            Calendar cal = GregorianCalendar.getInstance();
            cal.setTime(d);
            msg.setSendTimestamp(cal);
            return msg;
        } catch (XMLSyntaxException e) {
            // XXX logging
            e.printStackTrace();
        } catch (NumberFormatException e) {
            // XXX logging
            e.printStackTrace();
        } catch (ParseException e) {
            // XXX logging
            e.printStackTrace();
        }
        return null;
    }

}
