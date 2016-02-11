package main;

import java.io.IOException;

import normalOperation.ClientNode;


public class Client {
	public static void main(String[] args) throws IOException {
		//String ipClient = args[0];
		ClientNode client = new ClientNode("localhost", 4445, "throughput", 1, 50);
		client.start();
	}
}
