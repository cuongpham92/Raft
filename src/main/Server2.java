package main;

import java.io.IOException;

import normalOperation.ClientNode;
import normalOperation.ServerNode_ForLatency;
import normalOperation.ServerNode_ForThroughput;

public class Server2 {
	public static void main(String[] args) throws IOException {
		ServerNode_ForThroughput serverNode2 = new ServerNode_ForThroughput("localhost", 4448, 4449, 3, 50);
		serverNode2.start();
	}
}
