package tom.main;

import tom.Peer;
import tom.PeerImpl;

public class MainOne {

	public static void main(String[] args) {
		
		System.setProperty("java.net.preferIPv4Stack", "true");
		Callback callback1 = new Callback("peer1");
		Peer peer1 = new PeerImpl(12380, callback1);
/*		Callback callback2 = new Callback("peer2");
		Peer peer2 = new PeerImpl(12381, callback2);
		peer2.connect(12380);
		Callback callback3 = new Callback("peer3");
		Peer peer3 = new PeerImpl(12382, callback3);
		peer3.connect(12380);
		peer3.connect(12381);

		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				while (!Thread.interrupted()) {
					try {
						Thread.sleep(1000);
						peer1.send("Bonjour");
					} catch (InterruptedException e) {
						System.out.println("Interrupted");
					}
				}
			}
		};
		Thread t = new Thread(runnable);
		t.start();*/
	}
}
