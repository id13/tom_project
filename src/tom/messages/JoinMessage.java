package tom.messages;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.engine.Engine;
import messages.util.ByteUtil;

public class JoinMessage extends Message {
  
  private InetSocketAddress newMember;

  public JoinMessage(int logicalClock, InetSocketAddress newMember) {
    super(logicalClock, Message.JOIN, "completed later with setContent");
    this.newMember = newMember;
    byte[] content = new byte[8];
    ByteUtil.writeInetSocketAddress(content, 0, newMember);
    super.setContent(ByteUtil.readString(content));
  }
  
  public JoinMessage(Message message) {
    super(message.getLogicalClock(), Message.JOIN, message.getContent());
    byte[] content = ByteUtil.writeString(message.getContent());
    try {
      this.newMember = ByteUtil.readInetSocketAddress(content, 0);
    } catch (UnknownHostException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
      // TODO: handle exception
    }
  }
  
  public InetSocketAddress getNewMember() {
    return newMember;
  }

  @Override
  public String toString() {
    return "JOIN : LC: "+ getLogicalClock() + "; new member: " + newMember;
  }
}
