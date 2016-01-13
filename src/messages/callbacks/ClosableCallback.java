package messages.callbacks;

import java.net.InetSocketAddress;

public interface ClosableCallback {

  /**
   * Callback to notify that a connection has been closed
   * @param address identifies the remote host (by convention it is the address 
   * of a channel that accepts connections)
   */
  public void closed(InetSocketAddress address);
  
}
