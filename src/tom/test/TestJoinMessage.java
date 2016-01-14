package tom.test;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;

import org.junit.Test;

import tom.messages.JoinMessage;
import tom.messages.Message;

public class TestJoinMessage {

  @Test
  public void test() {
    InetSocketAddress address = new InetSocketAddress("localhost", 4242);
    JoinMessage join = new JoinMessage(42, address);
    assertEquals(42, join.getLogicalClock());
    assertEquals(Message.JOIN, join.getMessageType());
    assertTrue(address.equals(join.getNewMember()));
    
    JoinMessage join2 = (JoinMessage) Message.getMessageReceived(join.getFullMessage());
    
    assertEquals(join.getLogicalClock(), join2.getLogicalClock());
    assertEquals(join.getMessageType(), join2.getMessageType());
    assertTrue(join.getNewMember().equals(join2.getNewMember()));
    assertEquals(join.getContent(), join2.getContent());
  }

}
