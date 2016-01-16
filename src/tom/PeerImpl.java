package tom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.callbacks.AcceptCallback;
import messages.callbacks.ConnectCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.NioEngine;
import tom.messages.Message;

public class PeerImpl implements Peer, ConnectCallback, AcceptCallback {

  private static final byte IN_GROUP = 0;
  private static final byte CONNECTING = 1;

  private final Messenger messenger;
  private final MessageManager messageManager;
  private int logicalClock = 0;
  private final InetSocketAddress myAddress;
  private final DistantPeerManager distantPeerManager;
  private final TomJoinCallback joinCallback;
  private byte stateGroup = IN_GROUP;
  private InetSocketAddress integratorIntoGroup;

  /**
   * This builder initiates a Peer. So, it initiates a NioEngine, a Messenger
   * and a MessageManager. It then call the accept method of the Messenger.
   * 
   * @param myAddress:
   *          The IPv4 address and the port number of the Accept.
   * @param callback:
   *          The callback used to display delivered messages.
   */
  public PeerImpl(InetSocketAddress myAddress, TomDeliverCallback deliver, TomJoinCallback join) {
    this.myAddress = myAddress;
    this.distantPeerManager = new DistantPeerManager();
    this.joinCallback = join;
    NioEngine engine = NioEngine.getNioEngine();
    this.messenger = new Messenger(engine, myAddress.getPort());
    this.messageManager = new MessageManager(this, deliver, messenger, distantPeerManager);
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
  public void send(String content) throws SendException {
    if (stateGroup != IN_GROUP) {
      throw new SendException("A peer can not send a message if he is not in a group");
    }
    int lc = updateLogicalClock(0);
    Message message = new Message(lc, Message.MESSAGE, content);
    messageManager.treatMyMessage(message);
  }

  @Override
  public synchronized void connect(InetSocketAddress address)
      throws UnknownHostException, SecurityException, IOException, ConnectException {
    if (stateGroup == CONNECTING) {
      throw new ConnectException("call to method connect while the peer is already connecting to a group.");
    }
    if (distantPeerManager.getGroup().isEmpty()) {
      integratorIntoGroup = address;
      stateGroup = CONNECTING;
      messenger.connect(address.getAddress(), address.getPort());
    } else {
      throw new ConnectException("call to method connect while the peer is already connected to a group.");
    }

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
    if (integratorIntoGroup.equals(address)) {
      messageManager.sendJoinRequest(address);
    }
  }

  @Override
  public InetSocketAddress getMyAddress() {
    return myAddress;
  }

  @Override
  public void accepted(InetSocketAddress address) {
    System.out.println("[TOM] Accepted " + address);
    if (this.stateGroup != IN_GROUP) {
      Engine.panic("A connection has been accepted while we were not in a group");
    }
    distantPeerManager.addWaitingMember(address);
    this.messageManager.checkAndUpdatePendingAcks(address);
  }

  @Override
  public DistantPeerManager getDistantPeerManager() {
    return distantPeerManager;
  }

  @Override
  public void setConnected(int logicalClock) {
    this.logicalClock = logicalClock;
    this.stateGroup = IN_GROUP;
    joinCallback.joined(this);
  }

  @Override
  public boolean isInGroup() {
    return stateGroup == IN_GROUP;
  }
}
