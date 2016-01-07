package tom;

import messages.engine.Channel;
import messages.engine.Engine;
import messages.util.ByteUtil;

/**
 * A messageAck is a message presenting an acknowledgment.
 * So it is a Message with its headers, and it has three other details.
 * The pair (logicalClockAuthor, portNumberAuthor) identify completly the
 * acked message. Indeed, at a time, a server can produce only one message.
 * The crc32 allow to assert that the message has well been received in
 * the same way by everybody.
 * @author Thibaut PERRIN
 *
 */
public class MessageAck extends Message{

  private int logicalClockAuthor;
  private int portNumberAuthor;
  private long crc32;
  
  /**
   * This builder is used to create an ACK from a received Message.
   * Thus, when we receive a message of type TYPE_MESSAGE, we have to
   * acknowledge it and to do that, we use this builder.
   * @param messageToAck
   * @param channel : The channel from which we receive the message.
   * @param myLogicalClock : The logical clock of our own Server.
   */
  public MessageAck(Message messageToAck, Channel channel, int myLogicalClock) {
    super(myLogicalClock, Message.TYPE_ACK, new String());
    if (messageToAck.getMessageType() != Message.TYPE_MESSAGE) {
      Engine.panic("Builder MessageAck incorrectly used");
    }
    byte[] contentAck = new byte[16];
    this.logicalClockAuthor = messageToAck.getLogicalClock();
    this.portNumberAuthor = channel.getServer().getPort();
    this.crc32 = ByteUtil.computeCRC32(messageToAck.getContent().getBytes());
    ByteUtil.writeInt32(contentAck, 0, this.logicalClockAuthor);
    ByteUtil.writeInt32(contentAck, 4, this.portNumberAuthor);
    ByteUtil.writeLong64(contentAck, 8, this.crc32);
    this.setContent(new String(contentAck));
  }

  /**
   * This builder is used to extract the data of a Message received, known to
   * be of type TYPE_ACK.
   * This method should be used only in Message.getMessageReceived(String).
   * @param message : The message of type TYPE_ACK received. 
   */
  MessageAck(Message message) {
    super(message.getLogicalClock(), Message.TYPE_ACK, message.getContent());
    if (message.getMessageType() != Message.TYPE_ACK) {
      Engine.panic("Builder MessageAck incorrectly used");
    }
    if (message.getContent().length() != 16) {
      Engine.panic("MessageAck of the wrong size. Here size="+message.getContent().length());
    }
    byte[] contentAck = message.getContent().getBytes();
    this.logicalClockAuthor = ByteUtil.readInt32(contentAck, 0);
    this.portNumberAuthor = ByteUtil.readInt32(contentAck, 4);
    this.crc32 = ByteUtil.readLong64(contentAck, 8);
  }
  
  public int getLogicalClockAuthor() {
    return logicalClockAuthor;
  }

  public int getPortNumberAuthor() {
    return portNumberAuthor;
  }

  public long getCrc32() {
    return crc32;
  }
}