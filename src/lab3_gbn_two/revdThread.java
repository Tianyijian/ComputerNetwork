package lab3_gbn_two;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;

public class revdThread implements Runnable {
    
    private DatagramSocket cSocket;
    private DatagramPacket outPutPacket;
    private static int BUFFER_LENGTH = 1026; // 缓冲区大小
    private String type;    //标识该进程调用方
    
    public revdThread(String type, DatagramSocket cSocket, DatagramPacket outPutPacket) {
        super();
        this.type = type;
        this.cSocket = cSocket;
        this.outPutPacket = outPutPacket;
    }

    @Override
    public void run() {
        try {
            double pktLossRatio = 0.2; // 默认包丢失率 
            double ackLossRatio = 0.2; // 默认ack丢失率
            int seq = 0; // 包的序列号
            int waitSeq = 0; // 等待的序列号
            int recvSeq = -1; // 接收窗口大小为1，已确认的序列号
            while (true) {
                DatagramPacket inputPacket = new DatagramPacket(new byte[BUFFER_LENGTH], BUFFER_LENGTH);
                cSocket.receive(inputPacket); // 使用阻塞模式接收数据
                seq = (int) inputPacket.getData()[0];
                byte[] buffer = new byte[1026];
                boolean b = lossInLossRatio(pktLossRatio); // 判断是否模拟丢包
                if (b) { // 丢失数据包
                    System.err.println(type + "The packet with a seq of " + seq + " loss");
                    continue;
                } else {
                    System.out.println(type + "rcv pkt" + seq); // 收到数据包
                    if ((waitSeq - seq) == 0) { // 收到的包是期待的包
                        waitSeq++;
                        if (waitSeq == 20) {
                            waitSeq = 0;
                        }
//                        System.out.println(new String(inputPacket.getData(), 1, 10, Charset.forName("UTF-8")));
                        recvSeq = seq;
                        buffer[0] = (byte) recvSeq; // 返回包对应的Ack
                        buffer[1] = '0';
                    } else { // 收到的包不是期待的包
                        buffer[0] = (byte) recvSeq; // 构建期待包的Ack
                        buffer[1] = '0';
                    }
                    System.out.println("waitSeq: " + waitSeq);
                    b = lossInLossRatio(ackLossRatio); // 判断是否模拟丢失Ack
                    if (b) { // Ack丢失
                        System.err.println(type + "The ack of " + buffer[0] + " loss");
                        continue;
                    } else {
                        outPutPacket.setData(buffer); // 正常发送Ack
                        cSocket.send(outPutPacket);
                        System.out.println(type + "send ack" + buffer[0]);
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
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

}
