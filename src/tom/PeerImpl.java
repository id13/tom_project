package tom;

import java.net.InetSocketAddress;

import messages.callbacks.AcceptCallback;
import messages.callbacks.ConnectCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.NioEngine;
import tom.messages.Message;

public class PeerImpl implements Peer, ConnectCallback, AcceptCallback {

  private final Messenger messenger;
  private final MessageManager messageManager;
  private int logicalClock = 0;
  private final InetSocketAddress myAddress;
  private final DistantPeerManager distantPeerManager;

  /**
   * This builder initiates a Peer. So, it initiates a NioEngine, a Messenger
   * and a MessageManager. It then call the accept method of the Messenger.
   * 
   * @param myAddress:
   *          The IPv4 address and the port number of the Accept.
   * @param callback:
   *          The callback used to display delivered messages.
   */
  public PeerImpl(InetSocketAddress myAddress, TomDeliverCallback callback) {
    this.myAddress = myAddress;
    this.distantPeerManager = new DistantPeerManager();
    NioEngine engine = NioEngine.getNioEngine();
    this.messenger = new Messenger(engine, myAddress.getPort());
    this.messageManager = new MessageManager(this, callback, messenger, distantPeerManager);
    this.messenger.setDeliverCallback(messageManager);
    this.messenger.setConnectCallback(this);
    this.messenger.setAcceptCallback(this);
    try {
      messenger.accept();
    } catch (Exception ex) {
      ex.printStackTrace();
      Engine.panic(ex.getMessage());
    }
  }

  @Override
  public void send(String content) {
    int lc = updateLogicalClock(0);
    Message message = new Message(lc, Message.MESSAGE, content);
    messageManager.treatMyMessage(message);
  }

  @Override
  public void connect(InetSocketAddress address) {
    messenger.connect(address.getHostName(), address.getPort());
  }

  @Override
  public synchronized int updateLogicalClock(int outsideLogicalClock) {
    if (outsideLogicalClock < this.logicalClock) {
      this.logicalClock++;
      return this.logicalClock;
    } else {
      this.logicalClock = outsideLogicalClock + 1;
      return this.logicalClock;
    }
  }

  @Override
  public void closed(InetSocketAddress address) {
    this.distantPeerManager.removeMember(address);
  }

  @Override
  public void connected(InetSocketAddress address) {
    System.out.println("[TOM] Connected to " + address);
    distantPeerManager.addMember(address);
  }

  @Override
  public InetSocketAddress getMyAddress() {
    return myAddress;
  }

  @Override
  public void accepted(InetSocketAddress address) {
    System.out.println("[TOM] Accepted " + address);
    distantPeerManager.addMember(address);
  }

  @Override
  public DistantPeerManager getDistantPeerManager() {
    return distantPeerManager;
  }
}
