package tom.main;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import messages.engine.NioEngine;
import tom.Peer;
import tom.PeerImpl;

public class MainOne {

  public static void main(String[] args) throws UnknownHostException {

    System.setProperty("java.net.preferIPv4Stack", "true");
    Callback callback1 = new Callback("peer1");
    InetAddress myIpAddress = InetAddress.getLoopbackAddress();
    InetSocketAddress myAddress = new InetSocketAddress(myIpAddress, 12380);
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    Peer peer1 = new PeerImpl(myAddress, callback1, callback1);
  }
}
