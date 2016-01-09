package messages.engine;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import messages.engine.nio.NioServer;

public class Messenger implements AcceptCallback, ConnectCallback, DeliverCallback {

  private Engine engine;
  private int port;
  private Map<Server, Channel> channels = new HashMap<Server, Channel>();
  private Server acceptServer;
  private DeliverCallback deliverCallback;
  private ConnectCallback connectCallback;
  
  public Messenger(Engine engine, int port) {
    super();
    this.port = port;
    this.engine = engine;
  }
  
  public void setDeliverCallback(DeliverCallback callback) {
    this.deliverCallback = callback;
  }
  
  @Override
  public synchronized void deliver(Channel channel, byte[] bytes) {
    this.deliverCallback.deliver(channel, bytes);
  }

  @Override
  public void connected(Channel channel) {
    this.channels.put(channel.getServer(), channel);
    if (this.connectCallback != null) {
    	this.connectCallback.connected(channel);
    }
  }

  @Override
  public void accepted(Server server, Channel channel) {
    channel.setServer(server);
    this.channels.put(server, channel);
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
  
  public synchronized void broadcast(byte[] bytes) {
    for(Entry<Server, Channel> channel : channels.entrySet()) {
      try {
        channel.getValue().send(bytes, 0, bytes.length);
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
  

  public void closeAllConnections() {
    this.stopAccept();
    for(Entry<Server, Channel> channel : channels.entrySet()) {
      channel.getValue().close();
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
  
  public void send(Server server, byte[] bytes) {
    Channel channel = channels.get(server);
    if(channel == null)
      Engine.panic("send: the specified server was not found");
    try {
      channel.send(bytes, 0, bytes.length);
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }
  
  public void runBurstBroadcastThread(String message) {
    Messenger messenger = this;
    Runnable broadcast = new Runnable() {
      @Override
      public void run() {
        for(;;) {
          messenger.broadcast((message + " broadcasted from " + messenger.getAcceptPort()).getBytes());
        }
      }
      
    };
    Thread broadcastThread = new Thread(broadcast, "broadcastThread");
    broadcastThread.start();
  }

	public void setConnectCallback(ConnectCallback connectCallback) {
		this.connectCallback = connectCallback;
	}
  
}
