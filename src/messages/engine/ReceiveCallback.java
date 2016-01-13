package messages.engine;

import java.io.IOException;

interface ReceiveCallback {

  /**
   * Callback to handle processing of a readable input such as a channel
   * 
   * @throws IOException
   */
  public abstract void handleReceive() throws IOException;

}
