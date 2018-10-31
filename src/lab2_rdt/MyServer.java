package lab2_rdt;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * @author TYJ
 * @since 2018年10月31日
 */
public class MyServer implements Runnable{
    private int port = 8000;
    private DatagramSocket socket;
    private int count = 0;
    public MyServer() throws SocketException {
        socket = new DatagramSocket(port);
        System.out.println("Server has started...");
    }

    public void run() {
        while (true) {
            try {
                
                DatagramPacket packet = new DatagramPacket(new byte[512], 512);
                socket.receive(packet);
                String data = new String(packet.getData(), 0 , packet.getLength());
                String pktNum = data.substring(data.length() - 1);  
                String info = new String(packet.getAddress() + ":" + packet.getPort());
                System.out.println("Server receive: " + info + ">" + data.substring(0, data.length() - 1));
                if (data.startsWith("bye")) {   //结束
                    System.out.println("Server shutdown");
                    break;
                }
                if (data.startsWith("NoAck") && (count < 2)) {    //模拟不发送ACK
                    count++;
                    System.out.println("Pretending that I didn't see the pkt" + pktNum);
                } else {
                    count = 0;
                    String ack = "Ack" + pktNum;
                    packet.setData(ack.getBytes());
                    socket.send(packet);
                    System.out.println("Server send " + ack);
                }
              
            } catch (IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
        
    }
    
    public static void main(String[] args) {
        try {
            new MyServer().run();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    
    
}
