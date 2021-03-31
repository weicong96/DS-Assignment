package server;

// contains details about how each monitoring request can reach its client, expiry time and what request id to use
public class Monitor {
	private String clientAddress;
	private int clientPort;
	private long expiry;
	private int requestId;
	public Monitor(String address, int clientPort, long expiry, int requestId) {
		this.clientAddress = address;
		this.clientPort = clientPort;
		this.expiry = expiry;
		this.requestId = requestId;
	}
	
	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public String getClientAddress() {
		return clientAddress;
	}
	public void setClientAddress(String clientAddress) {
		this.clientAddress = clientAddress;
	}
	public int getClientPort() {
		return clientPort;
	}
	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}
	public long getExpiry() {
		return expiry;
	}
	public void setExpiry(long expiry) {
		this.expiry = expiry;
	}
	
	
}
