import java.net.*;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.*;

public class GreetingServer {
   
    boolean reply; // mstany reply mn el central server 
    ServerSocket serverSocket;
    ArrayList <AcceptClient> clientSockets;
    ArrayList <String> loginNames; 
    ArrayList<String>  allOnlineUsers; 
    AcceptCentralServer centralServer;
    String allConnectedUsers;  
   
    public static int ni(String s, int n) {
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
                } catch (IOException e) {}
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
                    String s = waitForFirstMessage(in, out); // wait for login name 
                    if(s!= null)
                    new AcceptClient(socket, s);
                } catch (Exception e) {
                    System.out.println("Exception");
                }
            }};
            Thread thread = new Thread(run); thread.start();
        }
    }
        
    public String waitForFirstMessage(DataInputStream in, DataOutputStream out) throws IOException {
        composeMessage2(out, "NOT", "Welcome, please enter your Login Name"); // type NOT notification 
        String s = "";
        
        while(true) {
            while(s.equals(""))
                s = in.readUTF();
            if(s.length() >= 3) {
                if(s.substring(0,3).equals("JON")) { // type JON  user send his login name 
                    reply = false; 
                    String user = s.substring(ni(s, 1) + 1);
                    composeMessage2(centralServer.out, "LST", getConnectedUsersToThisServerOnly());
                    while(!reply);
                    int l = 0; allOnlineUsers.clear(); // byshof min online users dlwa2ty 
                    for(int i = 0; i < allConnectedUsers.length(); i++) {
                        if(allConnectedUsers.charAt(i)=='%') {
                            allOnlineUsers.add(allConnectedUsers.substring(l, i));
                            System.out.println(allConnectedUsers.substring(l, i));
                            l = i+1;
                        }
                    }
                    s = s.substring(ni(s, 1) + 1);
                    if(allOnlineUsers.contains(s)) {
                        composeMessage2(out, "NOT", "Sorry this login name was already entered by another user. Enter another login name");
                        s = "";
                    }
                    else if(allOnlineUsers.contains("%")) {
                        composeMessage2(out, "NOT", "Login name can't contain \'%\'");
                        s = "";
                    }
                    else if(s.equals("")) {
                        composeMessage2(out, "NOT", "Login name can't be empty");
                    }
                    else {
                        composeMessage2(out, "ACP", "Successfully Logged in");
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
                } catch (Exception e) {}
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
                } catch (IOException e) {}
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
                        allConnectedUsers = s.substring(ni(s, 1) + 1);
                        System.out.println("received");
                        reply = true; 
                    }
                    else if(s.substring(0, 3).equals("MSG")) {
                        String sender = s.substring(ni(s, 1) + 1, ni(s, 2));
                        String receiver = s.substring(ni(s, 2) + 1, ni(s, 3));
                        int ttl = Integer.parseInt(s.substring(ni(s, 3) + 1, ni(s, 4)));
                        String message = s.substring(ni(s, 4) + 1);
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
                } catch (IOException e) {}
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
                        String user = s.substring(ni(s, 1) + 1);
                        composeMessage2(centralServer.out, "LST", getConnectedUsersToThisServerOnly(user));
                        Runnable run = new Runnable() {
                            public void run() {
                                try {
                                    sendConnectedUsers();
                                } catch (IOException e) {}
                        }};
                        Thread thread = new Thread(run); thread.start();
                    }
                    else if(s.substring(0, 3).equals("MSG")) {
                        String sender = s.substring(ni(s, 1) + 1, ni(s, 2));
                        String receiver = s.substring(ni(s, 2) + 1, ni(s, 3));
                        int ttl = Integer.parseInt(s.substring(ni(s, 3) + 1, ni(s, 4)));
                        String message = s.substring(ni(s, 4) + 1);
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
            while(!reply);
            composeMessage2(out, "LST", allConnectedUsers);
            System.out.println("done");
        }
    }
    
       
    public static void main(String [] args) throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter Port Number");
        int port = Integer.parseInt(br.readLine());
        GreetingServer gs = new GreetingServer(port);
            
        System.out.println("Enter the IP followed by the Port Number of the central server you want to connect to");
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