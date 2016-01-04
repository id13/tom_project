package messages.engine;

public class NioServer extends Server {

  private int port;
  
  public NioServer(int port) {
    this.port = port;
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
