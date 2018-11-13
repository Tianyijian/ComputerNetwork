package lab3_sr_two;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Client {

    private String remoteHost = "localhost";
    private int remotePort = 8888;
    private int remotePort2 = 8800;
    private DatagramSocket cSocket;

    private DatagramChannel channel;
    private DatagramSocket socket;
    private static int BUFFER_LENGTH = 1026; // 缓冲区大小
    private ByteBuffer buffer; // 缓冲区

    private SocketAddress remoteAddr;
    
    public Client() throws SocketException {
        try {
            cSocket = new DatagramSocket();
            
            channel = DatagramChannel.open();
            channel.configureBlocking(false); // 设置为非阻塞模式
            socket = channel.socket();
            SocketAddress localAddr = new InetSocketAddress(remotePort2);
            socket.bind(localAddr); // 绑定本地地址
            buffer = ByteBuffer.allocate(BUFFER_LENGTH);
            
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }

    private void run() {

        try {
            InetAddress remoteIp = InetAddress.getByName(remoteHost);
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String msg = "";
            while (msg != null) {
                System.out.print(">");
                msg = reader.readLine();
                byte[] outputData = msg.getBytes();
                DatagramPacket outPutPacket = new DatagramPacket(outputData, outputData.length, remoteIp, remotePort);
                cSocket.send(outPutPacket);

                if (msg.equals("bye")) {
                    break;
                } else if (msg.equals("testsr -two")) {
                    new Thread(new RecvThread("ClientRecv: ", cSocket, outPutPacket)).start();    //开启接收进程
                    
                    while ((remoteAddr = channel.receive(buffer)) == null) {    //等待服务器的发送消息
                        Thread.sleep(200);
                    }
                    buffer.flip();
                    String data = new String(buffer.array());
                    System.out.println("Receive from Server: " + data);
                    new Thread(new SendThread("ClientSend: ", channel, remoteAddr)).run();      //开启发送进程
                    
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            cSocket.close();
            System.out.println("Client shutdown");
        }

    }



    public static void main(String[] args) {
        try {
            new Client().run();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
