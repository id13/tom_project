package tom;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import messages.engine.Engine;
import messages.util.ByteUtil;

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
	private String content;
	private int logicalClock;
	private Set<InetSocketAddress> receivedAck = new HashSet<>();

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
		if (message.getMessageType() == Message.TYPE_MESSAGE) {
			this.content = message.getContent();
			this.logicalClock = message.getLogicalClock();
			this.author = author;
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
		if (message.getMessageType() == Message.TYPE_MESSAGE) {
			this.content = message.getContent();
			this.logicalClock = message.getLogicalClock();
			this.author = peer.getMyAddress();
		} else {
			Engine.panic("WaitingMessage build with a message not of" + "the type TYPE_MESSAGE");
		}
	}

	/**
	 * This method treat an ACK by verifying it and adding the address to the
	 * set containing address having acknowledged the message.
	 * 
	 * @param address:
	 *          The address from which the ACK has been sent.
	 * @param ackMessage:
	 *          The ACK received.
	 */
	public void addAck(InetSocketAddress address, AckMessage ackMessage) {
		if (ackMessage.getCrc32() != ByteUtil.computeCRC32(ByteUtil.writeString(content))
		    || ackMessage.getLogicalClock() <= this.logicalClock) {
			Engine
			    .panic("The ack doesn't correspond to the acked message" + " or there is a problem with the logical clock.");
		}
		if (receivedAck.contains(address)) {
			Engine.panic("This ack has already been received");
		}
		receivedAck.add(address);
	}

	public String getContent() {
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
			return this.author.toString().compareTo(o.author.toString());
		}
	}

	public InetSocketAddress getAuthor() {
		return author;
	}

	public Set<InetSocketAddress> getReceivedAck() {
		return receivedAck;
	}
	
}
