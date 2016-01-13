package messages.callbacks;

import java.net.InetSocketAddress;

public interface DeliverCallback {

  /**
   * Callback to notify the delivering of a message from a remote host
   * @param from the address of the remote host from which the message is
   * @param content the data contained in the message
   */
  public void delivered(InetSocketAddress from, byte[] content);
  
}
