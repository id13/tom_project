package messages.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioChannel extends Channel {

  private SocketChannel channel;
  private SelectionKey selectionKey;
  
  public NioChannel(SocketChannel channel, SelectionKey key) {
    this.channel = channel;
    this.selectionKey = key;
  }

  @Override
  public void setDeliverCallback(DeliverCallback callback) {
    // TODO Auto-generated method stub

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
      Engine.panic("can not close socket.");
    }
  }
}
