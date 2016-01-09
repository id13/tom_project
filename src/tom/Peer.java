package tom;

import java.net.InetSocketAddress;
import java.util.Set;

public interface Peer {

	/**
	 * Connect the Manager of the Peer to the corresponding address. 
	 * @param address: The address to connect.
	 */
	public void connect(InetSocketAddress address);
	
	/**
	 * Send a String to the other members of the group.
	 * @param content : the String to send.
	 */
	public void send(String content);

	/**
	 * 
	 * @return The addresses of the other members of the group.
	 */
	public Set<InetSocketAddress> getGroup();
	
	/**
	 * 
	 * @return the address that identifies me for distant peers.
	 */
	public InetSocketAddress getMyAddress();
	
	/**
	 * This method is used when we receive a message or a ACK to update
	 * the logical clock of the peer.
	 * @param outsideLogicalClock : the author's logical clock of the just received messages.
	 * @return the new logical clock.
	 */
	int updateLogicalClock(int outsideLogicalClock);
}
