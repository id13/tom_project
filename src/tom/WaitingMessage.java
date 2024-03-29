package tom;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import messages.engine.Engine;
import messages.util.ByteUtil;
import tom.messages.AckMessage;
import tom.messages.Message;

/**
 * This class represent a Message waiting the acknowledgments of other peers of
 * the group to be delivered. It contains: The string representing the content
 * of the message, the logical clock of the message (of type TYPE_MESSAGE), the
 * (ip,port) of the message's author, and a set representing the (ip,port) from
 * which we already received the ACKS.
 *
 */
public class WaitingMessage implements Comparable<WaitingMessage> {

  private InetSocketAddress author;
  private Message content;
  private long crc;
  private int logicalClock;
  private int ackLogicalClock;
  private Set<InetSocketAddress> receivedAck = new HashSet<>();

  /**
   * @return the ackLogicalClock
   */
  public int getAckLogicalClock() {
    return ackLogicalClock;
  }

  /**
   * @param ackLogicalClock
   *          the ackLogicalClock to set
   */
  public void setAckLogicalClock(int ackLogicalClock) {
    this.ackLogicalClock = ackLogicalClock;
  }

  /**
   * Build a waiting message from a Message received. This method automatically
   * adds the address to the set.
   * 
   * @param message:
   *          a Message received of type TYPE_MESSAGE.
   * @param author:
   *          the InetSocketAddress from which this message has been received.
   */
  public WaitingMessage(Message message, InetSocketAddress author) {
    if (message.getMessageType() == Message.MESSAGE || message.getMessageType() == Message.JOIN) {
      this.content = message;
      this.logicalClock = message.getLogicalClock();
      this.author = author;
      this.crc = ByteUtil.computeCRC32(ByteUtil.writeString(content.getContent()));
    } else {
      Engine.panic("WaitingMessage build with a message not of" + "the type TYPE_MESSAGE");
    }
    receivedAck.add(author);
  }

  /**
   * Build a waiting message from a Message that we are sending.
   * 
   * @param message:
   *          A Message from us, of type TYPE_MESSAGE.
   * @param Peer:
   *          The peer sending the message.
   */
  public WaitingMessage(Message message, Peer peer) {
    if (message.getMessageType() == Message.MESSAGE || message.getMessageType() == Message.JOIN) {
      this.content = message;
      this.logicalClock = message.getLogicalClock();
      this.author = peer.getMyAddress();
      this.crc = ByteUtil.computeCRC32(ByteUtil.writeString(content.getContent()));
    } else {
      Engine.panic("WaitingMessage build neither with type MESSAGE or JOIN");
    }
  }

  /**
   * Build a waiting message from an ACK of a message which has not been
   * received yet.
   * 
   * @param ack
   * @param address
   *          The author of the ACK.
   */
  public WaitingMessage(AckMessage ack, InetSocketAddress address) {
    this.author = ack.getAuthor();
    this.logicalClock = ack.getLogicalClockAuthor();
    this.receivedAck.add(address);
    this.crc = ack.getCrc32();
  }

  /**
   * This method treat an ACK by verifying it and adding the address to the set
   * containing address having acknowledged the message.
   * 
   * @param address:
   *          The address from which the ACK has been sent.
   * @param ackMessage:
   *          The ACK received.
   */
  public void addAck(InetSocketAddress address, AckMessage ackMessage) {
    if (ackMessage.getCrc32() != crc || ackMessage.getLogicalClock() <= this.logicalClock) {
      Engine.panic("The ack doesn't correspond to the acked message or there is a problem with the logical clock : " + ackMessage);
    }
    if (receivedAck.contains(address)) {
      Engine.panic("This ack has already been received : " + ackMessage);
    }
    receivedAck.add(address);
  }

  /**
   * Add the message to the waiting message. This method assumes that the
   * waiting message correspond to the message. That means that the received ACK
   * must have acknowledged this message. This method adds the content and adds
   * the address to the set of ACK.
   * 
   * @param address
   * @param message
   */
  public void addMessage(InetSocketAddress address, Message message) {
    if (content != null || logicalClock != message.getLogicalClock() || !author.equals(address)) {
      Engine.panic("The message doesn't correspond to the waiting message : " + message);
    }
    if (ByteUtil.computeCRC32(ByteUtil.writeString(message.getContent())) != crc) {
      Engine.panic("wrong CRC : " + message);
    }
    this.content = message;
    this.receivedAck.add(address);
  }

  public Message getContent() {
    return content;
  }

  /**
   * 
   * @return the logical clock of the author of the message, at the time when he
   *         sent it.
   */
  public int getLogicalClock() {
    return logicalClock;
  }

  /**
   * We implements Comparable because we use in MessagesStock a PriorityQueue of
   * WaitingMessage. A message is lower than another if its logical clock is
   * lower. In that way, the WaitingMessage with the lower logical clock will be
   * the first element of the priority queue. If two messages have the same
   * logical clock, there are arbitrary ordered by comparison of the strings
   * representing the inetSocketAddress. So, as two messages can't have the same
   * author and the same logicalClock, two messages are always comparable.
   */
  @Override
  public int compareTo(WaitingMessage o) {
    if (this.logicalClock < o.logicalClock) {
      return -1;
    } else if (this.logicalClock > o.logicalClock) {
      return 1;
    } else {
      if (this.author.getPort() != o.author.getPort()) {
        Integer int1 = this.author.getPort();
        Integer int2 = o.author.getPort();
        return int1.compareTo(int2);
      } else {
        String ip1 = ByteUtil.readString(this.author.getAddress().getAddress());
        String ip2 = ByteUtil.readString(o.author.getAddress().getAddress());
        return ip1.compareTo(ip2);
      }
    }
  }

  public InetSocketAddress getAuthor() {
    return author;
  }

  public Set<InetSocketAddress> getReceivedAck() {
    return receivedAck;
  }

}
