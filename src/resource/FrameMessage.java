package resource;

import java.io.Serializable;
import java.util.List;

public class FrameMessage implements Serializable {
	private transient List<Log> listLogs;
	private int firstIndex;

	public FrameMessage(List<Log> listLogs, int firstIndex) {
		super();
		this.listLogs = listLogs;
		this.firstIndex = firstIndex;
	}

	public List<Log> getListLogs() {
		return listLogs;
	}

	public void setListLogs(List<Log> listLogs) {
		this.listLogs = listLogs;
	}

	public int getFirstIndex() {
		return firstIndex;
	}

	public void setFirstIndex(int firstIndex) {
		this.firstIndex = firstIndex;
	}
}
