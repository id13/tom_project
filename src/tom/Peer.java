package tom;

import java.net.InetSocketAddress;
import java.util.Set;

public interface Peer {

	public void connect(int port);
	
	public void send(String content);

	public Set<InetSocketAddress> getGroup();
	
	public InetSocketAddress getMyAddress();
	
	int updateLogicalClock(int outsideLogicalClock);
}
