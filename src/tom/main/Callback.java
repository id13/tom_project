package tom.main;

import java.net.InetSocketAddress;

import tom.Peer;
import tom.TomDeliverCallback;
import tom.TomJoinCallback;

public class Callback implements TomDeliverCallback, TomJoinCallback {

  private String name;

  public Callback(String name) {
    this.name = name;
  }

  @Override
  public void deliver(InetSocketAddress from, String message) {
    System.out.println("Delivered by TOM layer from " + from.toString() + " : " + message);

  }

  @Override
  public void joined(Peer peer) {
    System.out.println("Joined.");
  }

}
