package server;

import shared.Constants;
import shared.Constants.INVOCATION_TYPE;

public class ServerRunner {
	public static void main(String[] args) {
		Server server = new Server(Constants.PORT, INVOCATION_TYPE.AT_MOST_ONCE);
		server.start();
	}

}
