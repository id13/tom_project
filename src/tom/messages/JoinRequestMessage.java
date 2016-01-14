package tom.messages;

public class JoinRequestMessage extends Message {

  public JoinRequestMessage() {
    super(0, Message.JOIN_REQUEST, "");
  }

  @Override
  public String toString() {
    return "Join Request";
  }
}
