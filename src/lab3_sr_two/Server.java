package lab3_sr_two;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Server {

    private int port = 8888; // 端口号
    private int remotePort = 8800;  //第二个端口
    private String remoteHost = "localhost";
    private DatagramChannel channel;
    private DatagramSocket socket;
    private static int BUFFER_LENGTH = 1026; // 缓冲区大小
    private ByteBuffer buffer; // 缓冲区
    private SocketAddress remoteAddr;
    
    private DatagramSocket cSocket;

    public Server() {
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false); // 设置为非阻塞模式
            socket = channel.socket();
            SocketAddress localAddr = new InetSocketAddress(port);
            socket.bind(localAddr); // 绑定本地地址
            buffer = ByteBuffer.allocate(BUFFER_LENGTH);
            cSocket = new DatagramSocket();
            System.out.println("Server has started...");
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void run() {
        while (true) {
            try {
                // 非阻塞接收，若没有接收到，返回为null
                remoteAddr = channel.receive(buffer);
                if (remoteAddr == null) {
                    Thread.sleep(200);
                    continue;
                }
                buffer.flip();
                String data = new String(buffer.array());
                System.out.println("Receive from client: " + data);
                if (data.startsWith("bye")) { // 结束
                    System.out.println("Server shutdown");
                    break;
                } else if (data.startsWith("testsr -two")) { // 开始测试
                    System.out.println("Begin to test SR protocol!");
                    new Thread(new SendThread("ServerSend: ", channel, remoteAddr)).start();  //开启发送文件线程
                    
                    InetAddress remoteIp = InetAddress.getByName(remoteHost);
                    byte[] outputData = "Server is ready to receive file!".getBytes();
                    DatagramPacket outPutPacket = new DatagramPacket(outputData, outputData.length, remoteIp, remotePort);
                    cSocket.send(outPutPacket);
                    
                    new Thread(new RecvThread("ServerRecv: ", cSocket, outPutPacket)).run();    //开启接收文件线程
                }
            } catch (IOException | InterruptedException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

   
    public static void main(String[] args) {
        new Server().run();
        // new Server().readFile();
    }
}
