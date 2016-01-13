package messages.callbacks;

import java.net.InetSocketAddress;

public interface ConnectCallback extends ClosableCallback {

  /**
   * Callback to notify that a connection has been created with a remote host
   * @param address identifies the remote host (by convention it is the address 
   * of a channel that accepts connections)
   */  
  public void connected(InetSocketAddress address);
  
}
