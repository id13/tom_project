package tom.test;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.util.Arrays;

import org.junit.Test;

import messages.util.ByteUtil;
import tom.Message;
import tom.AckMessage;

public class TestAckMessage {

  @Test
  public void test() {
    InetSocketAddress distAddress = new InetSocketAddress("localhost", 54321);
    Message message1 = new Message(18, Message.MESSAGE, "Hi, how are you?");
    AckMessage messageAck = new AckMessage(message1, distAddress, 789);
    assertEquals(ByteUtil.computeCRC32(ByteUtil.writeString("Hi, how are you?")), messageAck.getCrc32());
    assertEquals(distAddress, messageAck.getAuthor());
    assertEquals(18, messageAck.getLogicalClockAuthor());
    assertEquals(789, messageAck.getLogicalClock());
    assertEquals(Message.ACK, messageAck.getMessageType());

    Message messageAck2 = Message.getMessageReceived(messageAck.getFullMessage());
    assertTrue(Arrays.equals(messageAck.getFullMessage(), messageAck2.getFullMessage()));
    assertEquals(messageAck.getLogicalClock(), messageAck2.getLogicalClock());
    assertEquals(messageAck.getMessageType(), messageAck2.getMessageType());
    AckMessage messageAck3 = (AckMessage) messageAck2;
    assertEquals(messageAck.getCrc32(), messageAck3.getCrc32());
    assertEquals(messageAck.getAuthor(), messageAck3.getAuthor());
    assertEquals(messageAck.getLogicalClockAuthor(), messageAck3.getLogicalClockAuthor());
  }
}
