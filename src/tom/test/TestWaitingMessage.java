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
import tom.AckMessage;
import tom.WaitingMessage;

public class TestWaitingMessage {

  @Test
  public void test1() {
    MyServer server1 = new MyServer(1);
    MyServer server2 = new MyServer(2);
    MyServer server3 = new MyServer(3);
    MyServer server4 = new MyServer(4);
    MyServer myServer = new MyServer(42);

    MyChannel channel1 = new MyChannel(server1);
    MyChannel channel2 = new MyChannel(server2);
    MyChannel channel3 = new MyChannel(server3);
    MyChannel channel4 = new MyChannel(server4);

    MyChannel myChannel = new MyChannel(myServer);

    Message message = new Message(123, Message.TYPE_MESSAGE, "Hi, how are you?");
    WaitingMessage waitingMessage = new WaitingMessage(message, 42);
    assertEquals("Hi, how are you?", waitingMessage.getContent());
    assertEquals(123, waitingMessage.getLogicalClock());

    Set<Channel> channels = new HashSet<>();
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    channels.add(channel1);
    assertFalse(waitingMessage.isReadyToDeliver(channels));
    AckMessage messageAck1 = new AckMessage(message, channel1, 456);
    waitingMessage.addAck(channel1, messageAck1);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    AckMessage messageAck2 = new AckMessage(message, channel2, 321);
    waitingMessage.addAck(channel2, messageAck2);
    AckMessage messageAck3 = new AckMessage(message, channel3, 400);
    waitingMessage.addAck(channel3, messageAck3);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    channels.add(channel2);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    channels.add(channel3);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    channels.add(channel4);
    assertFalse(waitingMessage.isReadyToDeliver(channels));
    AckMessage messageAck4 = new AckMessage(message, channel4, 450);
    waitingMessage.addAck(channel4, messageAck4);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
  }

  @Test
  public void test2() {
    MyServer server1 = new MyServer(1);
    MyServer server2 = new MyServer(2);
    MyServer server3 = new MyServer(3);
    MyServer server4 = new MyServer(4);

    MyChannel channel1 = new MyChannel(server1);
    MyChannel channel2 = new MyChannel(server2);
    MyChannel channel3 = new MyChannel(server3);
    MyChannel channel4 = new MyChannel(server4);
    
    Message message = new Message(12, Message.TYPE_MESSAGE, "Hi, how are you?");
    WaitingMessage waitingMessage = new WaitingMessage(message, channel1);
    Set<Channel> channels = new HashSet<>();
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    channels.add(channel1);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    AckMessage messageAck2 = new AckMessage(message, channel2, 321);
    waitingMessage.addAck(channel2, messageAck2);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    channels.add(channel2);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
    channels.add(channel3);
    assertFalse(waitingMessage.isReadyToDeliver(channels));
    AckMessage messageAck3 = new AckMessage(message, channel3, 123);
    waitingMessage.addAck(channel3, messageAck3);
    assertTrue(waitingMessage.isReadyToDeliver(channels));
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
