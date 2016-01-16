package messages.engine.burstTest;

import java.net.InetAddress;

import messages.callbacks.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.NioEngine;

public class MainTwo {

  public MainTwo() {
    // TODO Auto-generated constructor stub
  }

  public static void main(String[] args) {
    System.setProperty("java.net.preferIPv4Stack", "true");
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    Messenger messenger = new Messenger(engine, 53124);
    DeliverCallback callback = new DeliverCallbackImpl();
    messenger.setDeliverCallback(callback);    
    try {
      messenger.accept();
      InetAddress myAddress = InetAddress.getLoopbackAddress();
      messenger.connect(myAddress, 43124);
      messenger.runBurstBroadcastThread("hello !");
    } catch (Exception ex) {
      messenger.closeAllConnections();
      ex.printStackTrace();
      Engine.panic(ex.getMessage());
    }
  }

}
