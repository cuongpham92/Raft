package normalOperation;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;

import protocol.UDPProtocol;

public class ClientNode extends Thread {
	private String myIPAddress;
	private int myPort;
	private DatagramSocket unicastSocket;
	private ProcessMessage_latency processMessage_latency;
	private ProcessMessage_throughput processMessage_throughput;
	private long sendTime;
	private long receiveTime;
	private int numOfClients;
	private BufferedWriter log;
	private String module;
	private String command;

	public ClientNode(String myIPAddress, int myPort, String module, int numOfNodes, int numOfClients)
			throws IOException {
		this.myIPAddress = myIPAddress;
		this.myPort = myPort;
		this.unicastSocket = new DatagramSocket(myPort);
		processMessage_latency = new ProcessMessage_latency();
		processMessage_throughput = new ProcessMessage_throughput();
		this.module = module;
		this.numOfClients = numOfClients;
		StringBuilder cmd = new StringBuilder();
		for (int i = 0; i < 1024; i++) {
			cmd.append('a');
		}
		command = cmd.toString();
		if (module.equals("latency")) {
			log = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream("latency_" + numOfNodes, true), "UTF-8"));
		}

		if (module.equals("throughput")) {
			log = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream("throughput_nodes" + numOfNodes + "_clients_" + numOfClients, true), "UTF-8"));
		}

	}

	public void run() {
		if (module.equals("latency")) {
			processMessage_latency.start();
		}
		if (module.equals("throughput")) {
			processMessage_throughput.start();
		}

	}

	public class ProcessMessage_latency extends Thread {
		@Override
		public void run() {
			int requestCount = 0;
			while (true) {
				System.out.println("Client sent request at " + new Date());
				sendTime = System.currentTimeMillis();
				try {
					UDPProtocol.sendUnicastString(unicastSocket, "jmp" + requestCount,
							InetAddress.getByName("192.168.24.50"), 4446);
					DatagramPacket receiveResult = UDPProtocol.receiveUnicast(unicastSocket);
					receiveTime = System.currentTimeMillis();
					log.write(receiveTime - sendTime + "");
					log.newLine();
					log.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				requestCount++;
				if (requestCount == 1000)
					break;
			}
		}
	}

	public class ProcessMessage_throughput extends Thread {
		@Override
		public void run() {
			int numOfTries = 1;
			int recv = 0;
			while (numOfTries <= 1) {
				int sendCount = 0;
				long start = System.nanoTime();
				
				while (System.nanoTime() - start < 1000000000) {
					try {
						if (sendCount < 50) {
							for (int i = sendCount; i < sendCount + numOfClients; i++) {
								System.out.println("Client sent " + i + "requests " + new Date());
								UDPProtocol.sendUnicastString(unicastSocket, command,
										InetAddress.getByName("localhost"), 4446);
							}
							sendCount += numOfClients;
						}
						//DatagramPacket receiveResult = UDPProtocol.receiveUnicast(unicastSocket);
						
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// try {
				// log.write(sendCount + "");
				// log.newLine();
				// log.flush();
				// } catch (IOException e) {
				// // TODO Auto-generated catch block
				// e.printStackTrace();
				// }
				numOfTries++;
			}
		}
	}
}
