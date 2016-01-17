package tom.main.burst;


import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import messages.callbacks.AcceptCallback;
import messages.callbacks.ConnectCallback;
import messages.callbacks.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.NioEngine;

public class BurstManager implements AcceptCallback, ConnectCallback, DeliverCallback {

  private HashMap<InetSocketAddress, LinkedList<String>> messagesToCheck;
  private Set<InetSocketAddress> slaves = new HashSet<>();
  private Set<InetSocketAddress> slavesToConnect;
  private Set<InetSocketAddress> slavesToJoin;
  private boolean isRunning = false;
  private Messenger messenger;
  private InetSocketAddress startingPeer;
  private int port;

  public BurstManager(int port, InetSocketAddress startingPeer) {
    this.port = port;
    this.messagesToCheck = new HashMap<InetSocketAddress, LinkedList<String>>();
    this.messagesToCheck.put(startingPeer, new LinkedList<>());
    this.startingPeer = startingPeer;
  }

  public void createMessenger(Set<InetSocketAddress> slavesToConnect, Set<InetSocketAddress> slavesToJoin) throws UnknownHostException, SecurityException, IOException {
    this.slavesToConnect = slavesToConnect;
    this.slavesToJoin = slavesToJoin;
    this.messenger = new Messenger(NioEngine.getNioEngine(), this.port);
    this.messenger.setConnectCallback(this);
    this.messenger.setClosedCallback(this);
    this.messenger.setDeliverCallback(this);
    messenger.accept();
    for (InetSocketAddress slaveToConnect : this.slavesToConnect) {
      this.messenger.connect(slaveToConnect.getAddress(), slaveToConnect.getPort());
    }
  }

  public static void main(String[] args) throws UnknownHostException, SecurityException, IOException {
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      @Override
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    InetAddress myIpAddress = InetAddress.getLoopbackAddress();
    BurstManager manager = new BurstManager(22379, new InetSocketAddress(myIpAddress, 22380));
    Set<InetSocketAddress> addressesToConnect = new HashSet<InetSocketAddress>();
    Set<InetSocketAddress> addressesToJoin = new HashSet<>();
    addressesToConnect.add(new InetSocketAddress(myIpAddress, 22380));
    addressesToConnect.add(new InetSocketAddress(myIpAddress, 22381));
    addressesToConnect.add(new InetSocketAddress(myIpAddress, 22382));
    addressesToJoin.add(new InetSocketAddress(myIpAddress, 22381));
    addressesToJoin.add(new InetSocketAddress(myIpAddress, 22382));    
    manager.createMessenger(addressesToConnect, addressesToJoin);
    manager.startBursting();
  }

  public void checkMessages() {
    String baseMessage = null;
    String nextMessage = null;
    // First we check whether the first message of each list are the same
    for (Entry<InetSocketAddress, LinkedList<String>> messages : this.messagesToCheck.entrySet()) {
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
    for (Entry<InetSocketAddress, LinkedList<String>> messages : this.messagesToCheck.entrySet()) {
      messages.getValue().poll();
    }
    System.out.println("message delivered by enslaved peers : " + nextMessage);
  }

  @Override
  public void delivered(InetSocketAddress from, byte[] content) {
    if((new String(content)).equals("JOINED")) {
      this.slavesToJoin.remove(from);
      this.messagesToCheck.put(from, new LinkedList<String>());
      this.slaves.add(from);
    } else {
      LinkedList<String> messages = this.messagesToCheck.get(from);
      messages.add(new String(content));
      this.messagesToCheck.put(from, messages);
      this.checkMessages();      
    }
  }

  public void sendMessagerOrder(String content) {
    for(InetSocketAddress slave : this.slaves) {
      this.messenger.send(slave, content.getBytes());
    }
  }

  @Override
  public void closed(InetSocketAddress address) {
    Engine.panic("a member left the group");
  }

  public void startBursting() {
    System.out.println("Start Bursting");
    this.isRunning = true;
    BurstManager manager = this;
    Runnable burstLoop = new Runnable() {
      @Override
      public void run() {
        int cpt = 0;
        for (;;) {
          try {
            Thread.currentThread().sleep(1);
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
  public void connected(InetSocketAddress address) {
    System.out.println("Connected to " + address);
    this.slavesToConnect.remove(address);
    if(address.equals(this.startingPeer)) {
      this.slaves.add(this.startingPeer);
    }
  }

  @Override
  public void accepted(InetSocketAddress address) {
    System.out.println("Accepted " + address);
  }

}
