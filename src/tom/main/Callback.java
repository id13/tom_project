package tom.main;

import java.net.InetSocketAddress;

import tom.TomDeliverCallback;

public class Callback implements TomDeliverCallback {

  private String name;

  public Callback(String name) {
    this.name = name;
  }

  @Override
  public void deliver(InetSocketAddress from, String message) {
    System.out.println("Delivered by TOM layer from " + from.toString() + " : " + message);

  }
}
