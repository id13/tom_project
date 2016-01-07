package tom;

import messages.engine.Engine;
import messages.util.ByteUtil;

public class Message {

  public static final byte TYPE_MESSAGE = (byte) 1;
  public static final byte TYPE_ACK = (byte) 2;
  public static final byte TYPE_JOIN = (byte) 3;
  
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
  public static Message getMessageReceived(String fullMessage) {
    if (fullMessage.length() < 5) {
      Engine.panic("This messages is to short so it can not have headers.");
    }
    byte[] bytes = fullMessage.getBytes();
    int logicalClock = ByteUtil.readInt32(bytes, 0);
    byte messageType = bytes[4];
    String content = fullMessage.substring(5);
    Message message = new Message(logicalClock, messageType, content);
    if (messageType == TYPE_ACK) {
      MessageAck messageAck = new MessageAck(message);
      return messageAck;
    } else {
      return message;
    }
  }

  /**
   * 
   * @return the String representing the message including the headers
   * and the content.
   */
  public String getFullMessage() {
    byte[] headers = new byte[5];
    ByteUtil.writeInt32(headers, 0, logicalClock);
    headers[4] = messageType;
    String stringHeaders = new String(headers);
    return stringHeaders + content;
  }
  
  public MessageAck getMessageAck() {
    return new MessageAck(this);
  }

  public int getLogicalClock() {
    return logicalClock;
  }

  public byte getMessageType() {
    return messageType;
  }

  /**
   * 
   * @return the String representing the content encapsulated in the message.
   * In particular, the headers are not included in it.
   */
  public String getContent() {
    return content;
  }

  /**
   * Used only for the builder of MessageAck.
   * @param content
   */
  protected void setContent(String content) {
    this.content = content;
  }
}
