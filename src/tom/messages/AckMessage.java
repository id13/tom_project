package tom.messages;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.engine.Engine;
import messages.util.ByteUtil;

/**
 * A messageAck is a message presenting an acknowledgment. So it is a Message
 * with its headers, and it has three other details. The pair
 * (logicalClockAuthor, author) identify completely the acknowledged
 * message. Indeed, at a time, a server can produce only one message. The crc32
 * allow to assert that the message has well been received in the same way by
 * everybody.
 *
 */
public class AckMessage extends Message {

  private int logicalClockAuthor;
  private InetSocketAddress author;
  private long crc32;

  /**
   * This builder is used to create an ACK from a received Message. Thus, when
   * we receive a message of type TYPE_MESSAGE, we have to acknowledge it and to
   * create the AckMessage, we use this builder.
   * 
   * @param messageToAck
   * @param author
   *          : The InetSocketAddress of the message's author.
   * @param myLogicalClock
   *          : The logical clock of our own Server.
   */
  public AckMessage(Message messageToAck, InetSocketAddress author, int myLogicalClock) {
    super(myLogicalClock, Message.ACK, "completed later with setContent");
    if (messageToAck.getMessageType() != Message.MESSAGE) {
      Engine.panic("Builder MessageAck incorrectly used");
    }
    byte[] contentAck = new byte[20];
    this.logicalClockAuthor = messageToAck.getLogicalClock();
    this.author = author;
    this.crc32 = ByteUtil.computeCRC32(ByteUtil.writeString(messageToAck.getContent()));
    ByteUtil.writeInt32(contentAck, 0, this.logicalClockAuthor);
    ByteUtil.writeInetSocketAddress(contentAck, 4, author);
    ByteUtil.writeLong64(contentAck, 12, this.crc32);
    this.setContent(ByteUtil.readString(contentAck));
  }

  /**
   * This builder is used to extract the data of a Message received, known to be
   * of type TYPE_ACK. This method should be used only in
   * Message.getMessageReceived(String).
   * 
   * @param message
   *          : The message of type TYPE_ACK received.
   */
  AckMessage(Message message) {
    super(message.getLogicalClock(), Message.ACK, message.getContent());
    if (message.getMessageType() != Message.ACK) {
      Engine.panic("Builder MessageAck incorrectly used");
    }
    if (message.getContent().length() != 20) {
      Engine.panic("MessageAck of the wrong size. Here size=" + message.getContent().length());
    }
    byte[] contentAck = ByteUtil.writeString(message.getContent());
    this.logicalClockAuthor = ByteUtil.readInt32(contentAck, 0);
    try {
      this.author = ByteUtil.readInetSocketAddress(contentAck, 4);
    } catch (UnknownHostException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
      // TODO: handle exception
    }
    this.crc32 = ByteUtil.readLong64(contentAck, 12);
  }

  public int getLogicalClockAuthor() {
    return logicalClockAuthor;
  }

  public InetSocketAddress getAuthor() {
    return this.author;
  }

  public long getCrc32() {
    return crc32;
  }

  @Override
  public String toString() {
    return "ACK: LC: " + getLogicalClock() + "; authorLC: " + logicalClockAuthor + "; AuthorOfAckedMessage: "
        + author + "; CRC: " + crc32;
  }
}