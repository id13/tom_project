package tom;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import messages.engine.Channel;
import messages.engine.Engine;

public class MessagesStock {

  private Set<Message> ackReceivedBeforeTheirMessages = new HashSet<>();
  private PriorityQueue<WaitingMessage> waitingMessages = new PriorityQueue<>();
  private Peer peer;
  
  public MessagesStock(Peer peer) {
    this.peer = peer;
  }

  public void ManageReceivedMessage(Message message, Channel channel) {
    if (message.getMessageType() == Message.TYPE_MESSAGE) {
      waitingMessages.add(new WaitingMessage(message, channel));
    } else if (message.getMessageType() == Message.TYPE_ACK) {
      // TODO
    } else {
      Engine.panic("Unknown message type");
    }
  }
}
