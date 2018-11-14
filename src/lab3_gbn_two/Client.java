package lab3_gbn_two;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class Client {

    private String remoteHost = "localhost";
    private int remotePort = 8888;  //端口1
    private int remoteport2 = 8800; //端口2
    private DatagramSocket cSocket;
    
    private DatagramChannel channel;    //发送使用非阻塞的channel
    private DatagramSocket socket;      //发送socket
    private SocketAddress remoteAddr;
    private static int BUFFER_LENGTH = 1026; // 缓冲区大小
    private ByteBuffer buffer; // 缓冲区
    
    public Client() {
        try {
            cSocket = new DatagramSocket();     //接收socket初始化
            
            channel = DatagramChannel.open();
            channel.configureBlocking(false);   // 设置为非阻塞模式
            socket = channel.socket();          //发送socket初始化
            SocketAddress localAddr = new InetSocketAddress(remoteport2);
            socket.bind(localAddr); // 绑定本地地址
            buffer = ByteBuffer.allocate(BUFFER_LENGTH);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
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
                } else if (msg.equals("testgbn")){      //测试单向GBN
                    new Thread(new revdThread("ClientRecv: ", cSocket, outPutPacket)).run();    //开启接收进程
                    
                } else if (msg.equals("testgbn -two")) {  // 测试双向GBN
                    new Thread(new revdThread("ClientRecv: ", cSocket, outPutPacket)).start();    //开启接收进程                    
                    
                    while ((remoteAddr = channel.receive(buffer)) == null) {    //等待服务器发送准备消息
                        Thread.sleep(200);
                    }
                    buffer.flip();
                    String data = new String(buffer.array());
                    System.out.println("Receive from Server: " + data);
                    new Thread(new sendThread("ClientSend: ", channel, remoteAddr)).run();      //开启发送进程
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
        new Client().run();
    }
    

}
