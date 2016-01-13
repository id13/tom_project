package tom.main;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.engine.NioEngine;
import tom.Peer;
import tom.PeerImpl;

public class MainThree {

  public static void main(String[] args) throws UnknownHostException {
    System.setProperty("java.net.preferIPv4Stack", "true");
    Callback callback3 = new Callback("peer3");
    InetAddress myIpAddress = Inet4Address.getLocalHost();
    InetSocketAddress myAddress = new InetSocketAddress(myIpAddress, 12382);
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    Peer peer3 = new PeerImpl(myAddress, callback3);
    peer3.connect(new InetSocketAddress("localhost", 12380));
    peer3.connect(new InetSocketAddress("localhost", 12381));
    Runnable runnable = new Runnable() {

      @Override
      public void run() {
        // while (!Thread.interrupted()) {
        try {
          Thread.sleep(1000);
          peer3.send("Bonjour");
        } catch (InterruptedException e) {
          System.out.println("Interrupted");
        }
        // }
      }
    };
    Thread t = new Thread(runnable);
    t.start();

  }

}
