package tom;

public interface TomJoinCallback {

  /**
   * Attach a callback to notify that a peer has successfully joined a tom group
   * requested before
   * @param peer
   */
  public void joined(Peer peer);
  
}
