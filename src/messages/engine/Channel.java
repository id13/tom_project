package messages.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * This class wraps an end-point of a channel. It allows to send and receive
 * messages, stored in ByteBuffers.
 */
public abstract class Channel implements Comparable<Channel> {

  /**
   * Set the callback to deliver messages to.
   * 
   * @param callback
   */
  public abstract void setDeliverCallback(DeliverCallback callback);

  /**
   * Get the Inet socket address for the other side of this channel.
   * 
   * @return
   * @throws IOException
   */
  public abstract InetSocketAddress getRemoteAddress() throws IOException;

  /**
   * Get the Inet socket address the remote host is using to accept new connection
   * It is used to uniquely identify a remote host.
   * @return
   * @throws IOException
   */
  public abstract InetSocketAddress getRemoteAcceptingAddress() throws IOException;

  
  
  /**
   * Sending the given byte array, a copy is made into internal buffers, so the
   * array can be reused after sending it.
   * 
   * @param bytes
   * @param offset
   * @param length
   * @throws IOException
   */
  public abstract void send(byte[] bytes, int offset, int length) throws IOException;

  public abstract void close();

  /**
   * Attach a Server to a channel which acts as a wrapper around the network
   * connection
   * 
   * @param server
   */
  public abstract void setServer(Server server);

  /**
   * Return the attached Server
   * 
   * @return
   */
  public abstract Server getServer();

  /**
   * Attach a callback to notify when the channel is closed;
   * 
   * @param callback
   */
  public abstract void setClosableCallback(ClosableCallback callback);
  
  /**
   * Attach a callback to notify when the channel is connected (ie accepted by the remote host)
   * @param callback
   */
  public abstract void setConnectCallback(ConnectCallback callback);
  
  /**
   * Attach a callback to notify when the channel is accepted
   * @param callback
   */
  public abstract void setAcceptCallback(AcceptCallback callback);
  
}
