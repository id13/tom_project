package messages.engine.nio.burstTest;

import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.nio.NioEngine;

public class MainThree {

  public MainThree() {
    // TODO Auto-generated constructor stub
  }

  public static void main(String[] args) {
    System.setProperty("java.net.preferIPv4Stack" , "true");
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    Messenger messenger = new Messenger(engine, 62124);
    try {    
      messenger.accept();
      messenger.connect("localhost", 43124);
      messenger.connect("localhost", 53124);
      messenger.runBurstBroadcastThread("hello !");
    } catch(Exception ex) {
      messenger.closeAllConnections();
      ex.printStackTrace();
      Engine.panic(ex.getMessage());
    } 
  }

}
