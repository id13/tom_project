package tom;

import java.util.HashSet;
import java.util.Set;

import messages.engine.Channel;
import messages.engine.Engine;
import messages.util.ByteUtil;

/**
 * This class represent a Message waiting the acknowledgements of other peers of
 * the group to be delivered. It contains: The string representing the content
 * of the message. The logical clock of the message (the message of type
 * TYPE_MESSAGE) A set representing the channels from which we already received
 * the ACKS.
 *
 */
public class WaitingMessage implements Comparable<WaitingMessage> {

  private int authorPortNumber;
  private String content;
  private int logicalClock;
  private Set<Channel> channelsHavingSentAck = new HashSet<>();

  /**
   * Build a waiting message from a Message received. This method automatically
   * add the channel to the set.
   * 
   * @param message:
   *          a Message received of type TYPE_MESSAGE.
   * @param channel:
   *          the channel from which this message has been received.
   */
  public WaitingMessage(Message message, Channel channel) {
    if (message.getMessageType() == Message.TYPE_MESSAGE) {
      this.content = message.getContent();
      this.logicalClock = message.getLogicalClock();
      this.authorPortNumber = channel.getServer().getPort();
    } else {
      Engine.panic("WaitingMessage build with a message not of" + "the type TYPE_MESSAGE");
    }
    channelsHavingSentAck.add(channel);
  }

  /**
   * Build a waiting message from a Message that we are sending.
   * 
   * @param message:
   *          A Message from us, of type TYPE_MESSAGE.
   * @param myPortNumber:
   *          The port number of our socket.
   */
  public WaitingMessage(Message message, int myPortNumber) {
    this.content = message.getContent();
    this.logicalClock = message.getLogicalClock();
    this.authorPortNumber = myPortNumber;
  }

  /**
   * This method treat an ACK by verifying it and adding the channel to the set.
   * 
   * @param channel:
   *          The channel from which the ACK has been received.
   * @param message:
   *          The ACK received.
   */
  public void addAck(Channel channel, MessageAck messageAck) {
    if (messageAck.getCrc32() != ByteUtil.computeCRC32(content.getBytes())
        || messageAck.getLogicalClock() <= this.logicalClock) {
      Engine.panic("The ack doesn't correspond to the acked message"
        + " or there is a problem with the logical clock.");
    }
    if (channelsHavingSentAck.contains(channel)) {
      Engine.panic("This ack has already been received");
    }
    channelsHavingSentAck.add(channel);
  }

  /**
   * Ask to the WaitingMessage if all the ACK have been received.
   * 
   * @param channelsOfGroup:
   *          A set containing all the channel from which we must receive ACK.
   * @return
   */
  public boolean isReadyToDeliver(Set<Channel> channelsOfGroup) {
    // Not != because a channel could have left.
    if (channelsHavingSentAck.size() < channelsOfGroup.size()) {
      return false;
    } else {
      for (Channel channel : channelsOfGroup) {
        if (!channelsHavingSentAck.contains(channel)) {
          return false;
        }
      }
      return true;
    }
  }

  public String getContent() {
    return content;
  }

  public int getLogicalClock() {
    return logicalClock;
  }

  /**
   * We implements Comparable because We use in MessagesStock a PriorityQueue of
   * WaitingMessage. A message is lower than another if its logical clock is
   * lower. In that way, the WaitingMessage with the lower logical clock will be
   * the first element of the priority queue. If two messages have the same
   * logical clock, there are arbitrary ordered by the authorPortNumber. So, as
   * two messages can't have the same authorPortNumber and the same
   * logicalClock, two messages are always comparable.
   */
  @Override
  public int compareTo(WaitingMessage o) {
    if (this.logicalClock < o.logicalClock) {
      return -1;
    } else if (this.logicalClock > o.logicalClock) {
      return 1;
    } else {
      return this.authorPortNumber - o.authorPortNumber;
    }
  }
}
