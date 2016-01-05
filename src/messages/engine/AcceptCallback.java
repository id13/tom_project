package messages.engine;

public interface AcceptCallback extends ClosableCallback {
  /**
   * Callback to notify about an accepted connection.
   * @param server
   * @param channel
   */
  public void accepted(Server server, Channel channel);
  
}
