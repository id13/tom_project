package tom.test;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;

import org.junit.Test;

import tom.messages.Message;
import tom.messages.NewMemberMessage;

public class TestNewMember {

  @Test
  public void test() {
    InetSocketAddress address = new InetSocketAddress("localhost", 4242);
    NewMemberMessage newMember = new NewMemberMessage(address);
    assertEquals(Message.NEW_MEMBER, newMember.getMessageType());
    assertTrue(address.equals(newMember.getNewMember()));
    
    NewMemberMessage join2 = (NewMemberMessage) Message.getMessageReceived(newMember.getFullMessage());
    
    assertEquals(newMember.getLogicalClock(), join2.getLogicalClock());
    assertEquals(newMember.getMessageType(), join2.getMessageType());
    assertTrue(newMember.getNewMember().equals(join2.getNewMember()));
    assertEquals(newMember.getContent(), join2.getContent());
  }

}
