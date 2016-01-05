package messages.engine.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import messages.engine.Channel;
import messages.engine.ClosableCallback;
import messages.engine.DeliverCallback;
import messages.engine.Engine;
import messages.engine.ReceiveCallback;
import messages.engine.Server;
import messages.engine.WriteCallback;

public class NioChannel extends Channel implements ReceiveCallback, WriteCallback {

  private static final int DISCONNECTED = 0;
  private static final int CONNECTED = 2;
  private static final int SENDING = 3;
  private static final int READING_LENGTH = 4;
  private static final int READING_MESSAGE = 5;  
  
  private SocketChannel channel;
  private SelectionKey key;
  private ByteBuffer receiveBuffer;
  private ByteBuffer sendBuffer;
  private DeliverCallback deliverCallback;
  private NioServer server;
  private Integer state;
  private ClosableCallback closableCallback;
  private InetSocketAddress remoteLocalAddress;
  
  public NioChannel(SocketChannel channel) throws IOException {
    this.channel = channel;
    this.receiveBuffer = ByteBuffer.allocate(4);
    this.state = NioChannel.CONNECTED;
    this.remoteLocalAddress = (InetSocketAddress) (channel.getLocalAddress());
  }
  
  @Override
  public void setDeliverCallback(DeliverCallback callback) {
    this.deliverCallback = callback;
  }

  @Override
  public InetSocketAddress getRemoteAddress() throws IOException {
      return this.remoteLocalAddress;
  }

  @Override
  public void send(byte[] bytes, int offset, int length) throws IOException {
    synchronized(state) {
      if(state != CONNECTED)
        return;
      state = NioChannel.SENDING;
      sendBuffer = ByteBuffer.allocate(4 + length);
      sendBuffer.putInt(length);
      sendBuffer.put(bytes, offset, length);
      sendBuffer.position(0);
      this.key.interestOps(SelectionKey.OP_WRITE);
    }
  }
  
  @Override
  public void handleWrite() throws IOException {
    synchronized(state) {
      if(state != SENDING)
        return;
      int count = channel.write(sendBuffer);
      if (count == -1) {
        // According to the nio doc, -1 means the channel is closed, so we notify
        // the closableCallback      
        this.close();
        return;
      }
      if (sendBuffer.remaining() == 0) {
        state = CONNECTED;
        this.key.interestOps(SelectionKey.OP_READ);
      }   
    }
  }

  @Override
  public void close() {
    try {
      synchronized(state) {
        this.server.close();
        this.state = DISCONNECTED;
        this.closableCallback.closed(this);
      }
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic("can not close channel.");
    }
  }
  
  @Override
  public void setServer(Server server) {
    if(!(server instanceof NioServer))
      Engine.panic("setServer: NioChannel MUST use ONLY NioServer");
    this.server = (NioServer) server;
    this.key = this.server.getSelectionKey();
  }

  @Override
  public void handleReceive() throws IOException {
    synchronized(this.state) {
      int len, count = 0;
      switch (state) {
      case SENDING:
        return;
      case DISCONNECTED:
        return;
      case CONNECTED:
        this.state = NioChannel.READING_LENGTH;
      case READING_LENGTH:
        count = channel.read(receiveBuffer);
        if (count == -1) {
          // According to the nio doc, -1 means the channel is closed, so we notify
          // the closableCallback
          this.close();
          return;
        }
        if (receiveBuffer.hasRemaining())
          return;
        state = READING_MESSAGE;
        receiveBuffer.position(0);
        len = receiveBuffer.getInt();
        receiveBuffer = ByteBuffer.allocate(len);
      case READING_MESSAGE:
        count = channel.read(receiveBuffer);
        if (count == -1) {
          // According to the nio doc, -1 means the channel is closed, so we notify
          // the closableCallback
          this.close();
          return;
        }
        if (receiveBuffer.hasRemaining())
          return;

        receiveBuffer.position(0);
        byte bytes[] = new byte[receiveBuffer.remaining()];
        receiveBuffer.get(bytes);

        state = READING_LENGTH;
        receiveBuffer = ByteBuffer.allocate(4);
        this.state = NioChannel.CONNECTED;
        this.deliverCallback.deliver(this, bytes);
        return;
      default: 
        Engine.panic("handleReceive: falling into unexpected state");
      }
    }
  }
  
  @Override
  public int compareTo(Channel o) {
    if(o.getServer() == null) {
      Engine.panic("attached server to a channel MUST not be null");
    }
    return new Integer(this.server.getPort()).compareTo(new Integer(o.getServer().getPort()));
  }

  @Override
  public Server getServer() {
    return this.server;
  }

  @Override
  public void setClosableCallback(ClosableCallback callback) {
    this.closableCallback = callback;
  }
}
