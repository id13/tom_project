package messages.engine;

import java.io.IOException;

interface WriteCallback {

  /**
   * Handles the writing into an input such as a channel
   * 
   * @throws IOException
   */
  public abstract void handleWrite() throws IOException;

}
