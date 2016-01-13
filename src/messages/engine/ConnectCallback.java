package messages.engine;

interface ConnectCallback extends ClosableCallback {

  /**
   * Callback to notify that a connection has succeeded.
   * 
   * @param channel
   */
  public void connected(Channel channel);
}
