package messages.engine;

import java.util.ArrayList;
import java.util.List;

public class Peer implements AcceptCallback, ConnectCallback, DeliverCallback {

  private Engine engine;
  
  private List<Channel> peers = new ArrayList();
  private List<Server> connections = new ArrayList<Server>();
  
  public Peer(Engine engine) {
    super();
    this.engine = engine;
  }
  
  @Override
  public void deliver(Channel channel, byte[] bytes) {
    // TODO Auto-generated method stub

  }

  @Override
  public void connected(Channel channel) {
    this.peers.add(channel);

  }

  @Override
  public void accepted(Server server, Channel channel) {
    this.connections.add(server);
    this.peers.add(channel);
  }

  @Override
  public void closed(Channel channel) {
    // TODO Auto-generated method stub

  }
  

}
