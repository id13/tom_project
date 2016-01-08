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
  	MyServer server1 = new MyServer(1);
  	MyServer server2 = new MyServer(2);
  	MyServer server3 = new MyServer(3);
  	MyChannel channel1 = new MyChannel(server1);
  	MyChannel channel2 = new MyChannel(server2);
  	MyChannel channel3 = new MyChannel(server3);
  	
  	Set<Channel> channels = new HashSet<>();
  	channels.add(channel1);
  	channels.add(channel2);
  	channels.add(channel3);
  	
  	MyPeer myPeer = new MyPeer(channels);
  	myPeer.setCorrectMessage("Nothing should be received now.");
  	MessageManager messageManager = new MessageManager(myPeer, myPeer);
  	
  	// On envoit un message "Message1" sur le canal 1,
  	// suivi des acks des canaux 2 et 3:
  	Message message1 = new Message(0, Message.TYPE_MESSAGE, "Message1");
  	AckMessage ack12 = new AckMessage(message1, channel1, 1);
  	AckMessage ack13 = new AckMessage(message1, channel1, 7);
  	messageManager.deliver(channel1, message1.getFullMessage());
  	messageManager.deliver(channel2, ack12.getFullMessage());
  	myPeer.setCorrectMessage("Message1");
  	messageManager.deliver(channel3, ack13.getFullMessage());
  	assertEquals(1, myPeer.getNumberOfDeliveredMessages());
  	myPeer.setCorrectMessage("Nothing should be received now.");
  	
  	// On envoit Le ack2 du Message2, suivi du message2,
  	// suivi du ack du canal 3.
  	Message message2 = new Message(40, Message.TYPE_MESSAGE, "Message2");
  	AckMessage ack22 = new AckMessage(message2, channel1, 42);
  	AckMessage ack23 = new AckMessage(message2, channel1, 41);
  	messageManager.deliver(channel2, ack22.getFullMessage());
  	messageManager.deliver(channel1, message2.getFullMessage());
  	myPeer.setCorrectMessage("Message2");
  	messageManager.deliver(channel3, ack23.getFullMessage());
  	assertEquals(2, myPeer.getNumberOfDeliveredMessages());
  	myPeer.setCorrectMessage("Nothing should be received now.");

  	// On envoit Le ack2 du Message3, suivi du ack3 du message3,
  	// suivi du message3.
  	Message message3 = new Message(80, Message.TYPE_MESSAGE, "Message3");
  	AckMessage ack32 = new AckMessage(message3, channel1, 83);
  	AckMessage ack33 = new AckMessage(message3, channel1, 82);
  	messageManager.deliver(channel2, ack32.getFullMessage());
  	messageManager.deliver(channel3, ack33.getFullMessage());
  	myPeer.setCorrectMessage("Message3");
  	messageManager.deliver(channel1, message3.getFullMessage());
  	assertEquals(3, myPeer.getNumberOfDeliveredMessages());
  	myPeer.setCorrectMessage("Nothing should be received now.");
  }

  public class MyPeer implements Peer, TomDeliverCallback {

  	private final Set<Channel> channels;
  	private String correctMessage;
  	private int numberOfDeliveredMessages = 0;
  	public MyPeer(Set<Channel> channels) {
  		this.channels = channels;
  	}
  	
		@Override
		public void send(String content) {
		}

		@Override
		public Set<Channel> getChannelGroup() {
			return this.channels;
		}

		@Override
		public void deliver(String message) {
			if (!message.equals(correctMessage)) {
				fail("Wrong message received, or not at the good time.\n"
						+ "CorrectMessage: "+correctMessage
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
  }
  
  public class MyServer extends Server {

    private int port;

    public MyServer(int port) {
      this.port = port;
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public void close() throws IOException {
    }
  }

  public class MyChannel extends Channel {

    private Server server;

    public MyChannel(Server server) {
      this.server = server;
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
      return null;
    }

    @Override
    public void send(byte[] bytes, int offset, int length) throws IOException {
    }

    @Override
    public void close() {
    }

    @Override
    public void setServer(Server server) {
      this.server = server;
    }

    @Override
    public Server getServer() {
      return server;
    }

    @Override
    public void setClosableCallback(ClosableCallback callback) {
    }
  }
}
