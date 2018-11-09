package lab3_gbn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class Client {

    private String remoteHost = "localhost";
    private int remotePort = 8888;
    private DatagramSocket cSocket;
    private static int BUFFER_LENGTH = 1026; // 缓冲区大小

    private void run() {

        try {
            cSocket = new DatagramSocket();
            InetAddress remoteIp = InetAddress.getByName(remoteHost);

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String msg = "";
            double pktLossRatio = 0.2; // 默认包丢失率
            double ackLossRatio = 0.2; // 默认ack丢失率
            int seq = 0; // 包的序列号
            int waitSeq = 0; // 等待的序列号
            int recvSeq = 0; // 接收窗口大小为1，已确认的序列号

            while (msg != null) {
                System.out.print(">");
                msg = reader.readLine();
                byte[] outputData = msg.getBytes();
                DatagramPacket outPutPacket = new DatagramPacket(outputData, outputData.length, remoteIp, remotePort);
                cSocket.send(outPutPacket);

                if (msg.equals("bye")) {
                    break;
                } else if (msg.equals("testgbn")) {
                    while (true) {
                        DatagramPacket inputPacket = new DatagramPacket(new byte[BUFFER_LENGTH], BUFFER_LENGTH);
                        cSocket.receive(inputPacket); // 使用阻塞模式接收数据
                        seq = (int) inputPacket.getData()[0];
                        byte[] buffer = new byte[1026];
                        boolean b = lossInLossRatio(pktLossRatio); // 判断是否模拟丢包
                        if (b) { // 丢失数据包
                            System.err.println("The packet with a seq of " + seq + " loss");
                            continue;
                        } else {
                            System.out.println("rcv pkt" + seq); // 收到数据包
                            if ((waitSeq - seq) == 0) { // 收到的包是期待的包
                                waitSeq++;
                                if (waitSeq == 20) {
                                    waitSeq = 0;
                                }
//                                System.out.println(new String(inputPacket.getData(), 1, 10, Charset.forName("UTF-8")));
                                recvSeq = seq;
                                buffer[0] = (byte) recvSeq; // 返回包对应的Ack
                                buffer[1] = '0';
                            } else { // 收到的包不是期待的包
                                buffer[0] = (byte) recvSeq; // 构建期待包的Ack
                                buffer[1] = '0';
                            }
                            b = lossInLossRatio(ackLossRatio); // 判断是否模拟丢失Ack
                            if (b) { // Ack丢失
                                System.err.println("The ack of " + buffer[0] + " loss");
                                continue;
                            } else {
                                outPutPacket.setData(buffer); // 正常发送Ack
                                cSocket.send(outPutPacket);
                                System.out.println("send ack" + buffer[0]);
                            }
                        }
                        Thread.sleep(500);
                    }

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

    // 根据丢失率，使用随机数进行判断
    private boolean lossInLossRatio(double lossRatio) {
        int lossBound = (int) (lossRatio * 100);
        Random rand = new Random();
        int r = rand.nextInt(100);
        if (r <= lossBound) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        new Client().run();
    }
}
