package messages.engine;

public class Peer implements AcceptCallback, ConnectCallback, DeliverCallback {

  @Override
  public void deliver(Channel channel, byte[] bytes) {
    // TODO Auto-generated method stub

  }

  @Override
  public void connected(Channel channel) {
    // TODO Auto-generated method stub

  }

  @Override
  public void accepted(Server server, Channel channel) {
    // TODO Auto-generated method stub

  }

  @Override
  public void closed(Channel channel) {
    // TODO Auto-generated method stub

  }
  

}
