package tom.main;

import tom.Peer;
import tom.PeerImpl;

public class MainTwo {

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
		Callback callback2 = new Callback("peer2");
		Peer peer2 = new PeerImpl(12381, callback2);
		peer2.connect(12380);
	}

}
