package messages.engine.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

import messages.engine.Server;

public class NioServer extends Server {

  private int port;
  private SelectionKey key;
  private SelectableChannel channel;
  
  public NioServer(int port, SelectableChannel channel, SelectionKey key) {
    this.port = port;
    this.key = key;
    this.channel = channel;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

}
