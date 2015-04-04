package i5.las2peer;

import static org.junit.Assert.assertEquals;
import i5.las2peer.p2p.PastryNodeImpl;
import i5.las2peer.p2p.PastryNodeImpl.STORAGE_MODE;
import i5.las2peer.security.Mediator;
import i5.las2peer.security.ServiceAgent;
import i5.las2peer.security.UserAgent;
import i5.las2peer.services.shortMessageService.ShortMessage;
import i5.las2peer.services.shortMessageService.ShortMessageService;
import i5.las2peer.tools.ColoredOutput;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;

import rice.pastry.socket.SocketPastryNodeFactory;

/**
 * This testcase checks if the service can be used inside a very small network consisting of 3 nodes. The service is
 * deployed at the first node. Then user a connects to the second node and sends a meessage to user b, which is not
 * connected yet. Sending messages to non connected users is tested this way. In the next step user b connects to
 * the third node and tries to read the message earlier send from user a and stored in the network. While user b is
 * now online user a sends another message and user b tries to receive it again. Last but not least user a
 * disconnects from the network and user b checks if his messages are still available.
 *
 */
public class TripleNodeRMITest {

	List<PastryNodeImpl> nodes;
	PastryNodeImpl bootstrap;

	@Before
	public void init() {
		ColoredOutput.allOff();
	}

	private void startNetwork(int numOfNodes) throws Exception {
		nodes = new ArrayList<>(numOfNodes);
		bootstrap = new PastryNodeImpl(30000, null, STORAGE_MODE.memory, false, null, null);
		bootstrap.setLogfilePrefix("./log/l2p-node_");
		bootstrap.launch();
		// get the address the boostrap node listens to
		MultiInetSocketAddress addr = (MultiInetSocketAddress) bootstrap.getPastryNode().getVars()
				.get(SocketPastryNodeFactory.PROXY_ADDRESS);
		String strAddr = addr.getAddress(0).getHostString();
		nodes.add(bootstrap);
		for (int i = 1; i < numOfNodes; i++) {
			PastryNodeImpl n = new PastryNodeImpl(30000 + i, strAddr + ":30000", STORAGE_MODE.memory, false, null, null);
			n.setLogfilePrefix("./log/l2p-node_");
			n.launch();
			nodes.add(n);
		}
	}

	private void stopNetwork() {
		for (PastryNodeImpl node : nodes) {
			node.shutDown();
		}
	}

	@Test
	public void sendMessageAcrossNodes() throws Exception {
		System.out.println("starting network...");
		startNetwork(3);

		// create agents
		System.out.println("creating user agents...");
		ServiceAgent service = ServiceAgent.generateNewAgent(ShortMessageService.class.getName(), "test-service-pass");
		UserAgent userA = UserAgent.createUserAgent("test-pass-a");
		UserAgent userB = UserAgent.createUserAgent("test-pass-b");

		// start service instance on node 0
		System.out.println("starting service on node 0");
		service.unlockPrivateKey("test-service-pass");
		nodes.get(0).registerReceiver(service);

		// UserA login at node 1
		System.out.println("user a login at node 1");
		userA.unlockPrivateKey("test-pass-a");
		Mediator mediatorA = nodes.get(1).getOrRegisterLocalMediator(userA);
		nodes.get(0).storeAgent(userA);

		// UserA send first message to UserB
		System.out.println("user a sending first message to user b");
		mediatorA.invoke(ShortMessageService.class.getName(), "sendShortMessage", new Serializable[] { userB.getId(),
				"First hello world to B from A" }, true);

		// UserB login at node 2
		System.out.println("user b login at node 2");
		userB.unlockPrivateKey("test-pass-b");
		Mediator mediatorB = nodes.get(2).getOrRegisterLocalMediator(userB);
		nodes.get(2).storeAgent(userB);

		// verify UserB received first message
		ShortMessage[] messages1 = (ShortMessage[]) mediatorB.invoke(ShortMessageService.class.getName(),
				"getShortMessages", new Serializable[] {}, true);
		assertEquals(messages1.length, 1);

		// UserA send second message to UserB
		mediatorA.invoke(ShortMessageService.class.getName(), "sendShortMessage", new Serializable[] { userB.getId(),
				"Second hello world to B from A" }, true);

		// verify UserB received two messages
		ShortMessage[] messages2 = (ShortMessage[]) mediatorB.invoke(ShortMessageService.class.getName(),
				"getShortMessages", new Serializable[] {}, true);
		assertEquals(messages2.length, 2);

		// UserA logout
		nodes.get(1).unregisterAgent(userA);

		// verify UserB still got two messages
		ShortMessage[] messages3 = (ShortMessage[]) mediatorB.invoke(ShortMessageService.class.getName(),
				"getShortMessages", new Serializable[] {}, true);
		assertEquals(messages3.length, 2);

		// UserB logout
		nodes.get(2).unregisterAgent(userB);

		stopNetwork();
	}
}
