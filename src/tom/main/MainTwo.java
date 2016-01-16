package tom.main;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.engine.Engine;
import messages.engine.NioEngine;
import tom.Peer;
import tom.PeerImpl;

public class MainTwo {

  public static void main(String[] args) throws SecurityException, IOException {
    System.setProperty("java.net.preferIPv4Stack", "true");
    Callback callback2 = new Callback("peer2");
    InetAddress myIpAddress = InetAddress.getLoopbackAddress();
    InetSocketAddress myAddress = new InetSocketAddress(myIpAddress, 12381);
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    Peer peer2 = new PeerImpl(myAddress, callback2, callback2);
    try {
      peer2.connect(new InetSocketAddress(myIpAddress, 12380));
    } catch (tom.ConnectException e) {
      Engine.panic(e.getMessage());
    }
  }

}
