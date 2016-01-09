package tom;

import java.util.Set;

import messages.engine.Channel;

public interface Peer {

	public void connect(int port);
	
	public void send(String content);

	public Set<Channel> getChannelGroup();
	
	public int getPort();
}
