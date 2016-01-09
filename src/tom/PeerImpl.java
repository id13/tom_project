package tom;

import java.util.HashSet;
import java.util.Set;

import messages.engine.Channel;
import messages.engine.ConnectCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.nio.NioEngine;

public class PeerImpl implements Peer, ConnectCallback {

  private final int port;
  private final Messenger messenger;
  private final MessageManager messageManager;
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
    this.messenger = new Messenger(engine, port);
    this.messageManager = new MessageManager(this, callback, messenger);
    this.messenger.setDeliverCallback(messageManager);
    this.messenger.setConnectCallback(this);
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
    messageManager.treatMyMessage(message);
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

	@Override
	public int updateLogicalClock(int outsideLogicalClock) {
		if (outsideLogicalClock < this.logicalClock) {
			this.logicalClock++;
			return this.logicalClock;
		} else {
			this.logicalClock = outsideLogicalClock + 1;
			return this.logicalClock;
		}
	}

	@Override
	public void closed(Channel channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void connected(Channel channel) {
		channels.add(channel);
		// TODO: later, we will add to channels only the new peers of the group
		// But there, we assume that the group is created before to use it.
	}
}
