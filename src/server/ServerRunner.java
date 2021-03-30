package server;

import shared.Constants.INVOCATION_TYPE;

public class ServerRunner {
	public final static int PORT = 4445;
	public static void main(String[] args) {
		Server server = new Server(PORT, INVOCATION_TYPE.AT_MOST_ONCE);
		server.start();
	}

}
