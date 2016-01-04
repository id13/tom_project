package messages.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioChannel extends Channel implements ReceiveCallback, WriteCallback {

  private static final int CONNECTED = 0;
  private static final int SENDING = 1;
  private static final int READING_LENGTH = 2;
  private static final int READING_MESSAGE = 3;  
  
  private SocketChannel channel;
  private SelectionKey key;
  private ByteBuffer receiveBuffer;
  private ByteBuffer sendBuffer;
  private DeliverCallback deliverCallback;
  private int state;
  
  public NioChannel(SocketChannel channel) {
    this.channel = channel;
    this.receiveBuffer = ByteBuffer.allocate(4);
    this.state = NioChannel.CONNECTED;
  }

  public void setSelectionKey(SelectionKey key) {
    this.key = key;
  }
  
  @Override
  public void setDeliverCallback(DeliverCallback callback) {
    this.deliverCallback = callback;
  }

  @Override
  public InetSocketAddress getRemoteAddress() throws IOException {
      return (InetSocketAddress) (channel.getLocalAddress());      
  }

  @Override
  public void send(byte[] bytes, int offset, int length) throws IOException {
    assert(state == NioChannel.CONNECTED);
    sendBuffer = ByteBuffer.allocate(4 + length);
    sendBuffer.putInt(length);
    sendBuffer.put(bytes, offset, length);
    sendBuffer.position(0);
    
    state = NioChannel.SENDING;
    this.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
  }
  
  @Override
  public void handleWrite() throws IOException {
    assert(state == SENDING);
    int count = channel.write(sendBuffer);
    if (count == -1) {
      Engine.panic("handleWrite: end of steam detected");
    }
    if (sendBuffer.remaining() == 0) {
      state = CONNECTED;
      this.key.interestOps(SelectionKey.OP_READ);
    }    
  }

  @Override
  public void close() {
    try {
      channel.close();
    } catch (IOException e) {
      Engine.panic("can not close channel.");
    }
  }

  @Override
  public void handleReceive() throws IOException {
    int len, count = 0;
    switch (state) {
    case CONNECTED:
      this.state = NioChannel.READING_LENGTH;
      handleReceive();
    case READING_LENGTH:
      count = channel.read(receiveBuffer);
      if (count == -1) {
        Engine.panic("handleReceive: end of stream");
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
        Engine.panic("handleReceive: end of stream");
      }
      if (receiveBuffer.hasRemaining())
        return;

      receiveBuffer.position(0);
      byte bytes[] = new byte[receiveBuffer.remaining()];
      receiveBuffer.get(bytes);

      state = READING_LENGTH;
      receiveBuffer = ByteBuffer.allocate(4);
      
      this.deliverCallback.deliver(this, bytes);
    default: 
      Engine.panic("handleReceive: falling into unexpected state");
    }
  }
}
