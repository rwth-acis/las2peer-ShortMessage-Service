package i5.las2peer;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;

import org.junit.Test;

import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.p2p.ServiceNameVersion;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.shortMessageService.ShortMessage;
import i5.las2peer.services.shortMessageService.ShortMessageService;
import i5.las2peer.testing.TestSuite;

/**
 * This testcase checks if the service can be used inside a very small network consisting of 3 nodes. The service is
 * deployed at the first node. Then user a connects to the second node and sends a meessage to user b, which is not
 * connected yet. Sending messages to non connected users is tested this way. In the next step user b connects to the
 * third node and tries to read the message earlier send from user a and stored in the network. While user b is now
 * online user a sends another message and user b tries to receive it again. Last but not least user a disconnects from
 * the network and user b checks if his messages are still available.
 *
 */
public class TripleNodeRMITest {

	@Test
	public void sendMessageAcrossNodes() throws Exception {
		System.out.println("starting network...");
		ArrayList<PastryNodeImpl> nodes = TestSuite.launchNetwork(3);

		// create agents
		System.out.println("creating user agents...");
		ServiceAgent service = ServiceAgent.createServiceAgent(
				new ServiceNameVersion(ShortMessageService.class.getName(), "1.0"), "test-service-pass");
		UserAgent userA = UserAgent.createUserAgent("test-pass-a");
		userA.unlockPrivateKey("test-pass-a");
		nodes.get(0).storeAgent(userA);
		UserAgent userB = UserAgent.createUserAgent("test-pass-b");
		userB.unlockPrivateKey("test-pass-b");
		nodes.get(2).storeAgent(userB);

		// start service instance on node 0
		System.out.println("starting service on node 0");
		service.unlockPrivateKey("test-service-pass");
		nodes.get(0).storeAgent(service);
		nodes.get(0).registerReceiver(service);

		// UserA login at node 1
		System.out.println("user a login at node 1");
		Mediator mediatorA = nodes.get(1).createMediatorForAgent(userA);

		// UserA send first message to UserB
		System.out.println("user a sending first message to user b");
		mediatorA.invoke(ShortMessageService.class.getName(), "sendShortMessage",
				new Serializable[] { userB.getId(), "First hello world to B from A" }, false);

		// UserB login at node 2
		System.out.println("user b login at node 2");
		Mediator mediatorB = nodes.get(2).createMediatorForAgent(userB);

		// verify UserB received first message
		ShortMessage[] messages1 = (ShortMessage[]) mediatorB.invoke(ShortMessageService.class.getName(),
				"getShortMessages", new Serializable[] {}, false);
		assertEquals(1, messages1.length);

		// UserA send second message to UserB
		mediatorA.invoke(ShortMessageService.class.getName(), "sendShortMessage",
				new Serializable[] { userB.getId(), "Second hello world to B from A" }, false);

		// verify UserB received two messages
		ShortMessage[] messages2 = (ShortMessage[]) mediatorB.invoke(ShortMessageService.class.getName(),
				"getShortMessages", new Serializable[] {}, false);
		assertEquals(2, messages2.length);

		for (PastryNodeImpl node : nodes) {
			node.shutDown();
		}
	}

}
