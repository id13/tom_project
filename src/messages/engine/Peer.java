package messages.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Peer implements AcceptCallback, ConnectCallback, DeliverCallback {

  private Engine engine;
  private int port;
  private List<Channel> peers = new ArrayList();
  private List<Server> connections = new ArrayList<Server>();
  
  public Peer(Engine engine, int port) {
    super();
    this.port = port;
    this.engine = engine;
  }
  
  @Override
  public void deliver(Channel channel, byte[] bytes) {
    
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

  }
  
  public void send(String message) {
    byte[] bytes = message.getBytes();
    for(Channel channel : peers) {
      try {
        channel.send(bytes, 0, bytes.length);
      } catch (IOException e) {
        e.printStackTrace();
        Engine.panic(e.getMessage());
      }
    }
  }
  
  public void connect(String hostname, int port) {
    InetAddress address;
    try {
      address = InetAddress.getByName(hostname);
      this.engine.connect(address, port, this);
    } catch (SecurityException | IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }
  
  public void accept() {
    try {
      this.engine.listen(port, this);
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }
  

}
