package tom.main;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.engine.nio.NioEngine;
import tom.Peer;
import tom.PeerImpl;

public class MainTwo {

  public static void main(String[] args) throws UnknownHostException {
    System.setProperty("java.net.preferIPv4Stack", "true");
    Callback callback2 = new Callback("peer2");
    InetAddress myIpAddress = Inet4Address.getLocalHost();
    InetSocketAddress myAddress = new InetSocketAddress(myIpAddress, 12381);
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    Peer peer2 = new PeerImpl(myAddress, callback2);
    peer2.connect(new InetSocketAddress("localhost", 12380));
  }

}
