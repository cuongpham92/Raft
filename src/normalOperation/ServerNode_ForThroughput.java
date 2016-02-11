package normalOperation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import entity.Server;
import protocol.UDPProtocol;
import resource.AckMessage;
import resource.CommitMessage;
import resource.FrameMessage;
import resource.Log;

public class ServerNode_ForThroughput extends Thread {
	private Server me;
	private DatagramSocket socketforClient;
	private DatagramSocket socketforServer;
	private List<Server> listServers;
	private Boolean isLeader;
	private List<String> listCommands;
	private int numOfClients;
	private int firstIndex;
	private int numOfProcessedCommands;
	// local experiment, different servers distinguish from each other by their
	// ports
	private Map<String, Boolean> listAcks;

	public ServerNode_ForThroughput(String myIPAddress, int myPortforClient, int myPortforServer, int numOfNodes,
			int numOfClients) throws IOException {
		me = new Server(myIPAddress, myPortforClient, myPortforServer);
		this.socketforClient = new DatagramSocket(myPortforClient);
		this.socketforServer = new DatagramSocket(myPortforServer);
		this.socketforServer.setSoTimeout(5000);
		isLeader = false;
		listCommands = new ArrayList<>();
		this.numOfClients = numOfClients;
		this.firstIndex = 0;
		this.numOfProcessedCommands = 0;
		/*if (myIPAddress.equals("localhost")) {
			isLeader = true;
		}*/
		if (myPortforServer == 4447) {
			isLeader = true;
		}
		setTopology(numOfNodes);
	}

	public void setTopology(int numOfNodes) {
		final Server server1 = new Server("localhost", 4446, 4447);
		final Server server2 = new Server("localhost", 4448, 4449);
		final Server server3 = new Server("localhost", 4450, 4451);
		final Server server4 = new Server("localhost", 4453, 4452);
		final Server server5 = new Server("localhost", 4454, 4455);

		if (numOfNodes == 1) {
			listServers = new ArrayList<Server>() {
				{
					add(server1);
				}
			};
		} else if (numOfNodes == 2) {
			listServers = new ArrayList<Server>() {
				{
					add(server1);
					add(server2);
				}
			};
		} else if (numOfNodes == 3) {
			listServers = new ArrayList<Server>() {
				{
					add(server1);
					add(server2);
					add(server3);
				}
			};
		} else if (numOfNodes == 4) {
			listServers = new ArrayList<Server>() {
				{
					add(server1);
					add(server2);
					add(server3);
					add(server4);
				}
			};
		} else if (numOfNodes == 5) {
			listServers = new ArrayList<Server>() {
				{
					add(server1);
					add(server2);
					add(server3);
					add(server4);
					add(server5);
				}
			};
		}
	}

	public void sendAppendEntryLeaderSide(List<String> listRequests, int firstIndex) {
		try {
			FrameMessage frameMessage;
			// check if it is the first request from client
			if (me.getListLog().size() == 0) {
				me.setTerm(1);
				for (String s : listRequests) {
					Log newLog = new Log(0, me.getTerm(), s, "uncommitted");
					me.getListLog().add(newLog);
				}
			} else {
				for (String s : listRequests) {
					Log newLog = new Log(me.getListLog().size(), me.getTerm(), s, "uncommited");
					me.getListLog().add(newLog);
				}
			}

			frameMessage = new FrameMessage(me.getListLog().subList(firstIndex, firstIndex + listRequests.size()),
					firstIndex);

			if (listServers.size() > 1) {
				for (Server s : listServers) {
					if (!me.equals(s)) {
						UDPProtocol.sendUnicastObject(socketforServer, frameMessage,
								InetAddress.getByName(s.getIpAddress()), s.getPortforServer());
						System.out.println("process" + me.getIpAddress() + " sends " + listRequests.size()
								+ " messages with the first index is " + firstIndex + " to process" + s.getIpAddress()
								+ " at " + new Date());
					}

				}
			} else {
				UDPProtocol.sendUnicastString(socketforClient, "done", InetAddress.getByName("localhost"), 4445);
				numOfProcessedCommands ++;
				System.out.println("number of processed commands1: " + numOfProcessedCommands);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processAckLeaderSide() {
		int ackCount = 0;
		try {
			while (true) {
				byte[] receiveData = new byte[1024];
				DatagramPacket receiveAck = new DatagramPacket(receiveData, receiveData.length);
				socketforServer.receive(receiveAck);
				ByteArrayInputStream bais = new ByteArrayInputStream(receiveData);
				ObjectInputStream ois = new ObjectInputStream(bais);
				AckMessage ack = (AckMessage) ois.readObject();
				if (ack.getLogIndex() == me.getListLog().size() - numOfClients && ack.getTerm() == me.getTerm()) {
					System.out.println("process" + me.getIpAddress() + "received ack from process"
							+ receiveAck.getAddress().getHostAddress() + " at " + new Date());
					ackCount++;
					if (ackCount >= listServers.size() / 2) {
						System.out.println("process" + me.getIpAddress() + " received " + ackCount + " acks for log "
								+ ack.getLogIndex() + "-" + ". Commit this log and send commit message");
						for (int i = me.getListLog().size() - numOfClients; i < me.getListLog().size(); i++) {
							me.getListLog().get(i).setState("committed");
						}
						UDPProtocol.sendUnicastString(socketforClient, "done", InetAddress.getByName("localhost"),
								4445);
						numOfProcessedCommands ++;
						System.out.println(numOfProcessedCommands);
						for (Server s : listServers) {
							if (!me.equals(s)) {
								UDPProtocol.sendUnicastObject(socketforServer,
										new CommitMessage(ack.getLogIndex(), ack.getTerm()),
										InetAddress.getByName(s.getIpAddress()), s.getPortforServer());
							}
						}

						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void followerSide() {
		while (true) {
			try {
				byte[] receiveData = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				socketforServer.receive(receivePacket);
				ByteArrayInputStream bais = new ByteArrayInputStream(receiveData);
				ObjectInputStream ois = new ObjectInputStream(bais);
				Object obj = ois.readObject();
				if (obj instanceof FrameMessage) {
					FrameMessage frameMes = (FrameMessage) obj;
					if (me.getListLog().size() == frameMes.getFirstIndex()
							&& frameMes.getListLogs().size() == numOfClients) {
						System.out.println("process" + me.getIpAddress() + " append " + frameMes.getListLogs().size()
								+ " messages with the first index is " + frameMes.getFirstIndex() + " at "
								+ new Date());
						for (Log l : frameMes.getListLogs()) {
							me.getListLog().add(l);
						}
					}
					UDPProtocol.sendUnicastObject(socketforServer,
							new AckMessage(frameMes.getFirstIndex(), frameMes.getListLogs().get(0).getTerm()),
							receivePacket.getAddress(), receivePacket.getPort());
				}

				if (obj instanceof CommitMessage) {
					CommitMessage commitMes = (CommitMessage) obj;
					for (int i = commitMes.getLogIndex(); i < commitMes.getLogIndex() + numOfClients; i++) {
						me.getListLog().get(i).setState("committed");
					}
					System.out.println("Process " + me.getIpAddress() + ": Committed " + numOfClients
							+ " logs with first index is " + commitMes.getLogIndex());
				}

//			} catch (SocketTimeoutException e) {
//				// e.printStackTrace();
//				System.out.println("Process" + me.getIpAddress() + ": socket timeout exception, elect new leader at "
//						+ new Date());
			} catch (Exception e) {
				System.out.println("Process" + me.getIpAddress() + " waiting");
			}
		}

	}

	public void run() {
		// leader part
		if (isLeader) {
			System.out.println("I am the leader");
			String command = "";
			CommunicationBetweenServers communicationBetweenServers = new CommunicationBetweenServers();
			communicationBetweenServers.start();
			int  x  = 0;
			while (true) {
				DatagramPacket receivePacket = UDPProtocol.receiveUnicast(socketforClient);
				//System.out.println("message size: " + receivePacket.getData().length);
				command = new String(receivePacket.getData()).trim();
				//System.out.println(command);
				//System.out.println("process" + me.getIpAddress() + " received command=" + command + " from client at "
				//		+ new Date());
				System.out.println(x++);
				synchronized (listCommands) {
					listCommands.add(command);
					System.out.println("list command size: " + listCommands.size());

					
				}
			}
		}
		// follower part
		else {
			System.out.println("I am the follower at " + new Date());
			followerSide();
		}
	}

	private class CommunicationBetweenServers extends Thread {
		@SuppressWarnings("null")
		@Override
		public void run() {
			while (true) {
				List<String> listRequests = null;
				synchronized (listCommands) {
					if (listCommands.size() != 0) {
						if (listCommands.size() - firstIndex > 0) {
							listRequests = listCommands.subList(firstIndex, firstIndex + numOfClients > listCommands.size() ? listCommands.size() : firstIndex + numOfClients);
							System.out.println("Process" + me.getIpAddress() + " starts sending " + listRequests.size()
									+ " messages with first index is " + firstIndex + " at " + new Date());
							sendAppendEntryLeaderSide(listRequests, firstIndex);
							if (listServers.size() > 1) {
								processAckLeaderSide();
							}
							firstIndex += listRequests.size();
						}
					}
				}
				
			}
		}
	}

}
