import java.net.*;
import java.util.ArrayList;
import java.io.*;
import java.util.StringTokenizer;

public class GreetingServer1
{
    public static void main(String [] args) throws Exception
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        GreetingServer gs = new GreetingServer(6000);
        InetAddress IP = InetAddress.getByName("127.0.0.1");
        gs.connectToServer(InetAddress.getByName("127.0.0.1"), 6004);
    }
}