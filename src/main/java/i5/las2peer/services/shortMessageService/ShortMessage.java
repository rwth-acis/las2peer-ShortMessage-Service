package i5.las2peer.services.shortMessageService;

import java.io.Serializable;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;

import net.minidev.json.JSONObject;

public class ShortMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String senderId;
	private final String recipientId;
	private final long index;
	private final String message;
	private final long timeSend;

	public ShortMessage(String senderId, String recipientId, long index, String message) {
		this.senderId = senderId;
		this.recipientId = recipientId;
		this.index = index;
		this.message = message;
		this.timeSend = new GregorianCalendar().getTimeInMillis();
	}

	public String getSenderId() {
		return senderId;
	}

	public String getRecipientId() {
		return recipientId;
	}

	public long getIndex() {
		return index;
	}

	public String getMessage() {
		return message;
	}

	public long getTimeSend() {
		return timeSend;
	}

	public HashMap<String, Serializable> toMap() {
		HashMap<String, Serializable> result = new HashMap<>();
		result.put("senderId", senderId);
		result.put("recipientId", recipientId);
		result.put("index", index);
		result.put("message", message);
		result.put("timeSend", timeSend);
		return result;
	}

	public JSONObject toJsonObject() {
		JSONObject result = new JSONObject();
		result.putAll(toMap());
		return result;
	}

	public static class ShortMessageTimeComparator implements Comparator<ShortMessage> {

		public static final ShortMessageTimeComparator INSTANCE = new ShortMessageTimeComparator();

		@Override
		public int compare(ShortMessage o1, ShortMessage o2) {
			if (o1 == o2) {
				return 0;
			} else if (o1 == null && o2 != null) {
				return -1;
			} else if (o1 != null && o2 == null) {
				return 1;
			} else if (o1.timeSend < o2.timeSend) {
				return -1;
			} else if (o1.timeSend > o2.timeSend) {
				return 1;
			}
			return 0;
		}

	}

}
