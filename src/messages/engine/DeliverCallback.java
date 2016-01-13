package messages.engine;

import java.nio.ByteBuffer;

interface DeliverCallback {

  /**
   * Callback to notify that a message has been delivered. The message is whole,
   * all bytes have been accumulated.
   * 
   * @param channel
   * @param bytes
   */
  public void deliver(Channel channel, byte[] bytes);
}
