package main;

import java.io.IOException;

import raft.ClientNode;
import raft.ServerNode;

public class Client {
	public static void main(String[] args) throws IOException {
		ClientNode client = new ClientNode("localhost", 4445);
		client.start();
		
	}
}
