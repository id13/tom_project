package tom.main;

import tom.TomDeliverCallback;

public class Callback implements TomDeliverCallback {

	private String name;

	public Callback(String name) {
		this.name = name;
	}

	@Override
	public void deliver(String message) {
		System.out.println("Delivered by TOM layer: " + message);

	}
}
