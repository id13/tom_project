package tom.main.burst;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.NioEngine;

public class MainTwo {

  public static void main(String[] args) {
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    InetSocketAddress myAddress = new InetSocketAddress("localhost", 12381);
    List<InetSocketAddress> addressesToConnect = new ArrayList<InetSocketAddress>();
    addressesToConnect.add(new InetSocketAddress("localhost", 12380));
    Messenger messenger = new Messenger(engine, 22381);
    PeerWrapper PeerWrp = new PeerWrapper(messenger, myAddress, addressesToConnect);
  }

}
