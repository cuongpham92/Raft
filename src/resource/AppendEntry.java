package resource;

import java.io.Serializable;

public class AppendEntry implements Serializable{
	private Log newLog;
	private Log precedingLog;
	
	public AppendEntry(Log newLog, Log precedingLog) {
		super();
		this.newLog = newLog;
		this.precedingLog = precedingLog;
	}

	public Log getNewLog() {
		return newLog;
	}

	public void setNewLog(Log newLog) {
		this.newLog = newLog;
	}

	public Log getPrecedingLog() {
		return precedingLog;
	}

	public void setPrecedingLog(Log precedingLog) {
		this.precedingLog = precedingLog;
	}
}
