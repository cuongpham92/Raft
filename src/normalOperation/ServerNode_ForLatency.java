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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.Server;
import protocol.UDPProtocol;
import resource.AckMessage;
import resource.AppendEntry;
import resource.CommitMessage;
import resource.Log;

public class ServerNode_ForLatency extends Thread {
	private Server me;
	private DatagramSocket socketforClient;
	private DatagramSocket socketforServer;
	private List<Server> listServers;
	private Boolean isLeader;
	private List<String> listCommands;
	// local experiment, different servers distinguish from each other by their
	// ports
	private Map<String, Boolean> listAcks;

	public ServerNode_ForLatency(String myIPAddress, int myPortforClient, int myPortforServer, int numOfNodes) throws IOException {
		me = new Server(myIPAddress, myPortforClient, myPortforServer);
		this.socketforClient = new DatagramSocket(myPortforClient);
		this.socketforServer = new DatagramSocket(myPortforServer);
		this.socketforServer.setSoTimeout(5000);
		isLeader = false;
		listCommands = new ArrayList<>();
		if (myIPAddress.equals("192.168.24.50")) {
			isLeader = true;
		}
		setTopology(numOfNodes);
	}

	public void setTopology(int numOfNodes) {
		final Server server1 = new Server("192.168.24.50", 4446, 4447);
		final Server server2 = new Server("192.168.24.51", 4446, 4447);
		final Server server3 = new Server("192.168.24.52", 4446, 4447);
		final Server server4 = new Server("192.168.24.53", 4446, 4447);
		final Server server5 = new Server("192.168.24.54", 4446, 4447);

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

	public void sendAppendEntryLeaderSide(String command) {
		try {
			AppendEntry appendEntry;
			// check if it is the first request from client
			if (me.getListLog().size() == 0) {
				me.setTerm(1);
				Log newLog = new Log(0, me.getTerm(), command, "uncommitted");
				me.getListLog().add(newLog);
				appendEntry = new AppendEntry(newLog, null);

			} else {
				System.out.println("aaaa at " + new Date());
				Log newLog = new Log(me.getListLog().size(), me.getTerm(), command, "uncommited");
				Log precedingLog = me.getListLog().get(me.getListLog().size() - 1);
				me.getListLog().add(newLog);
				appendEntry = new AppendEntry(newLog, precedingLog);
			}
			if (listServers.size() > 1) {
				for (Server s : listServers) {
					if (!s.getIpAddress().equals(me.getIpAddress())) {
						UDPProtocol.sendUnicastObject(socketforServer, appendEntry,
								InetAddress.getByName(s.getIpAddress()), s.getPortforServer());
						System.out.println("process" + me.getIpAddress() + " sends command= " + command + " to process"
								+ s.getIpAddress() + " at " + new Date());
					}

				}
			} else {
				UDPProtocol.sendUnicastString(socketforClient, "done",
						InetAddress.getByName("192.168.24.49"), 4445);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processAckLeaderSide() {
		int ackCount = 0;
		try {
			listAcks = new HashMap<String, Boolean>() {
				{
					for (Server s : listServers) {
						put(s.getIpAddress(), false);
					}
				}
			};
			while (true) {
				try {
					byte[] receiveData = new byte[1024];
					DatagramPacket receiveAck = new DatagramPacket(receiveData, receiveData.length);
					socketforServer.receive(receiveAck);
					ByteArrayInputStream bais = new ByteArrayInputStream(receiveData);
					ObjectInputStream ois = new ObjectInputStream(bais);
					AckMessage ack = (AckMessage) ois.readObject();
					if (ack.getLogIndex() == me.getListLog().size() - 1
							&& ack.getTerm() == me.getListLog().get(me.getListLog().size() - 1).getTerm()) {
						System.out.println("process" + me.getIpAddress() + "received ack from process"
								+ receiveAck.getAddress().getHostAddress() + " at " + new Date());
						listAcks.put(receiveAck.getAddress().getHostAddress(), true);
						ackCount++;
						if (ackCount >= listServers.size() / 2) {
							System.out.println("process" + me.getIpAddress() + " received " + ackCount
									+ " acks for log " + ack.getLogIndex() + "-" + ack.getTerm()
									+ ". Commit this log and send commit message");
							me.getListLog().get(ack.getLogIndex()).setState("committed");
							UDPProtocol.sendUnicastString(socketforClient, "done",
									InetAddress.getByName("192.168.24.49"), 4445);
							for (Server s : listServers) {
								if (!s.getIpAddress().equals(me.getIpAddress())) {
									UDPProtocol.sendUnicastObject(socketforServer,
											new CommitMessage(ack.getLogIndex(), ack.getTerm()),
											InetAddress.getByName(s.getIpAddress()), s.getPortforServer());
								}
							}

							break;
						}
					}
				} catch (SocketTimeoutException e) {
					Log precedingLog;
					Log newLog;
					if (me.getListLog().size() == 1) {
						precedingLog = null;
						newLog = me.getListLog().get(0);
					} else {
						precedingLog = me.getListLog().get(me.getListLog().size() - 2);
						newLog = me.getListLog().get(me.getListLog().size() - 1);
					}
					for (Server s : listServers) {
						if (s.getIpAddress().equals(me.getIpAddress())) {
							if (!listAcks.get(s.getIpAddress())) {
								System.out.println("process" + me.getIpAddress() + "send again " + newLog.getCommand()
										+ " to process" + s.getIpAddress() + " at " + new Date());
								AppendEntry appendEntry = new AppendEntry(newLog, precedingLog);
								UDPProtocol.sendUnicastObject(socketforServer, appendEntry,
										InetAddress.getByName(s.getIpAddress()), s.getPortforServer());
							}
						}

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
				if (obj instanceof AppendEntry) {
					AppendEntry appendEntry = (AppendEntry) obj;
					Log newLog = appendEntry.getNewLog();
					Log precedingLog = appendEntry.getPrecedingLog();
					// check if it is the first log
					if (appendEntry.getPrecedingLog() == null) {
						System.out.println("process" + me.getIpAddress() + " appended jmp0 at " + new Date());
						me.getListLog().add(newLog);
					} else {
						if (me.getListLog().get(me.getListLog().size() - 1).equals(precedingLog)) {
							System.out.println("process" + me.getIpAddress() + " appended " + newLog.getCommand()
									+ " at " + new Date());
							me.getListLog().add(newLog);
						}
					}
					UDPProtocol.sendUnicastObject(socketforServer,
							new AckMessage(newLog.getLogIndex(), newLog.getTerm()), receivePacket.getAddress(),
							receivePacket.getPort());
				}

				if (obj instanceof CommitMessage) {
					CommitMessage commitMes = (CommitMessage) obj;
					me.getListLog().get(commitMes.getLogIndex()).setState("committed");
					String committedLogs = "Process " + me.getIpAddress() + ": Committed log ";
					for (Log l : me.getListLog()) {
						if (l.getState().equals("committed")) {
							committedLogs += l.getLogIndex() + " ";
						}
					}
					System.out.println(committedLogs);
				}

			} catch (SocketTimeoutException e) {
				// e.printStackTrace();
				System.out.println("Process" + me.getIpAddress() + ": socket timeout exception, elect new leader at "
						+ new Date());
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
			while (true) {
				DatagramPacket receivePacket = UDPProtocol.receiveUnicast(socketforClient);
				System.out.println(receivePacket.getData().length);
				command = new String(receivePacket.getData()).trim();
				System.out.println("process" + me.getIpAddress() + " received command=" + command + " from client at "
						+ new Date());
				synchronized (listCommands) {
					listCommands.add(command);
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
		@Override
		public void run() {
			while (true) {
				String command = null;
				synchronized (listCommands) {
					if (listCommands.size() != 0) {
						command = listCommands.get(0);
						listCommands.remove(0);
					}
				}
				if (command != null) {
					System.out.println(
							"Process" + me.getIpAddress() + " starts sending command=" + command + " at " + new Date());
					sendAppendEntryLeaderSide(command);
					if(listServers.size() > 1) {
						processAckLeaderSide();
					}
					
				}
			}
		}
	}
}
