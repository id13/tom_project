package tom.main.burst;

import java.net.InetSocketAddress;
import java.util.List;

import messages.engine.AcceptCallback;
import messages.engine.Channel;
import messages.engine.ConnectCallback;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.Server;
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
  public void closed(Channel channel) {
    Engine.panic("manager closed the channel");
  }

  @Override
  public void accepted(Server server, Channel channel) {
    for(InetSocketAddress address : this.addressesToConnect) {
      peer.connect(address);
    }
  }

  @Override
  public void deliver(Channel channel, byte[] bytes) {
    peer.send(new String(bytes));
  }

  @Override
  public void deliver(InetSocketAddress from, String message) {

    messenger.broadcast((message + " from " + from.getHostName() + ':' + from.getPort()).getBytes());
  }

  @Override
  public void connected(Channel channel) {
  }
  
}
