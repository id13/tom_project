package tom;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

import messages.engine.Channel;
import messages.engine.ConnectCallback;
import messages.engine.Engine;
import messages.engine.Messenger;
import messages.engine.nio.NioEngine;

public class PeerImpl implements Peer, ConnectCallback {

  private final Messenger messenger;
  private final MessageManager messageManager;
  private int logicalClock = 0;
	private final InetSocketAddress myAddress;
	private Set<InetSocketAddress> group;

  public PeerImpl(InetSocketAddress myAddress, TomDeliverCallback callback) {
  	this.myAddress = myAddress;
  	this.group = new HashSet<>();
    NioEngine engine = NioEngine.getNioEngine();
    Runnable engineLoop = new Runnable() {
      public void run() {
        engine.mainloop();
      }
    };
    Thread engineThread = new Thread(engineLoop, "engineThread");
    engineThread.start();
    this.messenger = new Messenger(engine, myAddress.getPort());
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
		try {
			group.add(channel.getRemoteAddress());
		} catch (IOException e) {
			e.printStackTrace();
			Engine.panic(e.getMessage());
			// TODO: handle that correctly.
		}
		// TODO: later, we will add to channels only the new peers of the group
		// But there, we assume that the group is created before to use it.
	}

	@Override
	public Set<InetSocketAddress> getGroup() {
		return group;
	}

	@Override
	public InetSocketAddress getMyAddress() {
		return myAddress;
	}
}
