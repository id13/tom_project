package tom;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import messages.engine.Channel;

public class DistantPeerManager {

	private Set<Channel> group = new HashSet<>();
	private Map<Channel, InetSocketAddress> addresses = new HashMap<>();

	/**
	 * This method look the set of ACKs to assess if all peers of the group have
	 * acknowledged. To do that, it iterates over the channel of the group. If a
	 * channel isn't in the map, that means that we haven't receive a message from
	 * it, so we cannot have received its ACK. If the address associated to the
	 * channel isn't in the set of acks, that means that we didn't receive this
	 * ACK yet.
	 * 
	 * @param waitingMssage
	 *          : The message waiting its ACKs.
	 * @return True if all ACK have been received. False otherwise.
	 */
	public boolean allAckReceived(WaitingMessage waitingMessage) {
		Set<InetSocketAddress> acks = waitingMessage.getReceivedAck();
		for (Channel channel : group) {
			InetSocketAddress id = addresses.get(channel);
			if (id == null || !acks.contains(id)) {
				return false; // We would have add the Id if the ACK has been receive
			}
		}
		return true;
	}

	public void addId(Channel channel, InetSocketAddress address) {
		if (!addresses.containsKey(channel)) {
			addresses.put(channel, address);
		}
	}
	
	public void addChannel(Channel channel) {
		this.group.add(channel);
	}

}
