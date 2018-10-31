package lab2_rdt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * @author TYJ
 * @since 2018年10月31日
 */
public class MyClient implements Runnable {

    private String remoteHost = "localhost";
    private int remotePort = 8000;
    private DatagramSocket socket;
    private int TimeOut = 10000;
    private int pktNum = 0;
    
    public MyClient() throws SocketException {
        socket = new DatagramSocket();
        socket.setSoTimeout(TimeOut);
        System.out.println("Client started...");
    }


    public void run() {
            try {
                InetAddress remoteIp = InetAddress.getByName(remoteHost);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String data = " ";
                while(data !=null) {
                    System.out.print(">");
                    data = new String(reader.readLine() + pktNum);
                    byte[] outputData = data.getBytes();
                    DatagramPacket outPutPacket = new DatagramPacket(outputData, outputData.length, remoteIp, remotePort);
                    socket.send(outPutPacket);
                    
                    if (data.startsWith("bye")) {   //结束
                        break;
                    }
                    System.out.println("Client send pkt" + pktNum + " done, waiting ack...");
                    
                    DatagramPacket inputPacket = new DatagramPacket(new byte[512], 512);
                    while (true) {
                        try {
                            socket.receive(inputPacket);
                            String str =  new String(inputPacket.getData(),0, inputPacket.getLength());
                            System.out.println("Client receive " + str);
                            if (str.equals("Ack" + pktNum)) {
                                break;
                            }
                        } catch (SocketTimeoutException e) {
                            socket.send(outPutPacket);
                            System.out.println("TimeOut! Client send pkt" + pktNum + " again, waiting ack...");
                        }    
                    }
                    pktNum = pktNum ^ 1;
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            } finally {
                socket.close();
                System.out.println("Client shutdown");
            }
    }
    
    public static void main(String[] args) {
        try {
            new MyClient().run();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
