package resource;

import java.io.Serializable;

public class Log implements Serializable{
	private int logIndex;
	private int term;
	private String command;
	private String state;
	public Log(int logIndex, int term, String command, String state) {
		super();
		this.logIndex = logIndex;
		this.term = term;
		this.command = command;
		this.state = state;
	}
	
	public int getLogIndex() {
		return logIndex;
	}
	public void setLogIndex(int logIndex) {
		this.logIndex = logIndex;
	}
	public int getTerm() {
		return term;
	}
	public void setTerm(int term) {
		this.term = term;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	@Override
	public boolean equals(Object obj) {
		Log that = (Log) obj;
		if(this.logIndex == that.logIndex && this.term == that.term) {
			return true;
		}
		return false;
	}
}
