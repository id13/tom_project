package messages.engine.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;

import messages.engine.AcceptCallback;
import messages.engine.ConnectCallback;
import messages.engine.Engine;
import messages.engine.ReceiveCallback;
import messages.engine.Server;
import messages.engine.WriteCallback;
import messages.service.Peer;

public class NioEngine extends Engine {

  private static NioEngine nioEngine = new NioEngine();
  private static Object registerLock = new Object();
  
  public static NioEngine getNioEngine() {
    return nioEngine;
  }
  
  private Selector eventSelector;
  
  private NioEngine() {
    super();
    try {
      this.eventSelector = SelectorProvider.provider().openSelector();
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic(e.getMessage());
    }
  }
  
  private void handleGentlyException(Exception ex) {
    System.err.println("NioEngine got an exeption: " + ex.getMessage());
    ex.printStackTrace(System.err);
    System.exit(-1);    
  }

  private SelectionKey register(SelectableChannel channel, Object o, int intentions) {
    SelectionKey key = null;
    try {
      synchronized(NioEngine.registerLock) {
        this.eventSelector.wakeup();
        key = channel.register(this.eventSelector, intentions); 
      }
    } catch (ClosedChannelException e) {
      this.handleGentlyException(e);
    }
    key.attach(o);
    return key;
  }
  
  public void cancelKey(SelectionKey key) {
    synchronized(registerLock) {
      key.cancel();
    }
  }
  
  @Override
  public void mainloop() {
    long delay = 500;
    try {
      for (;;) {
        synchronized(registerLock) {}
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
              NioChannel channel = new NioChannel(socket);
              NioServer server = new NioServer(
                  socket.socket().getLocalPort(), 
                  socket, 
                  this.register(socket, channel, SelectionKey.OP_READ));
              channel.setDeliverCallback(peer);
              channel.setClosableCallback(peer);
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
              channel.setDeliverCallback(peer);
              channel.setClosableCallback(peer);
              Server nioServer = new NioServer(
                  socket.socket().getLocalPort(), 
                  socket, 
                  this.register(socket, channel, SelectionKey.OP_READ));
              channel.setServer(nioServer);
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
    ServerSocketChannel socket = ServerSocketChannel.open();
    socket.configureBlocking(false);
    InetSocketAddress isa = new InetSocketAddress("localhost", port);
    socket.bind(isa);
    return new NioServer(port, socket, 
        this.register(socket, callback, SelectionKey.OP_ACCEPT));
  }

  @Override
  public void connect(InetAddress hostAddress, int port, ConnectCallback callback)
      throws UnknownHostException, SecurityException, IOException {
    SocketChannel socket = SocketChannel.open();
    socket.configureBlocking(false);
    socket.socket().setTcpNoDelay(true);
    this.register(socket, callback, SelectionKey.OP_CONNECT);
    socket.connect(new InetSocketAddress(hostAddress, port));
  }

}
