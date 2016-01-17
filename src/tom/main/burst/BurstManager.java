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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import messages.callbacks.AcceptCallback;
import messages.callbacks.ConnectCallback;
import messages.callbacks.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.NioEngine;
import messages.util.ByteUtil;
import tom.messages.Message;

public class BurstManager implements AcceptCallback, ConnectCallback, DeliverCallback {

  private HashMap<InetSocketAddress, LinkedList<String>> messagesToCheck;
  private Set<InetSocketAddress> slaves = new HashSet<>();
  private Set<InetSocketAddress> slavesToConnect;
  private Set<InetSocketAddress> slavesToJoin;
  private boolean isRunning = false;
  private Messenger messenger;
  private InetSocketAddress startingPeer;
  private LinkedList<Integer> messengerPortsRange; 
  private LinkedList<Integer> peersPortsRange;  
  private int port;

  public BurstManager(int port, InetSocketAddress startingPeer, LinkedList<Integer> messengerPortsRange, LinkedList<Integer> peersPortsRange) {
    this.port = port;
    this.messagesToCheck = new HashMap<InetSocketAddress, LinkedList<String>>();
    this.messagesToCheck.put(startingPeer, new LinkedList<>());
    this.startingPeer = startingPeer;
    this.peersPortsRange = peersPortsRange;
    this.messengerPortsRange = messengerPortsRange;
  }
  
  /**
   * @return the messengerPortsRange
   */
  public LinkedList<Integer> getMessengerPortsRange() {
    return messengerPortsRange;
  }
  /**
   * @return the peersPortsRange
   */
  public LinkedList<Integer> getPeersPortsRange() {
    return peersPortsRange;
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
    LinkedList<Integer> messengerPortsRange = new LinkedList<>(
        IntStream.range(22383, 22983).boxed().collect(Collectors.toList()));
    LinkedList<Integer> peersPortsRange = new LinkedList<>(
        IntStream.range(12383, 12983).boxed().collect(Collectors.toSet()));    
    BurstManager manager = new BurstManager(22379, new InetSocketAddress(myIpAddress, 22380), messengerPortsRange, peersPortsRange);
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
    byte[] actualContent = new byte[content.length - 4];
    byte[] typeB = new byte[4];
    System.arraycopy(content, 0, typeB, 0, 4);
    System.arraycopy(content, 4, actualContent, 0, actualContent.length);
    int type = ByteUtil.readInt32(typeB, 0);
    if(type == Message.JOIN) {
      try {
        InetSocketAddress newMember = ByteUtil.readInetSocketAddress(actualContent, 0);
        this.slavesToJoin.remove(newMember);
        if(!this.slaves.contains(newMember)) {
          this.messagesToCheck.put(from, new LinkedList<String>());
          this.slaves.add(from);
        }
      } catch (UnknownHostException e) {
        e.printStackTrace();
        Engine.panic(e.getMessage());
      }
    } else if(type == Message.MESSAGE) {
      LinkedList<String> messages = this.messagesToCheck.get(from);
      messages.add(new String(actualContent));
      this.messagesToCheck.put(from, messages);
      this.checkMessages();      
    } else {
      Engine.panic("unexpected message type");
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
  
  public Messenger getMessenger() {
    return this.messenger;
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
            Thread.sleep(1);
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
