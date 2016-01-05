package messages.engine.nio;

import java.io.IOException;
import java.nio.channels.NetworkChannel;
import java.nio.channels.SelectionKey;

import messages.engine.Server;

public class NioServer extends Server {

  private int port;
  private SelectionKey key;
  private NetworkChannel channel;
  
  public NioServer(int port, NetworkChannel channel, SelectionKey key) {
    this.port = port;
    this.key = key;
    this.channel = channel;
  }

  public SelectionKey getSelectionKey() {
    return this.key;
  }
  
  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public void close() throws IOException {
    NioEngine.getNioEngine().cancelKey(key);
    channel.close();
  }

}
