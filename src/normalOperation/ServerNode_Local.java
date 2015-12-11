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


public class ServerNode_Local extends Thread {
	private Server me;
	private DatagramSocket socketforClient;
	private DatagramSocket socketforServer;
	private List<Server> listServers;
	private Boolean isLeader;
	private List<String> listCommands;
	private boolean finished = true;
	// local experiment, different servers distinguish from each other by their ports
	private Map<Integer, Boolean> listAcks;

	public ServerNode_Local(String myIPAddress, int myPortforClient, int myPortforServer) throws IOException {
		me = new Server(myIPAddress, myPortforClient, myPortforServer);
		this.socketforClient = new DatagramSocket(myPortforClient);
		this.socketforServer = new DatagramSocket(myPortforServer);
		this.socketforServer.setSoTimeout(5000);
		isLeader = false;
		listCommands = new ArrayList<>();
		if (myPortforServer == 4447) {
			isLeader = true;
		}
		listServers = new ArrayList<Server>() {
			{
				add(new Server("localhost", 4446, 4447));
				add(new Server("localhost", 4448, 4449));
				add(new Server("localhost", 4450, 4451));
				add(new Server("localhost", 4452, 4453));
			}
		};
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
			for (Server s : listServers) {
				if (s.getPortforServer() != me.getPortforServer()) {
					UDPProtocol.sendUnicastObject(socketforServer, appendEntry,
							InetAddress.getByName(s.getIpAddress()), s.getPortforServer());
					System.out.println("process" + me.getPortforServer() + " sends command= " + command
							+ " to process" + s.getPortforServer() + " at " + new Date());
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processAckLeaderSide() {
		int ackCount = 0;
		try {
			listAcks = new HashMap<Integer, Boolean>() {
				{
					for (Server s : listServers) {
						put(s.getPortforServer(), false);
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
						System.out.println("process" + me.getPortforServer() + "received ack from process"
								+ receiveAck.getPort() + " at " + new Date());
						listAcks.put(receiveAck.getPort(), true);
						ackCount++;
						if (ackCount >= listServers.size() / 2) {
							System.out.println("process" + me.getPortforServer() + " received " + ackCount
									+ " acks for log " + ack.getLogIndex() + "-" + ack.getTerm()
									+ ". Commit this log and send commit message");
							me.getListLog().get(ack.getLogIndex()).setState("committed");
							for(Server s : listServers) {
								if(s.getPortforServer() != me.getPortforServer()) {
									UDPProtocol.sendUnicastObject(socketforServer, new CommitMessage(ack.getLogIndex(), ack.getTerm()),
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
						if (s.getPortforServer() != me.getPortforServer()) {
							if (!listAcks.get(s.getPortforServer())) {
								System.out
										.println("process" + me.getPortforServer() + "send again " + newLog.getCommand()
												+ " to process" + s.getPortforServer() + " at " + new Date());
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
				if(obj instanceof AppendEntry) {
					AppendEntry appendEntry = (AppendEntry) obj;
					Log newLog = appendEntry.getNewLog();
					Log precedingLog = appendEntry.getPrecedingLog();
					// check if it is the first log
					if (appendEntry.getPrecedingLog() == null) {
						System.out.println("process" + me.getPortforServer() + " appended jmp0 at " + new Date());
						me.getListLog().add(newLog);
					} else {
						if (me.getListLog().get(me.getListLog().size() - 1).equals(precedingLog)) {
							System.out.println("process" + me.getPortforServer() + " appended " + newLog.getCommand()
									+ " at " + new Date());
							me.getListLog().add(newLog);
						}
					}
					UDPProtocol.sendUnicastObject(socketforServer, new AckMessage(newLog.getLogIndex(), newLog.getTerm()),
							receivePacket.getAddress(), receivePacket.getPort());
				}
				
				if(obj instanceof CommitMessage) {
					CommitMessage commitMes = (CommitMessage) obj;
					me.getListLog().get(commitMes.getLogIndex()).setState("committed");
					String committedLogs = "Process " + me.getPortforServer() + ": Committed log ";
					for(Log l : me.getListLog()) {
						if(l.getState().equals("committed")) {
							committedLogs += l.getLogIndex() + " ";
						}
					}
					System.out.println(committedLogs);
				}
				
			} catch (SocketTimeoutException e) {
				// e.printStackTrace();
				System.out.println("Process" + me.getPortforServer()
						+ ": socket timeout exception, elect new leader at " + new Date());
			} catch (Exception e) {
				System.out.println("Process" + me.getPortforServer() + " waiting");
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
				command = new String(receivePacket.getData()).trim();
				System.out.println("process" + me.getPortforServer() + " received command=" + command
						+ " from client at " + new Date());
				synchronized (listCommands) {
					listCommands.add(command);
				}
			}

		}
		// follower part
		else {
			System.out.println("I am the follower at " + new Date());
			try {
				if (me.getPortforServer() == 4451) {
					Thread.sleep(7000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
					System.out.println("Process" + me.getPortforServer() + " starts sending command=" + command + " at "
							+ new Date());
					sendAppendEntryLeaderSide(command);
					processAckLeaderSide();
				}
			}
		}
	}
}
