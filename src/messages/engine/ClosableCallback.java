package messages.engine;

interface ClosableCallback {
  /**
   * Callback to notify that a channel has been closed.
   * 
   * @param channel
   */
  public void closed(Channel channel);

}
