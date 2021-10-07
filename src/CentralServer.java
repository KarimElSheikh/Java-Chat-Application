import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class CentralServer {
	ServerSocket serverSocket;
	ArrayList <AcceptServer> serverSockets;
	StringBuilder currentConnectedUsers;
	int replies;
	
	static int counter1 = 0;
	
	public static int findNthPercSign(String s, int n) {  // Find the index of the nth percentage sign symbol in a given string s
		for(int i = 0; i < s.length(); i++) {
			if(s.charAt(i) == '%') {
				n--;
				if(n == 0) return i;
			}
		}
		return -1;
	}
	
	public static void composeMessage2(DataOutputStream out, String type, String message) throws IOException {
		out.writeUTF(type+"%" + message);
	}
	
	public static void composeMessage(DataOutputStream out, String type, String sender, String receiver, int ttl, String message) throws IOException {
		out.writeUTF(type+"%" + sender+"%" + receiver+"%" + ttl+"%" + message);
	}
	
	public CentralServer(int port) throws Exception {
		serverSockets = new ArrayList<AcceptServer>();  // List of the child servers (class AcceptServer)
		serverSocket = new ServerSocket(port);          // Opens a socket at port "port" to listen for child servers at
		System.out.println("Central Server is running");
		
		Runnable run = new Runnable() { 
			public void run() {
				try {
					waitForServers(); 
				} catch (IOException e) {}
		}};
		Thread thread = new Thread(run);
		thread.start();
	}
	
	public void waitForServers() throws IOException {
		while(true) {
			final Socket socket=serverSocket.accept();
			System.out.println(++counter1);

			Runnable run = new Runnable() {
				public void run() {
					try {
						AcceptServer as = new AcceptServer(socket);
					} catch (IOException e) {}
			}};
			Thread thread = new Thread(run);
			thread.start();
		}
	}
	
	class AcceptServer {
		Socket socket;
		DataInputStream in;
		DataOutputStream out;
		
		public AcceptServer(Socket inSocket) throws IOException {
			socket = inSocket;
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			serverSockets.add(this);
			
			Runnable run = () -> {
				try {
					inFromServer();
				} catch (IOException e) {}
			};
			Thread thread = new Thread(run);
			thread.start();
		}
		
		public void inFromServer() throws IOException {  // Process incoming message from a child server
			String s = "";
			while(true) {
				s = in.readUTF();
				if(s.length() > 0) System.out.println("From Child Server: " + s);
				if(s.length()>=3) {
					switch (s.substring(0, 3)) {
						case "REP":
							// Central server got a reply from a child server with its list of users
							currentConnectedUsers.append(s.substring(findNthPercSign(s, 1) + 1));
							replies++;
							break;
						case "LST":
							// A child server requested the list of online users' names
							currentConnectedUsers = new StringBuilder(s.substring(findNthPercSign(s, 1) + 1));
							replies = 1;
							for(int i = 0; i < serverSockets.size(); i++) {
								if(serverSockets.get(i)!=this) {
									composeMessage2(serverSockets.get(i).out, "REQ", "");  // Send to each child server requesting its list of users
								}
							}
							Runnable run = new Runnable() {  /* Run a thread to wait for replies from all child servers,
															  * then send the list of users to this AcceptServer once it's ready
															  */
								public void run() {
									try {
										sendConnectedUsers();
									} catch (IOException e) {}
								}
							};
							Thread thread = new Thread(run);
							thread.start();
							break;
						case "MSG":
							String sender = s.substring(findNthPercSign(s, 1) + 1, findNthPercSign(s, 2));
							String receiver = s.substring(findNthPercSign(s, 2) + 1, findNthPercSign(s, 3));
							int ttl = Integer.parseInt(s.substring(findNthPercSign(s, 3) + 1, findNthPercSign(s, 4)));
							String message = s.substring(findNthPercSign(s, 4) + 1);
							if(ttl != 0) {
								ttl--;
								for(int i = 0; i < serverSockets.size(); i++) {
									if(serverSockets.get(i)!=this) {
										composeMessage(serverSockets.get(i).out, "MSG", sender, receiver, ttl, message);
									}
								}
							}   break;
						default:
							break;
					}
				}
			}
		}
		
		public void sendConnectedUsers() throws IOException {
			while(replies != serverSockets.size()) {
			}
			composeMessage2(out, "LST", currentConnectedUsers.toString());
		}
	}
	
	public static void main(String[] args) throws Exception {
		CentralServer cs = new CentralServer(6050);
	}
}
