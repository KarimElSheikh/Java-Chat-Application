import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.*;

public class GreetingServer {
   
    boolean reply;  // Boolean indicating it's waiting for a reply from the central server
    ServerSocket serverSocket;
    ArrayList <AcceptClient> clientSockets;
    ArrayList <String> loginNames; 
    ArrayList<String>  allOnlineUsers; 
    AcceptCentralServer centralServer;
    String allConnectedUsers;  
   
    public static int findNthPercSign(String s, int n) {
        for(int i = 0; i < s.length(); i++) {
            if(s.charAt(i) == '%') {
                if(--n == 0) return i;
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
   
    public GreetingServer(int port) throws Exception {
        clientSockets = new ArrayList <AcceptClient>();
        loginNames = new ArrayList <String>();
        allOnlineUsers = new ArrayList <String>();
        serverSocket = new ServerSocket(port);
        System.out.println("Server started running");

        Runnable run = new Runnable() {
            public void run() {
                try {
                    waitForClients();
                } catch (IOException e) {
                    System.out.println("Exception1");
                }
        }};
        Thread thread = new Thread(run);
        thread.start();
    }
    
    public void waitForClients() throws IOException {
        while(true) {
            final Socket socket=serverSocket.accept();
            final DataInputStream in = new DataInputStream(socket.getInputStream());
            final DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            
            Runnable run = new Runnable() {
            public void run() {
                try {
                    String s = waitForFirstMessage(in, out);  // Wait for login name
                    if(s != null)
                        new AcceptClient(socket, s);
                } catch (Exception e) {
                    System.out.println("Exception2");
                }
            }};
            Thread thread = new Thread(run); thread.start();
        }
    }
        
    public String waitForFirstMessage(DataInputStream in, DataOutputStream out) throws IOException {
        composeMessage2(out, "NOT", "Welcome, please enter your Login Name");  // NOT = Notification
        String s = "";
        
        while(true) {
            while(s.equals(""))
                s = in.readUTF();
            if(s.length() >= 3) {
                if(s.equals("BYE%")) {
                    return null;
                }
                if(s.substring(0,3).equals("JON")) {  // JON = User sends his login name to "join" 
                    reply = false; 
                    String user = s.substring(findNthPercSign(s, 1) + 1);
                    composeMessage2(centralServer.out, "LST", getConnectedUsersToThisServerOnly());
                    while(!reply) {
                    }
                    // System.out.println("Got a reply in string "allConnectedUsers" with the list of users from the central server");
                    int start = 0; allOnlineUsers.clear();
					// Now parsing the string "allConnectedUsers"
                    for(int i = 0; i < allConnectedUsers.length(); i++) {
                        if(allConnectedUsers.charAt(i)=='%') {
                            allOnlineUsers.add(allConnectedUsers.substring(start, i));
                            System.out.println(allConnectedUsers.substring(start, i));
                            start = i+1;
                        }
                    }
                    s = s.substring(findNthPercSign(s, 1) + 1);
                    if(allOnlineUsers.contains(s)) {
                        composeMessage2(out, "NOT", "Sorry, this login name was already entered by another user. Please, enter another login name.");
                        s = "";
                    }
                    else if(s.contains("%")) {
                        composeMessage2(out, "NOT", "Sorry, the login name can't contain \'%\'.");
                        s = "";
                    }
                    else if(s.equals("")) {
                        composeMessage2(out, "NOT", "Sorry, the login name can't be empty.");
                    }
                    else {
                        composeMessage2(out, "ACP", "Successfully logged in.");
                        return s;
                    }
                }
            }
        }
    }
   
    public void connectToServer(final InetAddress IP, final int port) throws Exception {
        Runnable run = new Runnable() {
            public void run() {
                try {
                    centralServer = new AcceptCentralServer(new Socket(IP, port));
                } catch (Exception e) {
                    System.out.println("Exception3");
                }
        }};
        Thread thread = new Thread(run); thread.start();
    }
    
    public String getConnectedUsersToThisServerOnly(String user) {
        StringBuilder ss = new StringBuilder();
        for(int i = 0; i < loginNames.size(); i++) {
            if(!loginNames.get(i).equals(user)) {
                ss.append(loginNames.get(i));
                ss.append("%");
            }
        }
        return ss.toString();
    }
    
    public String getConnectedUsersToThisServerOnly() {
        StringBuilder ss = new StringBuilder();
        for(int i = 0; i < loginNames.size(); i++) {
            ss.append(loginNames.get(i));
            ss.append("%");
        }
        return ss.toString();
    }
    
    class AcceptCentralServer {
        Socket socket;
        DataInputStream in;
	DataOutputStream out;
        
        public AcceptCentralServer(Socket inSocket) throws IOException {
            socket = inSocket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            String s = "";
            
            Runnable run = new Runnable() {
            public void run() {
                try {
                    inFromCentralServer();
                } catch (IOException e) {
                    System.out.println("Exception4");
                    e.printStackTrace();
                }
            }};
            Thread thread = new Thread(run);
            thread.start();
        }
        
        public void inFromCentralServer() throws IOException {
            String s = "";
            while(true) {
                s = in.readUTF();
                if(s.length() > 0) System.out.println("From Central Server: " + s);
                if(s.length() >= 3) {
                    if(s.substring(0, 3).equals("REQ")) {
                        composeMessage2(out, "REP", getConnectedUsersToThisServerOnly());
                    }
                    else if (s.substring(0, 3).equals("LST")) {
                        allConnectedUsers = s.substring(findNthPercSign(s, 1) + 1);
                        reply = true; 
                    }
                    else if(s.substring(0, 3).equals("MSG")) {
                        String sender = s.substring(findNthPercSign(s, 1) + 1, findNthPercSign(s, 2));
                        String receiver = s.substring(findNthPercSign(s, 2) + 1, findNthPercSign(s, 3));
                        int ttl = Integer.parseInt(s.substring(findNthPercSign(s, 3) + 1, findNthPercSign(s, 4)));
                        String message = s.substring(findNthPercSign(s, 4) + 1);
                        if(ttl != 0) {
                            for(int i = 0; i < loginNames.size(); i++) {
                                if(loginNames.get(i).equals(receiver)) {
                                    ttl--;
                                    composeMessage(clientSockets.get(i).out, "MSG", sender, receiver, ttl, message);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
        
    class AcceptClient {
	Socket clientSocket;
	DataInputStream in;
	DataOutputStream out;
	String loginName;
		
        public AcceptClient (Socket inSocket, String inLoginName) throws Exception
        {
            clientSocket=inSocket;
            loginName = inLoginName;
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            loginNames.add(loginName);
            clientSockets.add(this);
            System.out.println("User Logged In: " + loginName);
            Runnable run = new Runnable() {
            public void run() {
                try {
                    inFromClient();
                } catch (IOException e) {
                    System.out.println("Exception5");
                }
            }};
            Thread thread = new Thread(run); thread.start();
        }
	
        public void inFromClient() throws IOException {
            String s = "";
            
            while(true) {
                s = in.readUTF();
                if(s.length() > 0) System.out.println("From Client: " + s);
                if(s.length() >= 3) {
                    if(s.substring(0, 3).equals("LST")) {
                        reply = false;
                        String user = s.substring(findNthPercSign(s, 1) + 1);
                        composeMessage2(centralServer.out, "LST", getConnectedUsersToThisServerOnly(user));
                        Runnable run = new Runnable() {
                            public void run() {
                                try {
                                    sendConnectedUsers();
                                } catch (IOException e) {
                                    System.out.println("Exception6");
                                }
                        }};
                        Thread thread = new Thread(run); thread.start();
                    }
                    else if(s.substring(0, 3).equals("MSG")) {
                        String sender = s.substring(findNthPercSign(s, 1) + 1, findNthPercSign(s, 2));
                        String receiver = s.substring(findNthPercSign(s, 2) + 1, findNthPercSign(s, 3));
                        int ttl = Integer.parseInt(s.substring(findNthPercSign(s, 3) + 1, findNthPercSign(s, 4)));
                        String message = s.substring(findNthPercSign(s, 4) + 1);
                        boolean foundReceiver = false;
                        if(ttl != 0) {
                            ttl--;
                            for(int i = 0; i < loginNames.size(); i++) {
                                if(loginNames.get(i).equals(receiver)) {
                                    composeMessage(clientSockets.get(i).out, "MSG", sender, receiver, ttl, message);
                                    foundReceiver = true;
                                    break;
                                }
                            }
                            if(!foundReceiver) {
                                composeMessage(centralServer.out, "MSG", sender, receiver, ttl, message);
                            }
                        }
                    }
                    else if(s.substring(0, 3).equals("BYE")) {
                        break;
                    }
                }
            }
            
            System.out.println("Closed client: " + loginName);
            clientSocket.close();
                           
            for(int j = 0; j < loginNames.size(); j++) {
                if(loginNames.get(j).equals(loginName)) {
                    clientSockets.remove(j);
                    loginNames.remove(j);
                }
            }
        }
        
        public void sendConnectedUsers() throws IOException {
            System.out.println("entered");
            while(!reply) {
            }
            composeMessage2(out, "LST", allConnectedUsers);
        }
    }
    
       
    public static void main(String [] args) throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter the port number, where you want to listen for users");
        int port = Integer.parseInt(br.readLine());
        GreetingServer gs = new GreetingServer(port);
            
        System.out.println("Enter (in 1 line) the IP followed by the port number of the central server you want to connect to");
        StringTokenizer st = new StringTokenizer(br.readLine());
        InetAddress IP = InetAddress.getByName(st.nextToken());
        port = Integer.parseInt(st.nextToken());
        gs.connectToServer(IP, port);
        
        while(true) {
            if(br.readLine().equals("1")) {
                System.out.println(gs.getConnectedUsersToThisServerOnly(""));
            }
        }
    }
}