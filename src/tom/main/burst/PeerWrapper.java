package tom.main.burst;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import messages.callbacks.AcceptCallback;
import messages.callbacks.ConnectCallback;
import messages.callbacks.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import tom.Peer;
import tom.PeerImpl;
import tom.TomDeliverCallback;

public class PeerWrapper implements AcceptCallback, ConnectCallback, DeliverCallback, TomDeliverCallback {

  Messenger messenger;
  Peer peer;
  List<InetSocketAddress> addressesToConnect;

  public PeerWrapper(Messenger messenger, InetSocketAddress peerAddress, List<InetSocketAddress> addressesToConnect) {
    this.messenger = messenger;
    this.peer = new PeerImpl(peerAddress, this);
    this.addressesToConnect = addressesToConnect;
    messenger.setAcceptCallback(this);
    messenger.setClosedCallback(this);
    messenger.setDeliverCallback(this);
    messenger.setConnectCallback(this);
    messenger.accept();
  }

  @Override
  public void closed(InetSocketAddress address) {
    Engine.panic("manager closed the channel");
  }

  @Override
  public void accepted(InetSocketAddress address) {
    System.out.println("Accepted " + address);

    for (InetSocketAddress distantPeer : this.addressesToConnect) {
      try {
        peer.connect(distantPeer);
      } catch (SecurityException | IOException e) {
        e.printStackTrace();
        Engine.panic(e.getMessage());
      }
    }
  }

  @Override
  public void delivered(InetSocketAddress from, byte[] content) {
    peer.send(new String(content));
  }

  @Override
  public void deliver(InetSocketAddress from, String message) {
    messenger.broadcast(
        (message + " from " + from.getAddress().getHostAddress() + ':' + from.getPort()).getBytes());
  }

  @Override
  public void connected(InetSocketAddress address) {
    System.out.println("Connected to " + address);
  }
}
