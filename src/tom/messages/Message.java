package tom.messages;

import messages.engine.Engine;
import messages.util.ByteUtil;

public class Message {

  public static final byte MESSAGE = (byte) 1;
  public static final byte ACK = (byte) 2;
  public static final byte JOIN = (byte) 3;
  public static final byte JOIN_REQUEST = (byte) 4;
  public static final byte JOIN_RESPONSE = (byte) 5;
  public static final byte NEW_MEMBER = (byte) 6;
  public static final byte WELCOME = (byte) 7;
  private int logicalClock;
  private byte messageType;
  private String content;

  /**
   * Create a message providing its headers and its content.
   * 
   * @param logicalClock
   * @param messageType
   * @param content
   */
  public Message(int logicalClock, byte messageType, String content) {
    this.logicalClock = logicalClock;
    this.messageType = messageType;
    this.content = content;
  }

  /**
   * Create a message from the String representing its bits. This string already
   * includes all the headers.
   * 
   * @param fullMessage
   */
  public static Message getMessageReceived(byte[] bytes) {
    if (bytes.length < 5) {
      Engine.panic("This messages is to short so it can not have headers.");
    }
    int logicalClock = ByteUtil.readInt32(bytes, 0);
    byte messageType = bytes[4];
    String content = ByteUtil.readString(bytes).substring(5); // TODO: verify
                                                              // that
    Message message = new Message(logicalClock, messageType, content);
    switch (messageType) {
    case MESSAGE:
      return message;
    case ACK:
      return new AckMessage(message);
    case JOIN:
      // TODO
      return null;
    case JOIN_REQUEST:
      return new JoinRequestMessage();
    case JOIN_RESPONSE:
      return new JoinResponseMessage(message);
    case NEW_MEMBER:
      return new NewMemberMessage(message);
    case WELCOME:
      return new WelcomeMessage(message);
    default:
      Engine.panic("Message type unknown");
      return null;
    }
  }

  /**
   * 
   * @return the byte[] representing the message including the headers and the
   *         content.
   */
  public byte[] getFullMessage() {
    byte[] bytes = new byte[5 + content.length()];
    ByteUtil.writeInt32(bytes, 0, logicalClock);
    bytes[4] = messageType;
    System.arraycopy(ByteUtil.writeString(content), 0, bytes, 5, content.length());
    return bytes;
  }

  public int getLogicalClock() {
    return logicalClock;
  }

  public byte getMessageType() {
    return messageType;
  }

  /**
   * 
   * @return the String representing the content encapsulated in the message. In
   *         particular, the headers are not included in it.
   */
  public String getContent() {
    return content;
  }

  /**
   * Used only for the builder of MessageAck.
   * 
   * @param content
   */
  protected void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "Message: LC: " + logicalClock + "; Type: " + messageType + "; content: " + content;
  }
}
