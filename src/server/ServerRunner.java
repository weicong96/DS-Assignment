package server;

public class ServerRunner {
	public final static int PORT = 4445;
	public static void main(String[] args) {
		Server server = new Server(PORT);
		server.start();
	}

}
