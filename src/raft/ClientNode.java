package raft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import protocol.UDPProtocol;

public class ClientNode extends Thread {
	private String myIPAddress;
	private int myPort;
	private DatagramSocket unicastSocket;
	private SendRequest sendRequest;
	private ProcessMessage processMessage;
	private int requestCount;

	public ClientNode(String myIPAddress, int myPort) throws IOException {
		this.myIPAddress = myIPAddress;
		this.myPort = myPort;
		this.unicastSocket = new DatagramSocket(myPort);
		sendRequest = new SendRequest();
		processMessage = new ProcessMessage();
		requestCount = 0;
	}

	public void run() {
		sendRequest.start();
		processMessage.start();
	}

	public class SendRequest extends Thread {
		@Override
		public void run() {
			try {
				while (requestCount < 3) {
					System.out.println("Client sent request at " + new Date());
					UDPProtocol.sendUnicastString(unicastSocket, "jmp" + requestCount, InetAddress.getByName("localhost"), 4446);
					Thread.sleep(1000);
					requestCount ++;
				}
			} catch (UnknownHostException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class ProcessMessage extends Thread {
		@Override
		public void run() {
			int receivedRequestCount = 0;
			while (true) {
				DatagramPacket receiveResult = UDPProtocol.receiveUnicast(unicastSocket);
				System.out.println(new String(receiveResult.getData()).trim());
				receivedRequestCount ++;
				if(receivedRequestCount == 10) break;
			}
		}
	}
}
