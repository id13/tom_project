package tom;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import messages.callbacks.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import tom.messages.AckMessage;
import tom.messages.JoinMessage;
import tom.messages.Message;

/**
 * This class is used to manage the messages in order to wait ACKs before to
 * deliver. This class uses a Queue to sort the messages (of type MESSAGE)
 * received with their ACKs. It uses a Set to store the ACKs received before the
 * messages there are acknowledging.
 *
 */
public class MessageManager implements DeliverCallback {

  private PriorityQueue<WaitingMessage> waitingMessages = new PriorityQueue<>();
  private Map<InetSocketAddress, AckMessage> pendingAcks = new HashMap<>();
  private final Peer peer;
  private final TomDeliverCallback tomDeliverCallback;
  private final Messenger messenger;
  private final DistantPeerManager distantPeerManager;

  public MessageManager(Peer peer, TomDeliverCallback tomDeliverCallback, Messenger messenger,
      DistantPeerManager distantPeerManager) {
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
  public void delivered(InetSocketAddress from, byte[] content) {
    Message message = Message.getMessageReceived(content);
    System.out.println("Received message (not delivered) from " + from + " : " + message);
    if (message instanceof AckMessage) {
      treatAck((AckMessage) message, from);
    } else if (message instanceof JoinMessage) {
      this.handleJoinMessage((JoinMessage) message, from);
    } else if (message.getMessageType() == Message.MESSAGE) {
      treatMessage(message, from);
    } else {
      Engine.panic("Unknown message type");
    }
  }

  private void handleJoinMessage(JoinMessage message, InetSocketAddress from) {
    int logicalClock = peer.updateLogicalClock(message.getLogicalClock());
    AckMessage ourAck = new AckMessage(message, from, logicalClock);
    if (this.distantPeerManager.isWaiting(message.getNewMember())) {
      sendToGroup(ourAck);
    } else {
      this.pendingAcks.put(message.getNewMember(), ourAck);
    }
    this.updateWaitingMessages(message, from, logicalClock);
  }

  private void updateWaitingMessages(Message message, InetSocketAddress from, int ackLogicalClock) {
    for (WaitingMessage waitingMessage : waitingMessages) {
      if (waitingMessage.getLogicalClock() == message.getLogicalClock() 
          && waitingMessage.getAuthor().equals(from) 
          && waitingMessage.getContent() == null) {
        waitingMessage.addMessage(from, message);
        waitingMessage.setAckLogicalClock(ackLogicalClock);
        deliverHeadIfNeeded();
        return;
      }
    }
    WaitingMessage waitingMessage = new WaitingMessage(message, from);
    waitingMessage.setAckLogicalClock(ackLogicalClock);
    waitingMessages.add(waitingMessage);
    deliverHeadIfNeeded(); // Useful for a group of 2 peers.
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
   * @param from
   *          : the address from which the message has been sent.
   */
  private void treatMessage(Message message, InetSocketAddress from) {
    int logicalClock = peer.updateLogicalClock(message.getLogicalClock());
    AckMessage ourAck = new AckMessage(message, from, logicalClock);
    if (messenger != null) { // Useful for JUnit
      sendToGroup(ourAck);
    }
    this.updateWaitingMessages(message, from, logicalClock);
  }

  /**
   * This method treats a received AckMessage. So, it looks if the corresponding
   * Message of type TYPE_MESSAGE has already been received. Depending on that,
   * the ACK is added to the waitingMessages or to the earlyAcks. At the end, we
   * look if the first message of the queue can be deliver.
   * 
   * @param ack
   *          : The AckMessage received.
   * @param from
   *          : The address from which the ACK has been sent.
   */
  private void treatAck(AckMessage ack, InetSocketAddress from) {
    peer.updateLogicalClock(ack.getLogicalClock());
    boolean foundInQueue = false;
    for (WaitingMessage waitingMessage : waitingMessages) {
      if (waitingMessage.getLogicalClock() == ack.getLogicalClockAuthor()
          && waitingMessage.getAuthor().equals(ack.getAuthor())) {
        waitingMessage.addAck(from, ack);
        foundInQueue = true;
        deliverHeadIfNeeded();
        break;
      }
    }
    if (!foundInQueue) {
      WaitingMessage waitingMessage = new WaitingMessage(ack, from);
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
      if (waitingMessage.getContent() instanceof JoinMessage) {
        JoinMessage joinMessage = (JoinMessage) waitingMessage.getContent();
        AckMessage ack = this.pendingAcks.get(joinMessage.getNewMember());
        if (ack == null) {
          waitingMessages.remove();
          this.distantPeerManager.removeWaitingMember(joinMessage.getNewMember());
          this.distantPeerManager.addMember(joinMessage.getNewMember());
          this.sendMissingMessages(joinMessage.getNewMember());
        } else {
          return;
        }
      } else {
        waitingMessages.remove();
      }
      System.out.println("delivered " + waitingMessage.getContent() + " from " + waitingMessage.getAuthor().toString());
      tomDeliverCallback.deliver(waitingMessage.getAuthor(), waitingMessage.getContent().getContent());
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
    sendToGroup(message);
    // Only for the situation where the group size is 1:
    deliverHeadIfNeeded();
  }

  public void sendToGroup(Message message) {
    Set<InetSocketAddress> group = distantPeerManager.getGroup();
    for (InetSocketAddress member : group) {
      messenger.send(member, message.getFullMessage());
    }
  }

  public void checkAndUpdatePendingAcks(InetSocketAddress newMember) {
    AckMessage ack = this.pendingAcks.get(newMember);
    if (ack != null) {
      this.pendingAcks.remove(newMember);
      this.deliverHeadIfNeeded();
    }
  }

  public void sendMissingMessages(InetSocketAddress newMember) {
    for (WaitingMessage waitingMessage : this.waitingMessages) {
      Message message = waitingMessage.getContent();
      InetSocketAddress author = waitingMessage.getAuthor();
      if (author.equals(this.peer.getMyAddress())) {
        this.messenger.send(newMember, message.getFullMessage());
      } else {
        AckMessage ack = new AckMessage(message, author, waitingMessage.getAckLogicalClock());
        this.messenger.send(newMember, ack.getFullMessage());
      }
    }
  }

  public void sendJoin(InetSocketAddress address) {
    int clock = peer.updateLogicalClock(0);
    JoinMessage message = new JoinMessage(clock, address);
    this.sendToGroup(message);
  }

}
