package tom.messages;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import messages.engine.Engine;
import messages.util.ByteUtil;

public class JoinResponseMessage extends Message {

  private ArrayList<InetSocketAddress> group;

  public JoinResponseMessage(Collection<InetSocketAddress> group) {
    super(0, Message.JOIN_RESPONSE, "completed later with setContent");
    this.group = new ArrayList<>(group.size());
    byte[] content = new byte[4 + group.size() * 8];
    ByteUtil.writeInt32(content, 0, group.size());
    int i = 4;
    for (InetSocketAddress member : group) {
      this.group.add(member);
      try {
        ByteUtil.writeInetSocketAddress(content, i, member);
      } catch (UnknownHostException e) {
        e.printStackTrace();
        Engine.panic(e.getMessage());
      }
      i += 8;
    }
    super.setContent(ByteUtil.readString(content));
  }

  public JoinResponseMessage(Message message) {
    super(message.getLogicalClock(), Message.JOIN_RESPONSE, message.getContent());
    byte[] bytes = ByteUtil.writeString(message.getContent());
    int size = ByteUtil.readInt32(bytes, 0);
    this.group = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      try {
        this.group.add(ByteUtil.readInetSocketAddress(bytes, 4 + 8 * i));
      } catch (UnknownHostException e) {
        e.printStackTrace();
        Engine.panic(e.getMessage());
        // TODO: handle exception
      }
    }
  }

  public ArrayList<InetSocketAddress> getGroup() {
    return group;
  }

  @Override
  public String toString() {
    String result = "Join Response: LC: " + super.getLogicalClock();
    result += "\n\t group size: + " + group.size() + "\n";
    for (InetSocketAddress member : group) {
      result += "\t" + member;
    }
    return result;
  }

}