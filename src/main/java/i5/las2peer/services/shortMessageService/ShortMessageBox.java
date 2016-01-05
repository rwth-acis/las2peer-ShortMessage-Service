package i5.las2peer.services.shortMessageService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;

import i5.las2peer.logging.L2pLogger;
import i5.las2peer.persistency.MalformedXMLException;

/**
 * This class represents the message inbox for an user agent. Each time a message is send to an agent the message is
 * stored in the corresponding instance of this class. Additionally this class makes handling messages inside the
 * las2peer Envelope class a bit easier.
 * 
 */
public class ShortMessageBox implements Serializable {

	private static final L2pLogger logger = L2pLogger.getInstance(ShortMessageService.class.getName());

	private static final long serialVersionUID = -300617519857096303L;
	private final ArrayList<String> messages;

	/**
	 * Constructor with initial capacity parameter for the internal storage.
	 * 
	 * @param initialCapacity Used to initialize the internal storage.
	 */
	public ShortMessageBox(int initialCapacity) {
		messages = new ArrayList<>(initialCapacity);
	}

	/**
	 * Adds a message to this inbox.
	 * 
	 * @param msg Message that should be stored.
	 */
	public void addMessage(ShortMessage msg) {
		messages.add(msg.toXmlString());
	}

	/**
	 * Get all messages from this instance.
	 * 
	 * @return Returns an array containing all messages of this box.
	 */
	public ShortMessage[] getMessages() {
		ArrayList<ShortMessage> result = new ArrayList<>(messages.size());
		for (String xml : messages) {
			try {
				ShortMessage msg = ShortMessage.createFromXml(xml);
				result.add(msg);
			} catch (MalformedXMLException e) {
				logger.log(Level.SEVERE, "Can't parse Message from xml '" + xml + "'!", e);
			}
		}
		ShortMessage[] array = result.toArray(new ShortMessage[0]);
		return array;
	}

	/**
	 * Get the number of Messages stored inside.
	 * 
	 * @return Returns the number of Messages stored inside.
	 */
	public int size() {
		return messages.size();
	}

	/**
	 * Deletes all Messages and empties the internal storage.
	 */
	public void clear() {
		messages.clear();
	}

}
