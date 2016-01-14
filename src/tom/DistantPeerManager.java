package tom;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import messages.engine.Channel;

public class DistantPeerManager {

  private Set<InetSocketAddress> group = new HashSet<>();

  /**
   * This method looks the set of ACKs to assess if all peers of the group have
   * acknowledged.
   * 
   * @param waitingMssage
   *          : The message waiting its ACKs.
   * @return True if all ACK have been received. False otherwise.
   */
  public boolean allAckReceived(WaitingMessage waitingMessage) {
    Set<InetSocketAddress> acks = waitingMessage.getReceivedAck();
    for (InetSocketAddress member : group) {
      if (!acks.contains(member)) {
        return false;
      }
    }
    return true;
  }

  public void addMember(InetSocketAddress member) {
    group.add(member);
  }

  public void removeMember(InetSocketAddress member) {
    group.remove(member);
  }
}
