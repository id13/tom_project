package messages.cli;

import messages.engine.Engine;
import messages.engine.nio.NioEngine;
import messages.service.Peer;

public class MainOne {

  public MainOne() {
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
    Peer peer = new Peer(engine, 43124, System.out);  
    try {  
      peer.accept();
    } catch(Exception ex) {
      peer.closeAllConnections();
      ex.printStackTrace();
      Engine.panic(ex.getMessage());
    } 
  }

}
