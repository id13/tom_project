package tom.test;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import tom.messages.JoinMessage;
import tom.messages.JoinResponseMessage;
import tom.messages.Message;

public class TestJoinResponse {

  @Test
  public void test() {
    InetSocketAddress address1 = new InetSocketAddress("localhost", 42421);
    InetSocketAddress address2 = new InetSocketAddress("localhost", 42422);
    InetSocketAddress address3 = new InetSocketAddress("localhost", 42423);
    InetSocketAddress address4 = new InetSocketAddress("localhost", 42424);

    Set<InetSocketAddress> addresses = new HashSet<>();
    
    // test with an empty set:
    JoinResponseMessage joinResponse = new JoinResponseMessage(42, addresses);
    assertEquals(42, joinResponse.getLogicalClock());
    assertEquals(Message.JOIN_RESPONSE, joinResponse.getMessageType());
    assertTrue(joinResponse.getGroup().isEmpty());
    
    JoinResponseMessage joinResponse2 = (JoinResponseMessage) Message.getMessageReceived(joinResponse.getFullMessage());
    
    assertEquals(joinResponse.getLogicalClock(), joinResponse2.getLogicalClock());
    assertEquals(joinResponse.getMessageType(), joinResponse2.getMessageType());
    assertEquals(joinResponse.getContent(), joinResponse2.getContent());

    // Test with a set of one item:
    addresses.add(address1);
    JoinResponseMessage joinResponse3 = new JoinResponseMessage(42, addresses);
    assertEquals(42, joinResponse3.getLogicalClock());
    assertEquals(Message.JOIN_RESPONSE, joinResponse3.getMessageType());
    assertEquals(1, joinResponse3.getGroup().size());
    assertTrue(address1.equals(joinResponse3.getGroup().get(0)));
    
    JoinResponseMessage joinResponse4 = (JoinResponseMessage) Message.getMessageReceived(joinResponse3.getFullMessage());
    
    assertEquals(joinResponse3.getLogicalClock(), joinResponse4.getLogicalClock());
    assertEquals(joinResponse3.getMessageType(), joinResponse4.getMessageType());
    assertEquals(joinResponse3.getContent(), joinResponse4.getContent());

    // Test with a set of 4 items:
    addresses.add(address2);
    addresses.add(address3);
    addresses.add(address4);
    JoinResponseMessage joinResponse5 = new JoinResponseMessage(42, addresses);
    assertEquals(42, joinResponse5.getLogicalClock());
    assertEquals(Message.JOIN_RESPONSE, joinResponse5.getMessageType());
    assertEquals(4, joinResponse5.getGroup().size());
    assertTrue(joinResponse5.getGroup().contains(address1));
    assertTrue(joinResponse5.getGroup().contains(address2));
    assertTrue(joinResponse5.getGroup().contains(address3));
    assertTrue(joinResponse5.getGroup().contains(address4));
    
    JoinResponseMessage joinResponse6 = (JoinResponseMessage) Message.getMessageReceived(joinResponse5.getFullMessage());
    
    assertEquals(joinResponse5.getLogicalClock(), joinResponse6.getLogicalClock());
    assertEquals(joinResponse5.getMessageType(), joinResponse6.getMessageType());
    assertEquals(joinResponse5.getContent(), joinResponse6.getContent());
  
  }

}
