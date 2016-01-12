package tom.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Test;

import messages.engine.Channel;
import messages.engine.ClosableCallback;
import messages.engine.DeliverCallback;
import messages.engine.Server;
import tom.Message;
import tom.Peer;
import tom.AckMessage;
import tom.DistantPeerManager;
import tom.WaitingMessage;

public class TestWaitingMessage {

  @Test
  public void test1() {

    InetSocketAddress myAddress = new InetSocketAddress("localhost", 12345);
    InetSocketAddress address1 = new InetSocketAddress("localhost", 12351);
    InetSocketAddress address2 = new InetSocketAddress("localhost", 12352);
    InetSocketAddress address3 = new InetSocketAddress("localhost", 12353);
    InetSocketAddress address4 = new InetSocketAddress("localhost", 12354);
    Channel channel1 = new MyChannel();
    Channel channel2 = new MyChannel();
    Channel channel3 = new MyChannel();
    Channel channel4 = new MyChannel();

    // We send a Message and we receive ack from 1,2,3 and 4 in the good order.
    MyPeer myPeer = new MyPeer(myAddress);
    DistantPeerManager manager = myPeer.getDistantPeerManager();
    Message message = new Message(123, Message.MESSAGE, myAddress, "Hi, how are you?");
    WaitingMessage waitingMessage = new WaitingMessage(message, myPeer);
    assertEquals("Hi, how are you?", waitingMessage.getContent());
    assertEquals(123, waitingMessage.getLogicalClock());
    manager.addId(channel1, address1);
    manager.addId(channel2, address2);
    manager.addId(channel3, address3);
    manager.addId(channel4, address4);

    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addChannel(channel1);
    assertFalse(manager.allAckReceived(waitingMessage));
    AckMessage messageAck1 = new AckMessage(message, myAddress, 456, address1);
    waitingMessage.addAck(address1, messageAck1);
    assertTrue(manager.allAckReceived(waitingMessage));
    AckMessage messageAck2 = new AckMessage(message, myAddress, 321, address2);
    waitingMessage.addAck(address2, messageAck2);
    AckMessage messageAck3 = new AckMessage(message, myAddress, 400, address3);
    waitingMessage.addAck(address3, messageAck3);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addChannel(channel2);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addChannel(channel3);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addChannel(channel4);
    assertFalse(manager.allAckReceived(waitingMessage));
    AckMessage messageAck4 = new AckMessage(message, myAddress, 450, address4);
    waitingMessage.addAck(address4, messageAck4);
    assertTrue(manager.allAckReceived(waitingMessage));
  }

  @Test
  public void test2() {
    InetSocketAddress myAddress = new InetSocketAddress("localhost", 12345);
    InetSocketAddress address1 = new InetSocketAddress("localhost", 12351);
    InetSocketAddress address2 = new InetSocketAddress("localhost", 12352);
    InetSocketAddress address3 = new InetSocketAddress("localhost", 12353);
    Channel channel1 = new MyChannel();
    Channel channel2 = new MyChannel();
    Channel channel3 = new MyChannel();

    MyPeer myPeer = new MyPeer(myAddress);
    DistantPeerManager manager = myPeer.getDistantPeerManager();
    manager.addId(channel1, address1);
    manager.addId(channel2, address2);
    manager.addId(channel3, address3);

    Message message = new Message(12, Message.MESSAGE, myAddress, "Hi, how are you?");
    WaitingMessage waitingMessage = new WaitingMessage(message, address1);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addChannel(channel1);
    assertTrue(manager.allAckReceived(waitingMessage));
    AckMessage messageAck2 = new AckMessage(message, address1, 321, address2);
    waitingMessage.addAck(address2, messageAck2);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addChannel(channel2);
    assertTrue(manager.allAckReceived(waitingMessage));
    manager.addChannel(channel3);
    assertFalse(manager.allAckReceived(waitingMessage));
    AckMessage messageAck3 = new AckMessage(message, address1, 123, address3);
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

  }

  private class MyChannel extends Channel {

    @Override
    public int compareTo(Channel arg0) {
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
