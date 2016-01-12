package tom.main.burst;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import messages.engine.AcceptCallback;
import messages.engine.Channel;
import messages.engine.ConnectCallback;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.Server;
import messages.engine.nio.NioEngine;

public class BurstManager implements AcceptCallback, ConnectCallback, DeliverCallback {

  private HashMap<InetSocketAddress, LinkedList<String>> messagesToChek;
  private Set<InetSocketAddress> slaves = new HashSet<>();
  private Set<InetSocketAddress> slavesToConnect;
  private Messenger messenger;
  private int port;

  public BurstManager(int port) {
    this.port = port;
    this.messagesToChek = new HashMap<InetSocketAddress, LinkedList<String>>();
  }

  public void createMessenger(Set<InetSocketAddress> slavesToConnect) {
    this.slavesToConnect = slavesToConnect;
    this.messenger = new Messenger(NioEngine.getNioEngine(), this.port);
    this.messenger.setConnectCallback(this);
    this.messenger.setClosedCallback(this);
    this.messenger.setDeliverCallback(this);
    messenger.accept();
    for (InetSocketAddress slaveToConnect : this.slavesToConnect) {
      this.messenger.connect("localhost", slaveToConnect.getPort());
      this.messagesToChek.put(new InetSocketAddress("localhost", slaveToConnect.getPort()), new LinkedList<String>());
    }
  }

  public static void main(String[] args) {
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      @Override
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    BurstManager manager = new BurstManager(22379);
    Set<InetSocketAddress> addressesToConnect = new HashSet<InetSocketAddress>();
    addressesToConnect.add(new InetSocketAddress("localhost", 22380));
    addressesToConnect.add(new InetSocketAddress("localhost", 22381));
    addressesToConnect.add(new InetSocketAddress("localhost", 22382));
    manager.createMessenger(addressesToConnect);

  }

  public void checkMessages() {
    String baseMessage = null;
    String nextMessage = null;
    // First we check whether the first message of each list are the same
    for (Entry<InetSocketAddress, LinkedList<String>> messages : this.messagesToChek.entrySet()) {
      if (messages.getValue().isEmpty())
        return;
      nextMessage = messages.getValue().peek();
      if (baseMessage == null) {
        baseMessage = nextMessage;
      } else {
        if (!baseMessage.equals(nextMessage))
          Engine.panic("the enslaved peers did not delivered the same message");
      }
    }
    // If so, we remove these messages
    for (Entry<InetSocketAddress, LinkedList<String>> messages : this.messagesToChek.entrySet()) {
      messages.getValue().poll();
    }
    System.out.println("message delivered by enslaved peers : " + nextMessage);
  }

  @Override
  public void deliver(Channel channel, byte[] bytes) {
    try {
      LinkedList<String> messages = this.messagesToChek.get(channel.getRemoteAddress());
      messages.add(new String(bytes));
      this.messagesToChek.put(channel.getRemoteAddress(), messages);
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
    this.checkMessages();
  }

  public void sendMessagerOrder(String content) {
    this.messenger.broadcast(content.getBytes());
  }

  @Override
  public void closed(Channel channel) {
    this.messenger.closeAllConnections();
    Engine.panic("a member left the group");
  }

  public void startBursting() {
    BurstManager manager = this;
    Runnable burstLoop = new Runnable() {
      @Override
      public void run() {
        int cpt = 0;
        for (;;) {
          try {
            // On my computer, this is the limit value, bellow that, the tom
            // layer begins to fall apart
            Thread.currentThread().sleep(60);
          } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            Engine.panic(e.getMessage());
          }
          manager.sendMessagerOrder("hello number " + cpt);
          cpt++;
        }
      }
    };
    Thread burstThread = new Thread(burstLoop, "burstThread");
    burstThread.start();
  }

  @Override
  public void connected(Channel channel) {
    try {
      this.slaves.add(channel.getRemoteAddress());
      this.slavesToConnect.remove(channel.getRemoteAddress());
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
    if (this.slavesToConnect.isEmpty())
      this.startBursting();
  }

  @Override
  public void accepted(Server server, Channel channel) {
    // XXX Auto-generated method stub

  }

}
