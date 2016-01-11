package tom;

import java.net.InetSocketAddress;
import java.util.PriorityQueue;

import messages.engine.Channel;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;

/**
 * This class is used to manage the messages in order to wait ACKs before to
 * deliver. This class uses a Queue to sort the messages (of type MESSAGE)
 * received with their ACKs. It uses a Set to store the ACKs received before the
 * messages there are acknowledging.
 *
 */
public class MessageManager implements DeliverCallback {

	private PriorityQueue<WaitingMessage> waitingMessages = new PriorityQueue<>();
	private final Peer peer;
	private final TomDeliverCallback tomDeliverCallback;
	private final Messenger messenger;
	private final DistantPeerManager distantPeerManager;

	public MessageManager(Peer peer, TomDeliverCallback tomDeliverCallback, Messenger messenger, DistantPeerManager distantPeerManager) {
		this.peer = peer;
		this.tomDeliverCallback = tomDeliverCallback;
		this.messenger = messenger;
		this.distantPeerManager = distantPeerManager;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see messages.engine.DeliverCallback#deliver(messages.engine.Channel,
	 * byte[]) This is the deliver of the Message layer. It is completely
	 * different from the TOM layer's deliver.
	 */
	@Override
	public void deliver(Channel channel, byte[] bytes) {
		Message message = Message.getMessageReceived(bytes);
		InetSocketAddress address = message.getAuthor();
		distantPeerManager.addId(channel, address);
		System.out.println("Received message (not delivered) from " + address + " : " + message);

		if (message.getMessageType() == Message.MESSAGE) {
			treatMessage(message, address);
		} else if (message.getMessageType() == Message.ACK) {
			treatAck((AckMessage) message, address);
		} else {
			Engine.panic("Unknown message type");
		}
	}

	/**
	 * This method treats a received message of type TYPE_MESSAGE. So, it adds it
	 * looks into the Set of earlyAck to look for a ACK corresponding to this
	 * message. Next, it creates the corresponding waitingMessage, and add it to
	 * the Set. At the end, we look if the first message of the queue can be
	 * deliver.
	 * 
	 * @param message
	 *          : the received message of type TYPE_MESSAGE.
	 * @param address
	 *          : the address from which the message has been sent.
	 */
	private void treatMessage(Message message, InetSocketAddress address) {
		int logicalClock = peer.updateLogicalClock(message.getLogicalClock());
		AckMessage ourAck = new AckMessage(message, address, logicalClock, peer.getMyAddress());
		if (messenger != null) { // Useful in JUnitTest
			messenger.broadcast(ourAck.getFullMessage());
		}
		for (WaitingMessage waitingMessage : waitingMessages) {
			if (waitingMessage.getLogicalClock() == message.getLogicalClock()
			    && waitingMessage.getAuthor().equals(address)) {
				waitingMessage.addMessage(address, message);
				deliverHeadIfNeeded();
				return;
			}
		}		
		WaitingMessage waitingMessage = new WaitingMessage(message, address);
		waitingMessages.add(waitingMessage);
		deliverHeadIfNeeded(); // Useful for a group of 2 peers.
	}

	/**
	 * This method treats a received AckMessage. So, it looks if the corresponding
	 * Message of type TYPE_MESSAGE has already been received. Depending on that,
	 * the ACK is added to the waitingMessages or to the earlyAcks. At the end, we
	 * look if the first message of the queue can be deliver.
	 * 
	 * @param ack
	 *          : The AckMessage received.
	 * @param address
	 *          : The address from which the ACK has been sent.
	 */
	private void treatAck(AckMessage ack, InetSocketAddress address) {
		peer.updateLogicalClock(ack.getLogicalClock());
		boolean foundInQueue = false;
		for (WaitingMessage waitingMessage : waitingMessages) {
			if (waitingMessage.getLogicalClock() == ack.getLogicalClockAuthor()
			    && waitingMessage.getAuthor().equals(ack.getAuthorOfAckedMessage())) {
				waitingMessage.addAck(address, ack);
				foundInQueue = true;
				deliverHeadIfNeeded();
				break;
			}
		}
		if (!foundInQueue) {
			WaitingMessage waitingMessage = new WaitingMessage(ack, ack.getAuthor());
			waitingMessages.add(waitingMessage);
		}
	}

	/**
	 * This method looks if a message can be deliver at the TOM layer. So, this
	 * method looks if the message with the lowest logical clock can be delivered.
	 * If it can, this method delivers it and looks if the next one can be
	 * delivered.
	 */
	private void deliverHeadIfNeeded() {
		WaitingMessage waitingMessage = waitingMessages.peek();
		while (waitingMessage != null && distantPeerManager.allAckReceived(waitingMessage)) {
			waitingMessages.remove();
			System.out.println("delivered " + waitingMessage.getContent() + " from " + waitingMessage.getAuthor().toString());
			tomDeliverCallback.deliver(waitingMessage.getAuthor(), waitingMessage.getContent());
			waitingMessage = waitingMessages.peek();
		}
	}

	/**
	 * This method is used when we send a message using Peer.send(). It will add
	 * the message to the Queue of waiting messages in order to deliver it when
	 * all the ACKs will be received.
	 * 
	 * @param message:
	 *          The message of type TYPE_MESSAGE that we are sending.
	 */
	public void treatMyMessage(Message message) {
		WaitingMessage waitingMessage = new WaitingMessage(message, peer);
		waitingMessages.add(waitingMessage);
		// Only for the situation where the group size is 1:
		deliverHeadIfNeeded();
	}
}
