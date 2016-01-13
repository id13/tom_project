package messages.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
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

  private SocketChannel channel;
  private SelectionKey key;
  private ByteBuffer receiveBuffer;
  private DeliverCallback deliverCallback;
  private NioServer server;
  private Integer state;
  private Integer sendingState;
  private Integer receivingState;
  private ClosableCallback closableCallback;
  private InetSocketAddress remoteLocalAddress;
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
    synchronized (sendingState) {
      ByteBuffer sendBuffer;
      if (state != CONNECTED)
        return;
      sendingState = NioChannel.SENDING;
      sendBuffer = ByteBuffer.allocate(4 + length + 8);
      sendBuffer.putInt(length);
      sendBuffer.put(bytes, offset, length);
      sendBuffer.putLong(ByteUtil.computeCRC32(bytes));
      sendBuffer.position(0);
      this.dataToSend.add(sendBuffer);
      this.key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
      NioEngine.getNioEngine().wakeUpSelector();
    }
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
        this.deliverCallback.deliver(this, this.currentMessage);
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
  public void setClosableCallback(ClosableCallback callback) {
    this.closableCallback = callback;
  }
}
