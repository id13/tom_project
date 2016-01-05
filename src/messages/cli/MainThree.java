package messages.cli;

import messages.engine.Engine;
import messages.engine.nio.NioEngine;
import messages.service.Peer;

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
    Peer peer = new Peer(engine, 62124, System.out);
    try {    
      peer.accept();
      peer.connect("localhost", 43124);
      peer.connect("localhost", 53124);
      peer.runBroadcastThread("hello !");
    } catch(Exception ex) {
      peer.closeAllConnections();
      ex.printStackTrace();
      Engine.panic(ex.getMessage());
    } 
  }

}
