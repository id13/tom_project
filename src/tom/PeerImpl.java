package tom;

import java.util.HashSet;
import java.util.Set;

import messages.engine.Channel;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.nio.NioEngine;

public class PeerImpl implements Peer {

  private final int port;
  private final Messenger messenger;
  private final MessageManager messagesStock;
  private int logicalClock = 0;
  private Set<Channel> channels;

  public PeerImpl(int port, TomDeliverCallback callback) {
  	this.port = port;
  	this.channels = new HashSet<>();
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
    messagesStock.treatMyMessage(message);
    messenger.broadcast(message.getFullMessage());
  }

  @Override
  public Set<Channel> getChannelGroup() {
  	return this.channels;
  }

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void connect(int port) {
		messenger.connect("localhost", port);
	}
}
