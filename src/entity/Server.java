package entity;

import java.util.ArrayList;
import java.util.List;

import resource.Log;

public class Server {
	private String name;
	private String ipAddress;
	private int portforClient;
	private int portforServer;
	private int term;
	private List<Log> listLog;
	private List<Log> listCommitedLog;

	public Server(String ipAddress, int portforClient, int portforServer) {
		super();
		this.ipAddress = ipAddress;
		this.portforClient = portforClient;
		this.portforServer = portforServer;
		this.listLog = new ArrayList<>();
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPortforClient() {
		return portforClient;
	}

	public void setPortforClient(int portforClient) {
		this.portforClient = portforClient;
	}

	public int getPortforServer() {
		return portforServer;
	}

	public void setPortforServer(int portforServer) {
		this.portforServer = portforServer;
	}

	public int getTerm() {
		return term;
	}

	public void setTerm(int term) {
		this.term = term;
	}

	public List<Log> getListLog() {
		return listLog;
	}

	public void setListLog(List<Log> listLog) {
		this.listLog = listLog;
	}

	public List<Log> getListCommitedLog() {
		return listCommitedLog;
	}

	public void setListCommitedLog(List<Log> listCommitedLog) {
		this.listCommitedLog = listCommitedLog;
	}
}
