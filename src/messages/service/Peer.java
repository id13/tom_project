package messages.service;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import messages.engine.AcceptCallback;
import messages.engine.Channel;
import messages.engine.ConnectCallback;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Server;

public class Peer implements AcceptCallback, ConnectCallback, DeliverCallback {

  private Engine engine;
  private int port;
  private List<Channel> peers = new ArrayList();
  private List<Server> connections = new ArrayList<Server>();
  private PrintStream out;
  
  public Peer(Engine engine, int port, PrintStream out) {
    super();
    this.port = port;
    this.engine = engine;
    this.out = out;
  }
  
  @Override
  public void deliver(Channel channel, byte[] bytes) {
    this.out.print(new String(bytes));
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
