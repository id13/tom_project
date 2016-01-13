package messages.engine.burstTest;

import messages.callbacks.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.NioEngine;

public class MainOne {

  public MainOne() {
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
    Messenger messenger = new Messenger(engine, 43124);
    DeliverCallback callback = new DeliverCallbackImpl();
    messenger.setDeliverCallback(callback);
    try {
      messenger.accept();
      messenger.runBurstBroadcastThread("hello !");
    } catch (Exception ex) {
      messenger.closeAllConnections();
      ex.printStackTrace();
      Engine.panic(ex.getMessage());
    }
  }

}
