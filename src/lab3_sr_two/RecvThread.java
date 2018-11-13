package lab3_sr_two;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Random;

public class RecvThread implements Runnable{

    private static final int BUFFER_LENGTH = 1026; // 缓冲区大小
    private static final int RCVD_WIND_SIZE = 10; // 缓存窗口大小
    private static final int SEQ_SIZE = 20; // 序列号个数
    private double pktLossRatio = 0.2; // 默认包丢失率
    private double ackLossRatio = 0; // 默认ack丢失率
    private int seq = 0; // 接收包的序列号
    private int waitSeq = 0; // 等待的序列号，也是窗口中第一个序列号
    private boolean ackSent[] = new boolean[SEQ_SIZE]; // 发送ack记录
    
    private DatagramSocket cSocket;
    private DatagramPacket outPutPacket;
    private String type;    //标识该进程调用方
    
    public RecvThread(String type, DatagramSocket cSocket, DatagramPacket outPutPacket) {
        super();
        this.type = type;
        this.cSocket = cSocket;
        this.outPutPacket = outPutPacket;
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                DatagramPacket inputPacket = new DatagramPacket(new byte[BUFFER_LENGTH], BUFFER_LENGTH);
                cSocket.receive(inputPacket); // 使用阻塞模式接收数据
                seq = (int) inputPacket.getData()[0];
                byte[] buffer = new byte[1026];
                boolean b = lossInLossRatio(pktLossRatio); // 模拟数据包丢失
                if (b) {
                    System.err.println(type + "The packet with a seq of " + seq + " loss");
                    continue;
                } else {
                    if ((waitSeq - seq) == 0) { // 收到的序列号是期待的序列号
                        System.out.println(type + "pkt" + seq + " rcvd, delivered, ack" + seq + " sent");
                        ackSent[waitSeq] = true;
                        int index;
                        for (int i = 0; i < RCVD_WIND_SIZE; i++) { // 缓存窗口右移,寻找下一个待接收的seq
                            index = (i + waitSeq + 1) % SEQ_SIZE;
                            if (ackSent[index] == false) {
                                waitSeq = index;
                                break;
                            }
                        }
                        if (waitSeq == seq) { // 窗口其它包均已缓存，则直接移至窗口末尾。新建窗口
                            waitSeq = (RCVD_WIND_SIZE + waitSeq) % SEQ_SIZE;
                        }
                        System.out.println(type + "waitseq " + waitSeq);
                        flushWindow(); // 更新窗口外的序列
                        // System.out.println(new
                        // String(inputPacket.getData(), 1, 10,
                        // Charset.forName("UTF-8")));
                    } else { // 不是期待的，进行缓存
                        ackSent[seq] = true;
                        System.out.println(type + "pkt" + seq + " rcvd, buffered, ack" + seq + " sent");
                    }
                    buffer[0] = (byte) seq; // 构建返回Ack数据帧
                    buffer[1] = '0';
                    b = lossInLossRatio(ackLossRatio); // 模拟ack丢失
                    if (b) {
                        System.err.println(type + "The ack of " + buffer[0] + " loss");
                        continue;
                    } else {
                        outPutPacket.setData(buffer);
                        cSocket.send(outPutPacket);
                        // System.out.println("send ack" + buffer[0]);
                    }
                }
                Thread.sleep(500);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(e.getMessage());
            // TODO: handle exception
        }
    }
    
    
    // 每次窗口滑动之后，更新窗口之外的ackSent值
    private void flushWindow() {
        if (waitSeq <= (SEQ_SIZE - RCVD_WIND_SIZE)) {
            for (int i = 0; i < waitSeq; i++) {
                ackSent[i] = false;
            }
            for (int i = (waitSeq + RCVD_WIND_SIZE); i < SEQ_SIZE; i++) {
                ackSent[i] = false;
            }
        } else {
            int index = (waitSeq + RCVD_WIND_SIZE) % SEQ_SIZE;
            for (int i = index; i < waitSeq; i++) {
                ackSent[i] = false;
            }
        }

    }

    // 根据丢失率，使用随机数进行判断
    private boolean lossInLossRatio(double lossRatio) {
        int lossBound = (int) (lossRatio * 100);
        Random rand = new Random();
        int r = rand.nextInt(100);
        if (r < lossBound) {
            return true;
        }
        return false;
    }
}
