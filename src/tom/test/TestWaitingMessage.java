package tom.test;

import static org.junit.Assert.*;
import java.net.InetSocketAddress;
import org.junit.Test;

import tom.Peer;
import tom.DistantPeerManager;
import tom.WaitingMessage;
import tom.messages.AckMessage;
import tom.messages.Message;

public class TestWaitingMessage {

  @Test
  public void test1() {

    InetSocketAddress myAddress = new InetSocketAddress("localhost", 12345);
    InetSocketAddress address1 = new InetSocketAddress("localhost", 12351);
    InetSocketAddress address2 = new InetSocketAddress("localhost", 12352);
    InetSocketAddress address3 = new InetSocketAddress("localhost", 12353);
    InetSocketAddress address4 = new InetSocketAddress("localhost", 12354);

    // We send a Message and we receive ack from 1,2,3 and 4 in the good order.
    MyPeer myPeer = new MyPeer(myAddress);
    DistantPeerManager manager = myPeer.getDistantPeerManager();
    Message message = new Message(123, Message.MESSAGE, "Hi, how are you?");
    WaitingMessage waitingMessage = new WaitingMessage(message, myPeer);
    assertEquals("Hi, how are you?", waitingMessage.getContent().getContent());
    assertEquals(123, waitingMessage.getLogicalClock());

    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addMember(address1);
    assertFalse(manager.allAckReceived(waitingMessage));
    AckMessage messageAck1 = new AckMessage(message, myAddress, 456);
    waitingMessage.addAck(address1, messageAck1);
    assertTrue(manager.allAckReceived(waitingMessage));
    AckMessage messageAck2 = new AckMessage(message, myAddress, 321);
    waitingMessage.addAck(address2, messageAck2);
    AckMessage messageAck3 = new AckMessage(message, myAddress, 400);
    waitingMessage.addAck(address3, messageAck3);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addMember(address2);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addMember(address3);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addMember(address4);
    assertFalse(manager.allAckReceived(waitingMessage));
    AckMessage messageAck4 = new AckMessage(message, myAddress, 450);
    waitingMessage.addAck(address4, messageAck4);
    assertTrue(manager.allAckReceived(waitingMessage));
  }

  @Test
  public void test2() {
    InetSocketAddress myAddress = new InetSocketAddress("localhost", 12345);
    InetSocketAddress address1 = new InetSocketAddress("localhost", 12351);
    InetSocketAddress address2 = new InetSocketAddress("localhost", 12352);
    InetSocketAddress address3 = new InetSocketAddress("localhost", 12353);

    MyPeer myPeer = new MyPeer(myAddress);
    DistantPeerManager manager = myPeer.getDistantPeerManager();

    Message message = new Message(12, Message.MESSAGE, "Hi, how are you?");
    WaitingMessage waitingMessage = new WaitingMessage(message, address1);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addMember(address1);
    assertTrue(manager.allAckReceived(waitingMessage));
    AckMessage messageAck2 = new AckMessage(message, address1, 321);
    waitingMessage.addAck(address2, messageAck2);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addMember(address2);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addMember(address3);
    assertFalse(manager.allAckReceived(waitingMessage));
    AckMessage messageAck3 = new AckMessage(message, address1, 123);
    waitingMessage.addAck(address3, messageAck3);
    assertTrue(manager.allAckReceived(waitingMessage));
  }

  private class MyPeer implements Peer {

    private InetSocketAddress myAddress;
    private DistantPeerManager distantPeerManager = new DistantPeerManager();

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
    public InetSocketAddress getMyAddress() {
      return this.myAddress;
    }

    @Override
    public int updateLogicalClock(int outsideLogicalClock) {
      return 0;
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
