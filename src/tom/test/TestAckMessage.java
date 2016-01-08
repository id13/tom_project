package tom.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;

import org.junit.Test;

import messages.engine.Channel;
import messages.engine.ClosableCallback;
import messages.engine.DeliverCallback;
import messages.engine.Server;
import messages.util.ByteUtil;
import tom.Message;
import tom.AckMessage;

public class TestAckMessage {

  @Test
  public void test() {
    Message message1 = new Message(18, Message.TYPE_MESSAGE, "Hi, how are you?");
    MyServer myServer = new MyServer(42);
    MyChannel myChannel = new MyChannel(myServer);
    
    AckMessage messageAck = new AckMessage(message1, myChannel, 789);
    assertEquals(ByteUtil.computeCRC32(ByteUtil.writeString("Hi, how are you?")),
        messageAck.getCrc32());
    assertEquals(42, messageAck.getPortNumberAuthor());
    assertEquals(18, messageAck.getLogicalClockAuthor());
    assertEquals(789, messageAck.getLogicalClock());
    assertEquals(Message.TYPE_ACK, messageAck.getMessageType());
    
    Message messageAck2 = Message.getMessageReceived(messageAck.getFullMessage());
    assertTrue(Arrays.equals(messageAck.getFullMessage(), messageAck2.getFullMessage()));
    assertEquals(messageAck.getLogicalClock(), messageAck2.getLogicalClock());
    assertEquals(messageAck.getMessageType(), messageAck2.getMessageType());
    AckMessage messageAck3 = (AckMessage) messageAck2;
    assertEquals(messageAck.getCrc32(), messageAck3.getCrc32());
    assertEquals(messageAck.getPortNumberAuthor(), messageAck3.getPortNumberAuthor());
    assertEquals(messageAck.getLogicalClockAuthor(), messageAck3.getLogicalClockAuthor());
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
