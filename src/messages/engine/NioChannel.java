package messages.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioChannel extends Channel {
  
  private SelectableChannel channel;
  private SelectionKey selectionKey;

  private NioChannel() {
    // Nothing to do
  }
  
  public NioChannel(SelectableChannel channel) {
    this.channel = channel;
  }
  /*public static NioChannel createNioChannel(String hostname, int port) throws IOException {
    NioChannel channel = new NioChannel();
    channel.socket = SocketChannel.open();
    channel.socket.configureBlocking(false);
    channel.socket.socket().setTcpNoDelay(true);

    
    // be notified when the connection to the server will be accepted
    // m_key = m_ch.register(m_selector, SelectionKey.OP_CONNECT);
    channel.selectionKey = NioEngine.getNioEngine().register(m_ch, this, SelectionKey.OP_CONNECT);

    // request to connect to the server
    channel.socket.connect(new InetSocketAddress(hostname, port));

    
    
    
    return channel;
    
  }*/
  
  @Override
  public void setDeliverCallback(DeliverCallback callback) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void send(byte[] bytes, int offset, int length) {
    //ByteBuffer[] buffer;
    // TODO Auto-generated method stub
    //socket.write(buffer, offset, length);
    
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
    
  }
}
