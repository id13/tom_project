package tom;

import java.util.Set;

import messages.engine.Channel;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.nio.NioEngine;

public class PeerImpl implements Peer {

  private final Messenger messenger;
  private final MessageManager messagesStock;
  private int logicalClock = 0;

  public PeerImpl(int port, TomDeliverCallback callback) {
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    this.messagesStock = new MessageManager(this, callback);
    this.messenger = new Messenger(engine, port);
    this.messenger.setDeliverCallback(messagesStock);
    try {
      messenger.accept();
    } catch (Exception ex) {
      ex.printStackTrace();
      Engine.panic(ex.getMessage());
    }
  }
  
  @Override
  public void send(String content) {
    logicalClock++; 
    Message message = new Message(logicalClock, Message.TYPE_MESSAGE, content);
    byte[] bytes = message.getFullMessage();
    messenger.broadcast(bytes);
  }

  @Override
  public Set<Channel> getChannelGroup() {
    // TODO Auto-generated method stub
    return null;
  }
}
