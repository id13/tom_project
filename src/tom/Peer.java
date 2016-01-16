package tom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Set;

public interface Peer {

  /**
   * Connect the Peer to a group.
   * 
   * @param address
   *          the address of one of the group members.
   * @throws IOException
   * @throws SecurityException
   * @throws UnknownHostException
   * @throws ConnectException
   */
  public void connect(InetSocketAddress address)
      throws UnknownHostException, SecurityException, IOException, ConnectException;

  /**
   * Send a String to the other members of the group.
   * 
   * @param content
   *          : the String to send.
   * @throws SendException
   */
  public void send(String content) throws SendException;

  /**
   * 
   * @return The instance of the class managing distant peers and channels.
   */
  public DistantPeerManager getDistantPeerManager();

  /**
   * 
   * @return the address that identifies me for distant peers.
   */
  public InetSocketAddress getMyAddress();

  /**
   * This method is used when we receive a message or a ACK to update the
   * logical clock of the peer.
   * 
   * @param outsideLogicalClock
   *          : the author's logical clock of the received messages.
   * @return the new logical clock.
   */
  int updateLogicalClock(int outsideLogicalClock);

  /**
   * This method is called when the peer receive the welcome message.
   * 
   * @param logicalClock
   */
  void setConnected(int logicalClock);

  /**
   * This method indicate if the peer is in a group or not. A peer is firstly in
   * a group containing only him. After a connect(), the peer isn't in a group
   * anymore since it is connecting to the new one. The peer state is back to
   * "in group" after the reception of a welcome message.
   * 
   * @return
   */
  public boolean isInGroup();
}
