package tom;

import messages.engine.Channel;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.nio.NioEngine;

public class Peer implements DeliverCallback {

  private Messenger messenger;
  private int logicalClock = 0;

  public Peer(int port) {
    System.setProperty("java.net.preferIPv4Stack", "true");
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    this.messenger = new Messenger(engine, port, System.out); // TODO: remove
                                                              // System.out
    try {
      messenger.accept();
    } catch (Exception ex) {
      ex.printStackTrace();
      Engine.panic(ex.getMessage());
    }
  }

  public void send(String content) {
    logicalClock++;
    Message message = new Message(logicalClock, Message.TYPE_MESSAGE, content);
    messenger.broadcast(message.getFullMessage());
  }

  @Override
  public void deliver(Channel channel, byte[] bytes) {
    // TODO Auto-generated method stub
  }
  
}
