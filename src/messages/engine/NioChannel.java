package messages.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioChannel extends Channel implements ReceiveCallback {

  private static final int READING_LENGTH = 2;
  private static final int READING_MESSAGE = 3;  
  
  private SocketChannel channel;
  private ByteBuffer receiveBuffer;
  private DeliverCallback deliverCallback;
  private int receiveState;
  
  public NioChannel(SocketChannel channel) {
    this.channel = channel;
    this.receiveBuffer = ByteBuffer.allocate(4);
    this.receiveState = NioChannel.READING_LENGTH;
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
    SocketChannel socketChannel = (SocketChannel) channel;
    ByteBuffer buffer = ByteBuffer.allocate(4 + length);
    buffer.putInt(length);
    buffer.put(bytes, offset, length);
    buffer.position(0);
    socketChannel.write(buffer);
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
    switch (receiveState) {
    case READING_LENGTH:
      count = channel.read(receiveBuffer);
      if (count == -1) {
        Engine.panic("handleReceive: end of stream");
      }
      if (receiveBuffer.hasRemaining())
        return;
      receiveState = READING_MESSAGE;
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

      receiveState = READING_LENGTH;
      receiveBuffer = ByteBuffer.allocate(4);
      
      this.deliverCallback.deliver(this, bytes);
    }
  }
}
