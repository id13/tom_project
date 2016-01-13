package messages.callbacks;

import java.net.InetSocketAddress;

public interface AcceptCallback extends ClosableCallback {

  /**
   * Callback to notify that a connection with a remote host has been accepted
   * @param address identifies the remote host (by convention it is the address 
   * of a channel that accepts connections)
   */
  public void accepted(InetSocketAddress address);
  
}
