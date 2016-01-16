package tom;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import messages.engine.Channel;
import messages.engine.Engine;

public class DistantPeerManager {

  private Set<InetSocketAddress> group = new HashSet<>();
  private Set<InetSocketAddress> membersToIntroduce = new HashSet<>();
  private Set<InetSocketAddress> waitingMembers = new HashSet<>();
  

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
  
  /**
   * @return the waitingMembers
   */
  public Set<InetSocketAddress> getWaitingMembers() {
    return waitingMembers;
  }
  
  public void addWaitingMember(InetSocketAddress member) {
    this.waitingMembers.add(member);
  }
  
  public void removeWaitingMember(InetSocketAddress member) {
    this.waitingMembers.remove(member);
  }
  
  /**
   * This method move a member from waitingMembers to membersToIntroduce.
   * @param member
   */
  public void introduce(InetSocketAddress member) {
    if (!waitingMembers.contains(member)) {
      Engine.panic("We received a JoinRequest from someone who wasn't in the waitingMember set");
    }
    this.waitingMembers.remove(member);
    this.membersToIntroduce.add(member);
  }
  
  public void removeMemberToIntroduce(InetSocketAddress member) {
    this.membersToIntroduce.remove(member);
  }
  
  public Set<InetSocketAddress> getMembersToIntroduce() {
    return this.membersToIntroduce;
  }

  public void addMember(InetSocketAddress member) {
    group.add(member);
  }

  public void removeMember(InetSocketAddress member) {
    if(this.group.contains(member)) {
      this.group.remove(member);
    } else if(this.membersToIntroduce.contains(member)) {
      this.removeMemberToIntroduce(member);
    } else if(this.waitingMembers.contains(member)) {
      this.removeWaitingMember(member);
    } else {
      Engine.panic("the following member " + member.toString() + " is not managed");
    }
  }

  public Set<InetSocketAddress> getGroup() {
    return group;
  }
  
  public boolean isWaiting(InetSocketAddress member) {
    return this.waitingMembers.contains(member);
  }
  
}
