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
    
    public static int ni(String s, int n) { // % type of the msg w ttl method gets the nth index of the % 
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
        serverSockets = new ArrayList <AcceptServer>(); // bta3et el nas e i gaya
        serverSocket = new ServerSocket(port); // bta3o eli byb3at mno 
        System.out.println("Central Server is running");
        
        Runnable run = new Runnable() { 
            public void run() {
                try {
                    waitForServers(); 
                } catch (IOException e) {}
        }};
        Thread thread = new Thread(run); thread.start();
    }
    
    public void waitForServers() throws IOException {
        while(true) {
            final Socket socket=serverSocket.accept();

            Runnable run = new Runnable() {
                public void run() {
                    try {
                        AcceptServer as = new AcceptServer(socket); //
                    } catch (Exception e) {}
            }};
            Thread thread = new Thread(run); thread.start();
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
            
            Runnable run = new Runnable() {
            public void run() {
                try {
                    inFromServer();
                } catch (Exception e) {}
            }};
            Thread thread = new Thread(run); thread.start();
        }
        
        public void inFromServer() throws IOException { // galo mn el server el so3'ayar eli ta7tih 
            String s = "";
            while(true) {
                s = in.readUTF();
                if(s.length() > 0) System.out.println("From Child Server: " + s);
                if(s.length()>=3) {
                    if (s.substring(0, 3).equals("REP")) { // lama el big server byrequest el online users bta3o
                        currentConnectedUsers.append(s.substring(ni(s, 1) + 1));
                        replies++;
                    }
                    else if(s.substring(0, 3).equals("LST")) { // asami el nas eli online clients kolohom  
                        currentConnectedUsers = new StringBuilder(s.substring(ni(s, 1) + 1));
                        replies = 1;
                        for(int i = 0; i < serverSockets.size(); i++) {
                            if(serverSockets.get(i)!=this) {
                                composeMessage2(serverSockets.get(i).out, "REQ", ""); // byb3at le ba2i el servers y2olo 3ayz el online users 
                            }
                        }
                        Runnable run = new Runnable() { // 3amal thread 3ashan mstani el replies 
                            public void run() {
                                try {
                                    sendConnectedUsers();
                                } catch (IOException e) {}
                        }};
                        Thread thread = new Thread(run); thread.start();
                    }
                    else if (s.substring(0, 3).equals("MSG")) {
                        String sender = s.substring(ni(s, 1) + 1, ni(s, 2));
                        String receiver = s.substring(ni(s, 2) + 1, ni(s, 3));
                        int ttl = Integer.parseInt(s.substring(ni(s, 3) + 1, ni(s, 4)));
                        String message = s.substring(ni(s, 4) + 1);
                        if(ttl != 0) {
                            ttl--;
                            for(int i = 0; i < serverSockets.size(); i++) {
                                if(serverSockets.get(i)!=this) {
                                    composeMessage(serverSockets.get(i).out, "MSG", sender, receiver, ttl, message);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        public void sendConnectedUsers() throws IOException {
            while(replies != serverSockets.size());
            composeMessage2(out, "LST", currentConnectedUsers.toString());
        }
    }
    
    public static void main(String[] args) throws Exception {
        CentralServer cs = new CentralServer(6004);
    }
}
