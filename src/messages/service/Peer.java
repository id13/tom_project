package messages.service;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import messages.engine.AcceptCallback;
import messages.engine.Channel;
import messages.engine.ConnectCallback;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Server;
import messages.engine.nio.NioServer;

public class Peer implements AcceptCallback, ConnectCallback, DeliverCallback {

  private Engine engine;
  private int port;
  private List<Channel> channels = new ArrayList();
  private PrintStream out;
  private Server acceptServer;
  
  public Peer(Engine engine, int port, PrintStream out) {
    super();
    this.port = port;
    this.engine = engine;
    this.out = out;
  }
  
  @Override
  public synchronized void deliver(Channel channel, byte[] bytes) {
    out.print("message delivered : " + new String(bytes) + "\n");
  }

  @Override
  public void connected(Channel channel) {
    this.channels.add(channel);
  }

  @Override
  public void accepted(Server server, Channel channel) {
    channel.setServer(server);
    this.channels.add(channel);
  }

  @Override
  public synchronized void closed(Channel channel) {
    try {
      System.err.println("channel with address " + channel.getRemoteAddress().toString() + " closed");
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
    this.channels.remove(channel);
  }
  
  public synchronized void send(String message) {
    byte[] bytes = message.getBytes();
    for(Channel channel : channels) {
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
      address = InetAddress.getByName (hostname);
      this.engine.connect(address, port, this);
    } catch (SecurityException | IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }
  
  public void stopAccept() {
    try {
      this.acceptServer.close();
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }
  
  /**
   * Disconnect the Peer from the peers group
   * Since we are working on a multi-cast basis, it does not make sense to close
   * one specific connection
   */
  public void closeAllConnections() {
    this.stopAccept();
    for(Channel channel : channels) {
      channel.close();
    }
  }
  
  public void accept() {
    try {
      Server server = this.engine.listen(port, this);
      this.acceptServer = server;
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }
  
  public int getAcceptPort() {
    return this.acceptServer.getPort();
  }
  
  public void runBroadcastThread(String message) {
    Peer peer = this;
    Runnable broadcast = new Runnable() {
      @Override
      public void run() {
        for(;;) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
          }
          peer.send(message + " broadcasted from " + peer.getAcceptPort());
        }
      }
      
    };
    Thread broadcastThread = new Thread(broadcast, "broadcastThread");
    broadcastThread.start();
  }
  
}
