
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.swing.text.BadLocationException;

public class GreetingClient4
{
    static Frame frame;
	
    public static void main(String [] args) throws IOException, BadLocationException
    {
        
	String serverName = "localhost";
	int port = 6003;
        
	Socket client = new Socket(InetAddress.getByName("127.0.0.1"), port);
        DataInputStream in = new DataInputStream(client.getInputStream());
        DataOutputStream out = new DataOutputStream(client.getOutputStream());
        frame = new Frame(client, in, out);
    }
}