package messages.engine;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Messenger implements AcceptCallback, ConnectCallback, DeliverCallback {

  private Engine engine;
  private int port;
  private HashMap<InetSocketAddress, Channel> channels = new HashMap<InetSocketAddress, Channel>();
  private Server acceptServer;
  private messages.callbacks.DeliverCallback deliverCallback;
  private messages.callbacks.ConnectCallback connectCallback;
  private messages.callbacks.AcceptCallback acceptCallback;
  private messages.callbacks.ClosableCallback closableCallback;
  private InetSocketAddress acceptAddress;
  
  public Messenger(Engine engine, int port) {
    super();
    this.port = port;
    this.engine = engine;
  }

  public void setDeliverCallback(messages.callbacks.DeliverCallback callback) {
    this.deliverCallback = callback;
  }

  @Override
  public synchronized void deliver(Channel channel, byte[] bytes) {
    try {
      this.deliverCallback.delivered(channel.getRemoteAcceptingAddress(), bytes);
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }

  @Override
  public void connected(Channel channel) {
    try {
      this.channels.put(channel.getRemoteAcceptingAddress(), channel);
      if (this.connectCallback != null) {
        this.connectCallback.connected(channel.getRemoteAcceptingAddress());
      }
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }

  }

  @Override
  public void accepted(Server server, Channel channel) {
    try {
      this.channels.put(channel.getRemoteAcceptingAddress(), channel);
      if (this.acceptCallback != null) {
        this.acceptCallback.accepted(channel.getRemoteAcceptingAddress());
      }
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }

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
    try {
      this.closableCallback.closed(channel.getRemoteAcceptingAddress());
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }
  
  public InetSocketAddress getAcceptAddress() throws UnknownHostException {
    if(this.acceptAddress == null)
      this.acceptAddress = new InetSocketAddress(Inet4Address.getLocalHost(), this.port);
    return this.acceptAddress;
  }

  public synchronized void broadcast(byte[] bytes) {
    for (Entry<InetSocketAddress, Channel> channel : channels.entrySet()) {
      try {
        channel.getValue().send(bytes, 0, bytes.length);
      } catch (IOException e) {
        e.printStackTrace();
        Engine.panic(e.getMessage());
      }
    }
  }

  public void connect(InetAddress address, int port) throws UnknownHostException, SecurityException, IOException {
      this.engine.connect(address, port, this);
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
    for (Entry<InetSocketAddress, Channel> channel : channels.entrySet()) {
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

  /**
   * sends message composed of bytes through a channel
   * @param channel
   * @param bytes
   * @deprecated this method is here only for legacy purpose
   */
  public void send(Channel channel, byte[] bytes) {
    if (channel == null)
      Engine.panic("send: the specified server was not found");
    try {
      channel.send(bytes, 0, bytes.length);
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }

  public void send(InetSocketAddress dest, byte[] bytes) {
    Channel channel = this.channels.get(dest);
    if(channel == null) 
      Engine.panic("Messenger.send() : destination " + dest.toString() + " not found in the messenger's registery");
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
        for (;;) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          messenger.broadcast((message + " broadcasted from " + messenger.getAcceptPort()).getBytes());
        }
      }

    };
    Thread broadcastThread = new Thread(broadcast, "broadcastThread");
    broadcastThread.start();
  }

  public void setConnectCallback(messages.callbacks.ConnectCallback connectCallback) {
    this.connectCallback = connectCallback;
    this.setClosedCallback(connectCallback);
  }

  public void setAcceptCallback(messages.callbacks.AcceptCallback acceptCallback) {
    this.acceptCallback = acceptCallback;
    this.setClosedCallback(acceptCallback);
  }

  public void setClosedCallback(messages.callbacks.ClosableCallback callback) {
    this.closableCallback = callback;
  }

}
