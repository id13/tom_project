package tom.main;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import tom.Peer;
import tom.PeerImpl;

public class MainTwo {

	public static void main(String[] args) throws UnknownHostException {
		System.setProperty("java.net.preferIPv4Stack", "true");
		Callback callback2 = new Callback("peer2");
		InetAddress myIpAddress = Inet4Address.getLocalHost();
		InetSocketAddress myAddress = new InetSocketAddress(myIpAddress, 12381);
		Peer peer2 = new PeerImpl(myAddress, callback2);
		peer2.connect(12380);
	}

}
