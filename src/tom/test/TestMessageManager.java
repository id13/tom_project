package tom.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import messages.engine.Channel;
import messages.engine.ClosableCallback;
import messages.engine.DeliverCallback;
import messages.engine.Server;
import tom.AckMessage;
import tom.Message;
import tom.MessageManager;
import tom.Peer;
import tom.TomDeliverCallback;

public class TestMessageManager {

	@Test
	public void test() {
		InetSocketAddress myAddress = new InetSocketAddress("localhost", 12345);
		InetSocketAddress address1 = new InetSocketAddress("localhost", 12351);
		InetSocketAddress address2 = new InetSocketAddress("localhost", 12352);
		InetSocketAddress address3 = new InetSocketAddress("localhost", 12353);
		MyChannel channel1 = new MyChannel(address1);
		MyChannel channel2 = new MyChannel(address2);
		MyChannel channel3 = new MyChannel(address3);
		MyPeer myPeer = new MyPeer(myAddress);
		myPeer.setCorrectMessage("Nothing should be received now.");
		myPeer.addToGroup(address1);
		myPeer.addToGroup(address2);
		myPeer.addToGroup(address3);
		MessageManager messageManager = new MessageManager(myPeer, myPeer, null);

		// On envoit un message "Message1" sur le canal 1,
		// suivi des acks des canaux 2 et 3:
		Message message1 = new Message(0, Message.TYPE_MESSAGE, address1, "Message1");
		AckMessage ack12 = new AckMessage(message1, address1, 1, address2);
		AckMessage ack13 = new AckMessage(message1, address1, 7, address3);
		messageManager.deliver(channel1, message1.getFullMessage());
		messageManager.deliver(channel2, ack12.getFullMessage());
		myPeer.setCorrectMessage("Message1");
		messageManager.deliver(channel3, ack13.getFullMessage());
		assertEquals(1, myPeer.getNumberOfDeliveredMessages());
		myPeer.setCorrectMessage("Nothing should be received now.");

		// On envoit Le ack2 du Message2, suivi du message2,
		// suivi du ack du canal 3.
		Message message2 = new Message(40, Message.TYPE_MESSAGE, address1, "Message2");
		AckMessage ack22 = new AckMessage(message2, address1, 42, address2);
		AckMessage ack23 = new AckMessage(message2, address1, 41, address3);
		messageManager.deliver(channel2, ack22.getFullMessage());
		messageManager.deliver(channel1, message2.getFullMessage());
		myPeer.setCorrectMessage("Message2");
		messageManager.deliver(channel3, ack23.getFullMessage());
		assertEquals(2, myPeer.getNumberOfDeliveredMessages());
		myPeer.setCorrectMessage("Nothing should be received now.");

		// On envoit Le ack2 du Message3, suivi du ack3 du message3,
		// suivi du message3.
		Message message3 = new Message(80, Message.TYPE_MESSAGE, address1, "Message3");
		AckMessage ack32 = new AckMessage(message3, address1, 83, address2);
		AckMessage ack33 = new AckMessage(message3, address1, 82, address3);
		messageManager.deliver(channel2, ack32.getFullMessage());
		messageManager.deliver(channel3, ack33.getFullMessage());
		myPeer.setCorrectMessage("Message3");
		messageManager.deliver(channel1, message3.getFullMessage());
		assertEquals(3, myPeer.getNumberOfDeliveredMessages());
		myPeer.setCorrectMessage("Nothing should be received now.");
	}

	public class MyPeer implements Peer, TomDeliverCallback {

		private Set<InetSocketAddress> group = new HashSet<>();
		private String correctMessage;
		private int numberOfDeliveredMessages = 0;
		private final InetSocketAddress myAddress;

		public MyPeer(InetSocketAddress myAddress) {
			this.myAddress = myAddress;
		}

		@Override
		public void send(String content) {
		}

		@Override
		public void deliver(String message) {
			if (!message.equals(correctMessage)) {
				fail("Wrong message received, or not at the good time.\n" + "CorrectMessage: " + correctMessage
				    + "DeliveredMessage: " + message);
			}
			this.numberOfDeliveredMessages++;
		}

		public void setCorrectMessage(String correctMessage) {
			this.correctMessage = correctMessage;
		}

		public int getNumberOfDeliveredMessages() {
			return this.numberOfDeliveredMessages;
		}

		@Override
		public void connect(InetSocketAddress address) {
		}

		@Override
		public int updateLogicalClock(int outsideLogicalClock) {
			return outsideLogicalClock + 1;
		}

		@Override
		public Set<InetSocketAddress> getGroup() {
			return group;
		}

		@Override
		public InetSocketAddress getMyAddress() {
			return myAddress;
		}

		public void addToGroup(InetSocketAddress address) {
			this.group.add(address);
		}

	}

	private class MyChannel extends Channel {
		InetSocketAddress address;

		public MyChannel(InetSocketAddress address) {
			this.address = address;
		}

		@Override
		public int compareTo(Channel o) {
			return 0;
		}

		@Override
		public void setDeliverCallback(DeliverCallback callback) {
		}

		@Override
		public InetSocketAddress getRemoteAddress() throws IOException {
			return this.address;
		}

		@Override
		public void send(byte[] bytes, int offset, int length) throws IOException {
		}

		@Override
		public void close() {
		}

		@Override
		public void setServer(Server server) {
		}

		@Override
		public Server getServer() {
			return null;
		}

		@Override
		public void setClosableCallback(ClosableCallback callback) {
		}

	}
}
