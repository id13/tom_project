package tom;

/**
 * This exception is thrown when the user calls the connect method while the
 * peer is currently connecting to a group or already connected to a group of
 * more than one member.
 *
 */
public class ConnectException extends Exception {

  public ConnectException(String string) {
    super(string);
  }
}
