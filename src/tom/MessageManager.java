package tom;

import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;

import messages.engine.Channel;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.util.ByteUtil;

public class MessageManager implements DeliverCallback {

	private Set<EarlyAck> ackReceivedBeforeTheirMessages = new HashSet<>();
	private PriorityQueue<WaitingMessage> waitingMessages = new PriorityQueue<>();
	private Peer peer;
	private TomDeliverCallback tomDeliverCallback;

	public MessageManager(Peer peer, TomDeliverCallback tomDeliverCallback) {
		this.peer = peer;
		this.tomDeliverCallback = tomDeliverCallback;
	}

	@Override
	public void deliver(Channel channel, byte[] bytes) {
		Message message = Message.getMessageReceived(bytes);
		if (message.getMessageType() == Message.TYPE_MESSAGE) {
			treatMessage(message, channel);
		} else if (message.getMessageType() == Message.TYPE_ACK) {
			treatAck((AckMessage) message, channel);
		} else {
			Engine.panic("Unknown message type");
		}
	}

	private void treatMessage(Message message, Channel channel) {
		int logicalClock = peer.updateLogicalClock(message.getLogicalClock());
		AckMessage ourAck = new AckMessage(message, channel, logicalClock);
		peer.send(ByteUtil.readString(ourAck.getFullMessage()));
		WaitingMessage waitingMessage = new WaitingMessage(message, channel);
		// We will perhaps remove elements from the Set so we have to use an
		// Iterator like that:
		for (Iterator<EarlyAck> it = ackReceivedBeforeTheirMessages.iterator(); it.hasNext();) {
			EarlyAck earlyAck = it.next();
			AckMessage ack = earlyAck.getAck();
			if (ack.getLogicalClockAuthor() == message.getLogicalClock()
			    && ack.getPortNumberAuthor() == channel.getServer().getPort()) {
				waitingMessage.addAck(earlyAck.getChannel(), ack);
				it.remove();
			}
		}
		waitingMessages.add(waitingMessage);
		deliverHeadIfNeeded();
	}

	private void treatAck(AckMessage ack, Channel channel) {
		peer.updateLogicalClock(ack.getLogicalClock());
		boolean foundInQueue = false;
		for (WaitingMessage waitingMessage : waitingMessages) {
			if (waitingMessage.getLogicalClock() == ack.getLogicalClockAuthor()
			    && waitingMessage.getAuthorPortNumber() == ack.getPortNumberAuthor()) {
				waitingMessage.addAck(channel, ack);
				foundInQueue = true;
				deliverHeadIfNeeded();
				break;
			}
		}
		if (!foundInQueue) {
			ackReceivedBeforeTheirMessages.add(new EarlyAck(ack, channel));
		}
	}

	private void deliverHeadIfNeeded() {
		WaitingMessage waitingMessage = waitingMessages.peek();
		while (waitingMessage != null && waitingMessage.isReadyToDeliver(peer.getChannelGroup())) {
			waitingMessages.remove();
			tomDeliverCallback.deliver(waitingMessage.getContent());
			waitingMessage = waitingMessages.peek();
		}
	}
	
	public void treatMyMessage(Message message) {
		WaitingMessage waitingMessage = new WaitingMessage(message, peer.getPort());
		waitingMessages.add(waitingMessage);
		// Only for the situation where the group size is 1:
		deliverHeadIfNeeded();
	}

	private class EarlyAck {
		AckMessage ack;
		Channel channel;

		public EarlyAck(AckMessage ack, Channel channel) {
			this.ack = ack;
			this.channel = channel;
		}

		public AckMessage getAck() {
			return ack;
		}

		public Channel getChannel() {
			return channel;
		}
	}
}
