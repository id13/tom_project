package messages.engine;

import java.io.IOException;
import java.net.Inet4Address;
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

public class NioEngine extends Engine {

  private static NioEngine nioEngine = new NioEngine();
  private static Object registerLock = new Object();

  public void wakeUpSelector() {
    this.eventSelector.wakeup();
  }

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
    super.startEcho();
  }

  private void handleGentlyException(Exception ex) {
    System.err.println("NioEngine got an exeption: " + ex.getMessage());
    ex.printStackTrace(System.err);
    System.exit(-1);
  }

  private SelectionKey register(SelectableChannel channel, Object o, int intentions) {
    SelectionKey key = null;
    try {
      synchronized (NioEngine.registerLock) {
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
    synchronized (registerLock) {
      key.cancel();
    }
  }

  @Override
  public void mainloop() {
    long delay = 500;
    try {
      for (;;) {
        synchronized (registerLock) {
        }
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
              Messenger messenger = (Messenger) subject;
              ServerSocketChannel sc = (ServerSocketChannel) key.channel();
              SocketChannel socket = sc.accept();
              socket.configureBlocking(false);
              socket.socket().setTcpNoDelay(true);
              NioChannel channel = new NioChannel(socket);
              NioServer server = new NioServer(socket.socket().getLocalPort(), socket,
                  this.register(socket, channel, SelectionKey.OP_READ));
              channel.setDeliverCallback(messenger);
              channel.setAcceptCallback(messenger);
              channel.setServer(server);
              channel.sendHello(messenger.getAcceptAddress());
              super.acceptCount++;
            } else if (key.isReadable()) {
              ReceiveCallback receiver = (ReceiveCallback) subject;
              receiver.handleReceive();
              super.readCount++;
            } else if (key.isWritable()) {
              WriteCallback writer = (WriteCallback) subject;
              writer.handleWrite();
              super.writeCount++;
            } else if (key.isConnectable()) {
              Messenger messenger = (Messenger) subject;
              SocketChannel socket = (SocketChannel) key.channel();
              socket.configureBlocking(false);
              socket.socket().setTcpNoDelay(true);
              socket.finishConnect();
              NioChannel channel = new NioChannel(socket);
              channel.setDeliverCallback(messenger);
              channel.setConnectCallback(messenger);
              Server nioServer = new NioServer(socket.socket().getLocalPort(), socket,
                  this.register(socket, channel, SelectionKey.OP_READ));
              channel.setServer(nioServer);
              channel.sendHello(messenger.getAcceptAddress());
              super.connectCount++;
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
    InetAddress address = null;
    socket.configureBlocking(false);
    InetSocketAddress isa = new InetSocketAddress(address, port);
    socket.bind(isa);
    return new NioServer(port, socket, this.register(socket, callback, SelectionKey.OP_ACCEPT));
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
