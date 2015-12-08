package resource;

import java.io.Serializable;

public class AckMessage implements Serializable{
	private int logIndex;
	private int term;
	public AckMessage(int logIndex, int term) {
		super();
		this.logIndex = logIndex;
		this.term = term;
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
}
