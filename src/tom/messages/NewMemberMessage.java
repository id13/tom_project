package tom.messages;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.engine.Engine;
import messages.util.ByteUtil;

public class NewMemberMessage extends Message {

  private InetSocketAddress newMember;

  public NewMemberMessage(InetSocketAddress newMember) {
    super(0, Message.NEW_MEMBER, "completed later with setContent");
    this.newMember = newMember;
    byte[] content = new byte[8];
    try {
      ByteUtil.writeInetSocketAddress(content, 0, newMember);
    } catch (UnknownHostException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
    super.setContent(ByteUtil.readString(content));
  }

  public NewMemberMessage(Message message) {
    super(message.getLogicalClock(), Message.NEW_MEMBER, message.getContent());
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
    return "New Member: LC = " + getLogicalClock() + "; new member: " + newMember;
  }
}