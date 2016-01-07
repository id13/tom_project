package tom.test;

import static org.junit.Assert.*;

import org.junit.Test;

import tom.Message;

public class TestMessage {

  @Test
  public void test() {
    Message message1 = new Message(42, Message.TYPE_MESSAGE, "Hi, how are you?");
    assertEquals(42, message1.getLogicalClock());
    assertEquals(Message.TYPE_MESSAGE, message1.getMessageType());
    assertTrue(new String("Hi, how are you?").equals(message1.getContent()));
    
    Message message2 = Message.getMessageReceived(message1.getFullMessage());
    assertEquals(message1.getLogicalClock(), message2.getLogicalClock());
    assertEquals(message1.getMessageType(), message2.getMessageType());
    assertTrue(message1.getContent().equals(message2.getContent()));
    
    byte[] bytes = new byte[7];
    bytes[0] = 0;
    bytes[1] = 42;
    bytes[2] = -42;
    bytes[3] = 127;
    bytes[4] = -128;
    bytes[5] = -127;
    bytes[6] = -100;
    String gloomyMessage = new String(bytes);
    Message message3 = new Message(123456, Message.TYPE_MESSAGE, gloomyMessage);
    Message message4 = Message.getMessageReceived(message3.getFullMessage());
    assertEquals(message3.getLogicalClock(), message4.getLogicalClock());
    assertEquals(message3.getMessageType(), message4.getMessageType());
    assertTrue(message3.getContent().equals(message4.getContent()));    
  }

}
