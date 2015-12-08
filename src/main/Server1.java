package main;

import java.io.IOException;

import raft.ClientNode;
import raft.ServerNode;

public class Server1 {
	public static void main(String[] args) throws IOException {
		ClientNode client = new ClientNode("localhost", 4445);
		ServerNode serverNode1 = new ServerNode("localhost", 4446, 4447);
		ServerNode serverNode2 = new ServerNode("localhost", 4448, 4449);
		ServerNode serverNode3 = new ServerNode("localhost", 4450, 4451);
		ServerNode serverNode4 = new ServerNode("localhost", 4452, 4453);
		serverNode2.start();
		serverNode3.start();
		serverNode1.start();
		serverNode4.start();
		client.start();
	}
}
