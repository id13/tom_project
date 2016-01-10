package tom;

import java.net.InetSocketAddress;

public interface TomDeliverCallback {

	public void deliver(InetSocketAddress from, String message);

}
