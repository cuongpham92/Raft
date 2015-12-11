package main;

import java.io.IOException;

import normalOperation.ServerNode_ForLatency;
import normalOperation.ServerNode_ForThroughput;

public class Server {
	public static void main(String[] args) throws IOException {
//		String ipServer = args[0];
//		String numOfNodes = args[1];
//		ServerNode_ForLatency serverNode = new ServerNode_ForLatency(ipServer, 4446, 4447, Integer.parseInt(numOfNodes));
		String module = args[0];
		if(module.equals("latency")) {
			ServerNode_ForLatency serverNode1 = new ServerNode_ForLatency("localhost", 4446, 4447, 1);
			serverNode1.start();
		} 
		if(module.equals("throughput")) {
			ServerNode_ForThroughput serverNode1 = new ServerNode_ForThroughput("localhost", 4446, 4447, 1, 1);
			serverNode1.start();
		}
	}
}
