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

  private HashMap<InetSocketAddress, LinkedList<String>> messagesToChek;
  private Set<InetSocketAddress> slaves = new HashSet<>();
  private Set<InetSocketAddress> slavesToConnect;
  private Set<InetSocketAddress> slavesToJoin;
  private boolean isRunning = false;
  private Messenger messenger;
  private int port;

  public BurstManager(int port) {
    this.port = port;
    this.messagesToChek = new HashMap<InetSocketAddress, LinkedList<String>>();
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
      this.messagesToChek.put(new InetSocketAddress(slaveToConnect.getAddress(), slaveToConnect.getPort()), new LinkedList<String>());
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
    BurstManager manager = new BurstManager(22379);
    InetAddress myIpAddress = InetAddress.getLoopbackAddress();
    Set<InetSocketAddress> addressesToConnect = new HashSet<InetSocketAddress>();
    Set<InetSocketAddress> addressesToJoin = new HashSet<>();
    addressesToConnect.add(new InetSocketAddress(myIpAddress, 22380));
    addressesToConnect.add(new InetSocketAddress(myIpAddress, 22381));
    addressesToConnect.add(new InetSocketAddress(myIpAddress, 22382));
    addressesToJoin.add(new InetSocketAddress(myIpAddress, 22381));
    addressesToJoin.add(new InetSocketAddress(myIpAddress, 22382));    
    manager.createMessenger(addressesToConnect, addressesToJoin);

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
  public void delivered(InetSocketAddress from, byte[] content) {
    if((new String(content)).equals("JOINED")) {
      this.slavesToJoin.remove(from);
      this.startBurstIfPossible();
    } else {
      LinkedList<String> messages = this.messagesToChek.get(from);
      messages.add(new String(content));
      this.messagesToChek.put(from, messages);
      this.checkMessages();      
    }
  }

  public void sendMessagerOrder(String content) {
    this.messenger.broadcast(content.getBytes());
  }

  @Override
  public void closed(InetSocketAddress address) {
    this.messenger.closeAllConnections();
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
            // On my computer, this is the limit value, bellow that, the tom
            // layer begins to fall apart
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
  
  private void startBurstIfPossible() {
    if(this.slavesToConnect.isEmpty() && this.slavesToJoin.isEmpty() && !this.isRunning)
      this.startBursting();
  }
  
  @Override
  public void connected(InetSocketAddress address) {
    System.out.println("Connected to " + address);
    this.slaves.add(address);
    this.slavesToConnect.remove(address);
    this.startBurstIfPossible();
  }

  @Override
  public void accepted(InetSocketAddress address) {
    System.out.println("Accepted " + address);
  }

}
