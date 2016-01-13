package messages.engine.burstTest;

import java.net.InetSocketAddress;

import messages.callbacks.DeliverCallback;

public class DeliverCallbackImpl implements DeliverCallback {

  
  
  @Override
  public void delivered(InetSocketAddress from, byte[] content) {
    System.out.println("message delivered from " + from.toString() + " : " + new String(content));
  }

}
