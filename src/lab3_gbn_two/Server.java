package lab3_gbn_two;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Server {

    private int port = 8888; // 端口1
    private int remotePort = 8800;  //端口2
    private String remoteHost = "localhost";
    private DatagramChannel channel;    //发送使用非阻塞的channel
    private DatagramSocket socket;      //发送socket
    private static int BUFFER_LENGTH = 1026; // 缓冲区大小
    private ByteBuffer buffer; // 缓冲区

    private SocketAddress remoteAddr;
    private DatagramSocket cSocket; //接收socket
    
    public Server() {
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false); // 设置为非阻塞模式
            socket = channel.socket();        //发送socket初始化
            SocketAddress localAddr = new InetSocketAddress(port);
            socket.bind(localAddr); // 绑定本地地址
            buffer = ByteBuffer.allocate(BUFFER_LENGTH);
            cSocket = new DatagramSocket();    //接收socket初始化
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
                System.out.println("Receive from client: " + data); //根据接收到的客户端命令选择操作类型
                if (data.startsWith("bye")) { // 结束
                    System.out.println("Server shutdown");
                    break; 
                    
                } else if (data.startsWith("testgbn -two")) { // 开始测试双向GBN协议
                    System.out.println("Begin to test two-way GBN protocol!");
                    new Thread(new sendThread("ServerSend: ", channel, remoteAddr)).start();  //开启发送文件线程
                    
                    InetAddress remoteIp = InetAddress.getByName(remoteHost); //告知客户端接收文件已准备就绪
                    String ready = "Server is ready to receive file!";
                    System.out.println(ready);
                    byte[] outputData = ready.getBytes();
                    DatagramPacket outPutPacket = new DatagramPacket(outputData, outputData.length, remoteIp, remotePort);
                    cSocket.send(outPutPacket);
                    
                    new Thread(new revdThread("ServerRecv: ", cSocket, outPutPacket)).run();    //开启接收文件线程
                    
                } else if (data.startsWith("testgbn")) {    //开始测试单向GBN协议
                    System.out.println("Begin to test GBN protocol!");
                    new Thread(new sendThread("ServerSend: ", channel, remoteAddr)).run();  //开启发送文件线程
                    
                } 

            } catch (IOException | InterruptedException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }


       
    public static void main(String[] args) {
        new Server().run();
    }


}
