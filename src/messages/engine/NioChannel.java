package messages.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

import messages.util.ByteUtil;

public class NioChannel extends Channel implements ReceiveCallback, WriteCallback {

  private static final int DISCONNECTED = 0;
  private static final int CONNECTED = 1;
  private static final int NOT_SENDING = 10;
  private static final int SENDING = 11;
  private static final int READING_LENGTH = 21;
  private static final int READING_MESSAGE = 22;
  private static final int READING_CHECKSUM = 23;

  private static final int MESSAGE = 30;
  private static final int HELLO = 31;

  private SocketChannel channel;
  private SelectionKey key;
  private ByteBuffer receiveBuffer;
  private DeliverCallback deliverCallback;
  private NioServer server;
  private Integer state;
  private Integer sendingState;
  private Integer receivingState;
  private ClosableCallback closableCallback;
  private AcceptCallback acceptCallback;
  private ConnectCallback connectCallback;
  private InetSocketAddress remoteLocalAddress;
  private InetSocketAddress remoteAcceptingAddress;
  private ConcurrentLinkedQueue<ByteBuffer> dataToSend;
  private byte[] currentMessage;

  public NioChannel(SocketChannel channel) throws IOException {
    this.channel = channel;
    this.receiveBuffer = ByteBuffer.allocate(4);
    this.state = NioChannel.CONNECTED;
    this.sendingState = NOT_SENDING;
    this.receivingState = READING_LENGTH;
    this.dataToSend = new ConcurrentLinkedQueue<>();
    this.remoteLocalAddress = (InetSocketAddress) (channel.getRemoteAddress());
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
    this.send(bytes, offset, length, MESSAGE);
  }

  private void send(byte[] bytes, int offset, int length, int type) {
    synchronized (sendingState) {
      ByteBuffer sendBuffer;
      int lengthWithType = length + 4;
      if (state != CONNECTED)
        return;
      sendingState = NioChannel.SENDING;
      byte[] messageWithType = new byte[lengthWithType];
      ByteUtil.writeInt32(messageWithType, 0, type);
      System.arraycopy(bytes, 0, messageWithType, 4, length);
      // the message is composed of
      // LENGTH (4 bytes) | TYPE (4 bytes) | CONTENT | CRC (8 bytes)
      sendBuffer = ByteBuffer.allocate(4 + lengthWithType + 8);
      sendBuffer.putInt(lengthWithType);
      sendBuffer.put(messageWithType);
      sendBuffer.putLong(ByteUtil.computeCRC32(messageWithType));
      sendBuffer.position(0);
      this.dataToSend.add(sendBuffer);
      this.key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
      NioEngine.getNioEngine().wakeUpSelector();
    }
  }

  public void sendHello(InetSocketAddress address) {
    byte[] content = new byte[8];
    ByteUtil.writeInetSocketAddress(content, 0, address);
    this.send(content, 0, 8, HELLO);
  }
  
  @Override
  public void handleWrite() throws IOException {
    if (state != CONNECTED)
      return;
    synchronized (sendingState) {
      ByteBuffer sendBuffer = dataToSend.peek();
      if (sendingState != SENDING) {
        Engine.panic("handleWrite: fall into unexpected state, expected SENDING");
      }
      int count = channel.write(sendBuffer);
      if (count == -1) {
        // According to the nio doc, -1 means the channel is closed, so we
        // notify
        // the closableCallback
        this.close();
        return;
      }
      if (sendBuffer.remaining() == 0) {
        this.dataToSend.poll();
      }
      if (dataToSend.isEmpty()) {
        sendingState = NOT_SENDING;
        this.key.interestOps(SelectionKey.OP_READ);
      }
    }
  }

  @Override
  public void close() {
    try {
      synchronized (state) {
        this.server.close();
        this.state = DISCONNECTED;
        this.closableCallback.closed(this);
      }
    } catch (IOException e) {
      e.printStackTrace();
      Engine.panic("can not close channel");
    }
  }

  
  
  private void deliver(byte[] bytes) {
    int type = ByteUtil.readInt32(bytes, 0);
    byte [] content = new byte[bytes.length - 4];
    System.arraycopy(bytes, 4, content, 0, content.length);
    if(type == MESSAGE) {
      this.deliverCallback.deliver(this, content);
    } else if(type == HELLO) {
      try {
        this.remoteAcceptingAddress = ByteUtil.readInetSocketAddress(content, 0);
        if(this.connectCallback != null) {
          this.connectCallback.connected(this);
        } else {
          this.acceptCallback.accepted(this.server, this);
        }
      } catch (UnknownHostException e) {
        e.printStackTrace();
        Engine.panic(e.getMessage());
      }
    }
  }

  @Override
  public void setServer(Server server) {
    if (!(server instanceof NioServer))
      Engine.panic("setServer: NioChannel MUST use ONLY NioServer");
    this.server = (NioServer) server;
    this.key = this.server.getSelectionKey();
  }

  @Override
  public void handleReceive() throws IOException {
    if (state != CONNECTED)
      return;
    synchronized (this.receivingState) {
      int len, count = 0;
      long checksum;
      switch (this.receivingState) {
      case READING_LENGTH:
        this.currentMessage = null;
        count = channel.read(receiveBuffer);
        if (count == -1) {
          // According to the nio doc, -1 means the channel is closed, so we
          // notify
          // the closableCallback
          this.close();
          return;
        }
        if (receiveBuffer.hasRemaining())
          return;
        receivingState = READING_MESSAGE;
        receiveBuffer.position(0);
        len = receiveBuffer.getInt();
        receiveBuffer = ByteBuffer.allocate(len);
      case READING_MESSAGE:
        count = channel.read(receiveBuffer);
        if (count == -1) {
          // According to the nio doc, -1 means the channel is closed, so we
          // notify
          // the closableCallback
          this.close();
          return;
        }
        if (receiveBuffer.hasRemaining())
          return;

        receiveBuffer.position(0);
        this.currentMessage = new byte[receiveBuffer.remaining()];
        receiveBuffer.get(this.currentMessage);
        receiveBuffer = ByteBuffer.allocate(8);
        this.receivingState = READING_CHECKSUM;
      case READING_CHECKSUM:
        count = channel.read(receiveBuffer);
        if (count == -1) {
          // According to the nio doc, -1 means the channel is closed, so we
          // notify
          // the closableCallback
          this.close();
          return;
        }
        if (receiveBuffer.hasRemaining())
          return;
        receiveBuffer.position(0);
        checksum = receiveBuffer.getLong();
        long checksumToCompare = ByteUtil.computeCRC32(this.currentMessage);
        if (!new Long(checksum).equals(new Long(checksumToCompare)))
          Engine.panic("CRC checksum error");
        this.receiveBuffer = ByteBuffer.allocate(4);
        this.receivingState = READING_LENGTH;
        this.deliver(this.currentMessage);
        return;
      default:
        Engine.panic("handleReceive: falling into unexpected state");
      }
    }
  }

  @Override
  public int compareTo(Channel o) {
    if (o.getServer() == null) {
      Engine.panic("attached server to a channel MUST not be null");
    }
    return new Integer(this.server.getPort()).compareTo(new Integer(o.getServer().getPort()));
  }

  @Override
  public Server getServer() {
    return this.server;
  }
 
  @Override
  public void setAcceptCallback(AcceptCallback callback) {
    this.acceptCallback = callback;
    this.setClosableCallback(callback);
  }
  
  @Override
  public void setConnectCallback(ConnectCallback callback) {
    this.connectCallback = callback;
    this.setClosableCallback(callback);
  }
  
  @Override
  public void setClosableCallback(ClosableCallback callback) {
    this.closableCallback = callback;
  }

  @Override
  public InetSocketAddress getRemoteAcceptingAddress() throws IOException {
    return this.remoteAcceptingAddress;
  }

}
