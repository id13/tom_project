package messages.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

public class NioEngine extends Engine {

  private static NioEngine nioEngine = new NioEngine();
  
  public static NioEngine getNioEngine() {
    return nioEngine;
  }
  
  
  public NioEngine() {
  }

  @Override
  public void mainloop() {
    
  }

  @Override
  public Server listen(int port, AcceptCallback callback) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void connect(InetAddress hostAddress, int port, ConnectCallback callback)
      throws UnknownHostException, SecurityException, IOException {
    // TODO Auto-generated method stub

  }

}
