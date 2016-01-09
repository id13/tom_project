package tom.main;

import tom.Peer;
import tom.PeerImpl;

public class MainThree {

	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
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
						peer3.send("Bonjour");
					} catch (InterruptedException e) {
						System.out.println("Interrupted");
					}
				}
			}
		};
		Thread t = new Thread(runnable);
		t.start();

	}

}
