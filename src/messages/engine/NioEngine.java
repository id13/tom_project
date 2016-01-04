package messages.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioEngine extends Engine {

  private static NioEngine nioEngine = new NioEngine();
  
  public static NioEngine getNioEngine() {
    return nioEngine;
  }
  
  private Selector eventSelector;
  
  public NioEngine() {
  }
  
  private void handleGentlyException(Exception ex) {
    System.err.println("NioEngine got an exeption: " + ex.getMessage());
    ex.printStackTrace(System.err);
    System.exit(-1);    
  }

  private SelectionKey register(SelectableChannel channel, Object o, int intentions) {
    SelectionKey key = null;
    try {
      key = channel.register(this.eventSelector, intentions);
    } catch (ClosedChannelException e) {
      this.handleGentlyException(e);
    }
    key.attach(o);
    return key;
  }
  
  @Override
  public void mainloop() {
    long delay = 0;
    try {
      for (;;) {
        eventSelector.select(delay);
        Iterator<?> selectedKeys = this.eventSelector.selectedKeys().iterator();
        if (selectedKeys.hasNext()) {
          SelectionKey key = (SelectionKey) selectedKeys.next();
          selectedKeys.remove();
          if (!key.isValid()) {
            continue;
          } else {
            Object subject = key.attachment();
            if (key.isAcceptable()) {
              Peer peer = (Peer)subject;
              ServerSocketChannel sc = (ServerSocketChannel) key.channel();
              SocketChannel socket = sc.accept();
              socket.configureBlocking(false);
              socket.socket().setTcpNoDelay(true);
              NioServer server = new NioServer(socket.socket().getLocalPort());
              NioChannel channel = new NioChannel(socket);
              channel.setSelectionKey(
                  this.register(socket, channel, SelectionKey.OP_READ));
              channel.setDeliverCallback(peer);
              peer.accepted(server, channel);
            } else if (key.isReadable()) {
              ReceiveCallback receiver = (ReceiveCallback)subject;
              receiver.handleReceive();
            } else if (key.isWritable()) {
              WriteCallback writer = (WriteCallback)subject;
              writer.handleWrite();
            } else if (key.isConnectable()) {
              Peer peer = (Peer)subject;
              SocketChannel socket = (SocketChannel) key.channel();
              socket.configureBlocking(false);
              socket.socket().setTcpNoDelay(true);
              socket.finishConnect();
              NioChannel channel = new NioChannel(socket);
              channel.setSelectionKey(
                  this.register(socket, channel, SelectionKey.OP_READ));
              channel.setDeliverCallback(peer);              
              peer.connected(channel);
            }
          }
        }
      }
    } catch (Exception ex) {
      this.handleGentlyException(ex);
    }    
  }

  @Override
  public Server listen(int port, AcceptCallback callback) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void connect(InetAddress hostAddress, int port, ConnectCallback callback)
      throws UnknownHostException, SecurityException, IOException {
    // TODO Auto-generated method stub

  }

}
