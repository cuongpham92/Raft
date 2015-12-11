package main;

import java.io.IOException;

import normalOperation.ClientNode;
import normalOperation.ServerNode_ForLatency;
import normalOperation.ServerNode_ForThroughput;

public class Server3 {
	public static void main(String[] args) throws IOException {
		ServerNode_ForThroughput serverNode3 = new ServerNode_ForThroughput("localhost", 4450, 4451, 3, 2);
		serverNode3.start();	
	}
}
