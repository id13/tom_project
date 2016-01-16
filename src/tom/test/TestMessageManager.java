package tom.test;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import org.junit.Test;

import tom.DistantPeerManager;
import tom.MessageManager;
import tom.Peer;
import tom.TomDeliverCallback;
import tom.messages.AckMessage;
import tom.messages.Message;

public class TestMessageManager {

  @Test
  public void test() {
    InetSocketAddress myAddress = new InetSocketAddress("localhost", 12345);
    InetSocketAddress address1 = new InetSocketAddress("localhost", 12351);
    InetSocketAddress address2 = new InetSocketAddress("localhost", 12352);
    InetSocketAddress address3 = new InetSocketAddress("localhost", 12353);
    MyPeer myPeer = new MyPeer(myAddress);
    myPeer.setCorrectMessage("Nothing should be received now.");
    DistantPeerManager manager = myPeer.getDistantPeerManager();
    manager.addMember(address1);
    manager.addMember(address2);
    manager.addMember(address3);
    MessageManager messageManager = new MessageManager(myPeer, myPeer, null, manager);

    // On envoit un message "Message1" sur le canal 1,
    // suivi des acks des canaux 2 et 3:
    Message message1 = new Message(0, Message.MESSAGE, "Message1");
    AckMessage ack12 = new AckMessage(message1, address1, 1);
    AckMessage ack13 = new AckMessage(message1, address1, 7);
    messageManager.delivered(address1, message1.getFullMessage());
    messageManager.delivered(address2, ack12.getFullMessage());
    myPeer.setCorrectMessage("Message1");
    messageManager.delivered(address3, ack13.getFullMessage());
    assertEquals(1, myPeer.getNumberOfDeliveredMessages());
    myPeer.setCorrectMessage("Nothing should be received now.");

    // On envoit Le ack2 du Message2, suivi du message2,
    // suivi du ack du canal 3.
    Message message2 = new Message(40, Message.MESSAGE, "Message2");
    AckMessage ack22 = new AckMessage(message2, address1, 42);
    AckMessage ack23 = new AckMessage(message2, address1, 41);
    messageManager.delivered(address2, ack22.getFullMessage());
    messageManager.delivered(address1, message2.getFullMessage());
    myPeer.setCorrectMessage("Message2");
    messageManager.delivered(address3, ack23.getFullMessage());
    assertEquals(2, myPeer.getNumberOfDeliveredMessages());
    myPeer.setCorrectMessage("Nothing should be received now.");

    // On envoit Le ack2 du Message3, suivi du ack3 du message3,
    // suivi du message3.
    Message message3 = new Message(80, Message.MESSAGE, "Message3");
    AckMessage ack32 = new AckMessage(message3, address1, 83);
    AckMessage ack33 = new AckMessage(message3, address1, 82);
    messageManager.delivered(address2, ack32.getFullMessage());
    messageManager.delivered(address3, ack33.getFullMessage());
    myPeer.setCorrectMessage("Message3");
    messageManager.delivered(address1, message3.getFullMessage());
    assertEquals(3, myPeer.getNumberOfDeliveredMessages());
    myPeer.setCorrectMessage("Nothing should be received now.");
  }

  public class MyPeer implements Peer, TomDeliverCallback {

    private String correctMessage;
    private int numberOfDeliveredMessages = 0;
    private final InetSocketAddress myAddress;
    private final DistantPeerManager distantPeerManager = new DistantPeerManager();

    public MyPeer(InetSocketAddress myAddress) {
      this.myAddress = myAddress;
    }

    @Override
    public void send(String content) {
    }

    @Override
    public void deliver(InetSocketAddress from, String message) {
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
    public InetSocketAddress getMyAddress() {
      return myAddress;
    }

    @Override
    public DistantPeerManager getDistantPeerManager() {
      return this.distantPeerManager;
    }

    @Override
    public void setConnected(int logicalClock) {
    }

    @Override
    public boolean isInGroup() {
      return false;
    }

  }
}
