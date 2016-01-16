package tom.main.burst;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import messages.engine.Messenger;
import messages.engine.NioEngine;

public class MainThree {

  public static void main(String[] args) {
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    InetAddress myIpAddress = InetAddress.getLoopbackAddress();
    InetSocketAddress myAddress = new InetSocketAddress(myIpAddress, 12382);
    List<InetSocketAddress> addressesToConnect = new ArrayList<InetSocketAddress>();
    addressesToConnect.add(new InetSocketAddress(myIpAddress, 12380));
    addressesToConnect.add(new InetSocketAddress(myIpAddress, 12381));
    Messenger messenger = new Messenger(engine, 22382);
    PeerWrapper PeerWrp = new PeerWrapper(messenger, myAddress, addressesToConnect);

  }

}
