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
import tom.Message;
import tom.Peer;
import tom.AckMessage;
import tom.WaitingMessage;

public class TestWaitingMessage {

	@Test
	public void test1() {

		InetSocketAddress myAddress = new InetSocketAddress("localhost", 12345);
		InetSocketAddress address1 = new InetSocketAddress("localhost", 12351);
		InetSocketAddress address2 = new InetSocketAddress("localhost", 12352);
		InetSocketAddress address3 = new InetSocketAddress("localhost", 12353);
		InetSocketAddress address4 = new InetSocketAddress("localhost", 12354);
		
		MyPeer myPeer = new MyPeer(myAddress);
		Message message = new Message(123, Message.TYPE_MESSAGE, "Hi, how are you?");
		WaitingMessage waitingMessage = new WaitingMessage(message, myPeer);
		assertEquals("Hi, how are you?", waitingMessage.getContent());
		assertEquals(123, waitingMessage.getLogicalClock());

		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		myPeer.addToGroup(address1);
		assertFalse(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		AckMessage messageAck1 = new AckMessage(message, address1, 456);
		waitingMessage.addAck(address1, messageAck1);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		AckMessage messageAck2 = new AckMessage(message, address1, 321);
		waitingMessage.addAck(address2, messageAck2);
		AckMessage messageAck3 = new AckMessage(message, address3, 400);
		waitingMessage.addAck(address3, messageAck3);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		myPeer.addToGroup(address2);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		myPeer.addToGroup(address3);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		myPeer.addToGroup(address4);
		assertFalse(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		AckMessage messageAck4 = new AckMessage(message, address4, 450);
		waitingMessage.addAck(address4, messageAck4);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
	}

	@Test
	public void test2() {
		InetSocketAddress myAddress = new InetSocketAddress("localhost", 12345);
		InetSocketAddress address1 = new InetSocketAddress("localhost", 12351);
		InetSocketAddress address2 = new InetSocketAddress("localhost", 12352);
		InetSocketAddress address3 = new InetSocketAddress("localhost", 12353);
		InetSocketAddress address4 = new InetSocketAddress("localhost", 12354);
		
		MyPeer myPeer = new MyPeer(myAddress);

		Message message = new Message(12, Message.TYPE_MESSAGE, "Hi, how are you?");
		WaitingMessage waitingMessage = new WaitingMessage(message, address1);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		myPeer.addToGroup(address1);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		AckMessage messageAck2 = new AckMessage(message, address2, 321);
		waitingMessage.addAck(address2, messageAck2);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		myPeer.addToGroup(address2);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		myPeer.addToGroup(address3);
		assertFalse(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
		AckMessage messageAck3 = new AckMessage(message, address3, 123);
		waitingMessage.addAck(address3, messageAck3);
		assertTrue(waitingMessage.isReadyToDeliver(myPeer.getGroup()));
	}

	private class MyPeer implements Peer {

		private InetSocketAddress myAddress;
		private Set<InetSocketAddress> group = new HashSet<>();

		private MyPeer(InetSocketAddress myAddress) {
			this.myAddress = myAddress;
		}

		@Override
		public void connect(InetSocketAddress address) {
		}

		@Override
		public void send(String content) {
		}

		@Override
		public Set<InetSocketAddress> getGroup() {
			return this.group;
		}

		@Override
		public InetSocketAddress getMyAddress() {
			return this.myAddress;
		}

		@Override
		public int updateLogicalClock(int outsideLogicalClock) {
			return 0;
		}

		public void addToGroup(InetSocketAddress address) {
			this.group.add(address);
		}

	}
}
