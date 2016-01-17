package tom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import messages.callbacks.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import tom.messages.AckMessage;
import tom.messages.JoinMessage;
import tom.messages.JoinRequestMessage;
import tom.messages.JoinResponseMessage;
import tom.messages.Message;
import tom.messages.NewMemberMessage;
import tom.messages.WelcomeMessage;

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
  private HashSet<InGroupMessage> inGroupMessages = new HashSet<>();
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
      handleAck((AckMessage) message, from);
    } else if (message instanceof JoinMessage) {
      this.handleJoinMessage((JoinMessage) message, from);
    } else if (message instanceof JoinRequestMessage) {
      this.handleJoinRequest((JoinRequestMessage) message, from);
    } else if (message instanceof JoinResponseMessage) {
      this.handleJoinResponse((JoinResponseMessage) message, from);
    } else if (message instanceof NewMemberMessage) {
      this.handleNewMember(((NewMemberMessage) message).getNewMember());
    } else if (message instanceof WelcomeMessage) {
      this.handleWelcome((WelcomeMessage) message, from);
    } else if (message.getMessageType() == Message.MESSAGE) {
      handleMessage(message, from);
    } else {
      Engine.panic("Unknown message type");
    }
  }

  private void handleJoinMessage(JoinMessage message, InetSocketAddress from) {
    if (!peer.isInGroup()) {
      inGroupMessages.add(new InGroupMessage(message, from));
    }
    int logicalClock = peer.updateLogicalClock(message.getLogicalClock());
    AckMessage ourAck = new AckMessage(message, from, logicalClock);
    if (this.distantPeerManager.isWaiting(message.getNewMember())) {
      sendToGroup(ourAck);
    } else {
      this.pendingAcks.put(message.getNewMember(), ourAck);
    }
    this.updateWaitingMessages(message, from, logicalClock);
  }

  /**
   * This method is called when a message (of type Message or Join) is received.
   * This method add the message to the waiting messages. To do that, we look if
   * a waiting message already exist. If a waiting message already exist, that
   * means that we already received one of the ACK of this message and so we
   * mustn't create a new waiting message.
   * 
   * @param message
   * @param from
   * @param ackLogicalClock
   */
  private void updateWaitingMessages(Message message, InetSocketAddress from, int ackLogicalClock) {
    for (WaitingMessage waitingMessage : waitingMessages) {
      if (waitingMessage.getLogicalClock() == message.getLogicalClock() && waitingMessage.getAuthor().equals(from)) {
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
  private void handleMessage(Message message, InetSocketAddress from) {
    if (!peer.isInGroup()) {
      inGroupMessages.add(new InGroupMessage(message, from));
    }
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
  private void handleAck(AckMessage ack, InetSocketAddress from) {
    if (!peer.isInGroup()) {
      inGroupMessages.add(new InGroupMessage(ack, from));
    }
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
          InetSocketAddress newMember = joinMessage.getNewMember();
          this.distantPeerManager.removeWaitingMember(newMember);
          this.distantPeerManager.addMember(newMember);
          if (this.distantPeerManager.getMembersToIntroduce().contains(newMember)) {
            this.distantPeerManager.removeMemberToIntroduce(newMember);
            this.sendWelcome(waitingMessage.getLogicalClock(), newMember);            
          }
          this.sendMissingMessages(newMember);
          this.notifyNewMember(newMember);
        } else {
          return;
        }
      } else {
        waitingMessages.remove();
      }
      System.out.println("delivered " + waitingMessage.getContent() + " from " + waitingMessage.getAuthor().toString());
      if(waitingMessage.getContent().getMessageType() == Message.MESSAGE)
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
    deliverHeadIfNeeded();
  }

  public void checkAndUpdatePendingAcks(InetSocketAddress newMember) {
    AckMessage ack = this.pendingAcks.get(newMember);
    if (ack != null) {
      this.pendingAcks.remove(newMember);
      sendToGroup(ack);
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

  public void sendJoinRequest(InetSocketAddress address) {
    JoinRequestMessage request = new JoinRequestMessage();
    messenger.send(address, request.getFullMessage());
  }

  private void sendJoin(InetSocketAddress address) {
    int clock = peer.updateLogicalClock(0);
    JoinMessage message = new JoinMessage(clock, address);
    this.treatMyMessage(message);
  }

  private void handleJoinRequest(JoinRequestMessage message, InetSocketAddress from) {
    if (!peer.isInGroup()) {
      Engine.panic("can't receive an ACK when we aren't in a group.");
    }
    Set<InetSocketAddress> group = distantPeerManager.getGroup();
    distantPeerManager.introduce(from);
    JoinResponseMessage response = new JoinResponseMessage(group);
    messenger.send(from, response.getFullMessage());
    sendJoin(from);
  }

  private void handleJoinResponse(JoinResponseMessage message, InetSocketAddress from) {
    if (peer.isInGroup()) {
      Engine.panic("can't receive a JoinResponse when we already are in a group.");
    }
    ArrayList<InetSocketAddress> group = message.getGroup();
    for (InetSocketAddress member : group) {
      handleNewMember(member);
    }
  }

  private void notifyNewMember(InetSocketAddress newMember) {
    byte[] message = new NewMemberMessage(newMember).getFullMessage();
    for (InetSocketAddress member : distantPeerManager.getMembersToIntroduce()) {
      messenger.send(member, message);
    }
  }

  private void handleNewMember(InetSocketAddress newMember) {
    if (peer.isInGroup()) {
      Engine.panic("can't receive a NewMemberMessage when we already are in a group.");
    }
    try {
      messenger.connect(newMember.getAddress(), newMember.getPort());
    } catch (SecurityException | IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }

  }

  private void sendWelcome(int logicalClock, InetSocketAddress newMember) {
    WelcomeMessage welcome = new WelcomeMessage(logicalClock);
    messenger.send(newMember, welcome.getFullMessage());
  }

  private void handleWelcome(WelcomeMessage message, InetSocketAddress from) {
    if (peer.isInGroup()) {
      Engine.panic("can't receive a Welcome when we already are in a group.");
    }
    peer.setConnected(message.getLogicalClock());
    for (InGroupMessage inGroup : inGroupMessages) {
      delivered(inGroup.from, inGroup.message.getFullMessage());
    }
  }

  /**
   * This bean is used to store a message and its origin when we receive a
   * Message before to be in a group. This situation can happened just after the
   * delivery of a Join in a group. At this moment, the new member can sometimes
   * receive the retransmitted messages before to receive his welcome. So he has
   * to receive the welcome before to handle these messages.
   *
   */
  private class InGroupMessage {
    private Message message;
    private InetSocketAddress from;

    private InGroupMessage(Message message, InetSocketAddress from) {
      this.message = message;
      this.from = from;
    }
  }
}
