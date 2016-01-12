package tom;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.engine.Engine;
import messages.util.ByteUtil;

public class Message {

  public static final byte MESSAGE = (byte) 1;
  public static final byte ACK = (byte) 2;
  public static final byte JOIN = (byte) 3;

  private int logicalClock;
  private byte messageType;
  private String content;
  private InetSocketAddress author;

  /**
   * Create a message providing its headers and its content.
   * 
   * @param logicalClock
   * @param messageType
   * @param content
   * @param author
   */
  public Message(int logicalClock, byte messageType, InetSocketAddress author, String content) {
    this.logicalClock = logicalClock;
    this.messageType = messageType;
    this.author = author;
    this.content = content;
  }

  /**
   * Create a message from the String representing its bits. This string already
   * includes all the headers.
   * 
   * @param fullMessage
   */
  public static Message getMessageReceived(byte[] bytes) {
    if (bytes.length < 13) {
      Engine.panic("This messages is to short so it can not have headers.");
    }
    int logicalClock = ByteUtil.readInt32(bytes, 0);
    byte messageType = bytes[4];
    InetSocketAddress author = null;
    try {
      author = ByteUtil.readInetSocketAddress(bytes, 5);
    } catch (UnknownHostException e) {
      Engine.panic("The received message is malformed.");
    }
    String content = ByteUtil.readString(bytes).substring(13);
    Message message = new Message(logicalClock, messageType, author, content);
    if (messageType == ACK) {
      AckMessage messageAck = new AckMessage(message);
      return messageAck;
    } else {
      return message;
    }
  }

  /**
   * 
   * @return the byte[] representing the message including the headers and the
   *         content.
   */
  public byte[] getFullMessage() {
    byte[] bytes = new byte[13 + content.length()];
    ByteUtil.writeInt32(bytes, 0, logicalClock);
    bytes[4] = messageType;
    ByteUtil.writeInetSocketAddress(bytes, 5, author);
    System.arraycopy(ByteUtil.writeString(content), 0, bytes, 13, content.length());
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
    return "Message: LC: " + logicalClock + "; Author: " + author + "; Type: " + messageType + "; content: " + content;
  }

  public InetSocketAddress getAuthor() {
    return author;
  }

}
