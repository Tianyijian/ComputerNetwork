package lab3_sr_two;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class SendThread implements Runnable{


    private DatagramChannel channel;
    private SocketAddress remoteAddr;
    
    private static int SEND_WIND_SIZE = 10; // 发送窗口大小
    private static int SEQ_SIZE = 20; // 序列号个数
    private byte[][] fileData = new byte[50][1026]; // 数据缓存
    private boolean[] ack = new boolean[SEQ_SIZE]; // 收到ACK的情况，对应0~19
    private int curSeq = 0; // 当前数据包的seq
    private int curAck = 0; // 当前等待确认的ack
    private int totalSeq = 0; // 收到的包的总数
    private int totalPacket; // 需要发送的包的总数
    private int time[] = new int[SEQ_SIZE]; // 每个包的计时器
    private static int BUFFER_LENGTH = 1026; // 缓冲区大小
    private ByteBuffer buffer; // 缓冲区
    private String type;
    
    public SendThread(String type, DatagramChannel channel, SocketAddress remoteAddr) {
        super();
        this.type = type;
        this.channel = channel;
        this.remoteAddr = remoteAddr;
        buffer = ByteBuffer.allocate(BUFFER_LENGTH);
        for (int i = 0; i < SEQ_SIZE; i++) {
            ack[i] = true;
        }
    }

    @Override
    public void run() {
        try {
            readFile(); // 将文件读入内存
            while (true) {
                if (SeqIsAvailable()) { // 判断是否可以发送新的数据包
                    fileData[totalSeq][0] = (byte) (curSeq);
                    ack[curSeq] = false;
                    System.out.printf(type + "send pkt%d\n", curSeq);
                    channel.send(ByteBuffer.wrap(fileData[totalSeq]), remoteAddr);
                    curSeq++;
                    curSeq %= SEQ_SIZE;
                    totalSeq++;
                    Thread.sleep(500);
                }
                buffer.clear();
                SocketAddress remoteAddr1 = channel.receive(buffer);
                if (remoteAddr1 == null) { // 未收到，所有计数器+1
                    int index;
                    for (int i = 0; i < SEQ_SIZE; i++) {
                        index = (i + curAck) % SEQ_SIZE;
                        if (ack[index] == false) {
                            time[index]++;
                            if (time[index] > 20) {
                                timeoutHandle(index);
                                time[index] = 0;
                            }
                        }
                    }
                } else { // 收到ACK
                    ackHandle(buffer.array()[0]);
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        
    }

    // 处理ack
    private void ackHandle(byte a) throws IOException {
        int index = (int) a;
        System.out.println(type + "rcv ack" + a);
        ack[index] = true; // 只对该ack进行确认
        time[index] = 0;
        if (curAck == index) { // 窗口滑动
            for (int i = 0; i < SEQ_SIZE; i++) { // 在已发送中寻找下一个待确认序列号
                index = (curAck + i + 1) % SEQ_SIZE;
                if (ack[i] == false) {
                    curAck = i;
                    break;
                }
            }
            if (curAck == index) {
                curAck = curSeq;
            }
        }
        for (int i = 0; i < SEQ_SIZE; i++) { // 对其它包的计时器加1
            index = (i + curAck) % SEQ_SIZE;
            if (ack[index] == false) {
                time[index]++;
                if (time[index] > 20) {
                    timeoutHandle(index);
                    time[index] = 0;
                }
            }
        }
        // System.out.println("ackhandle end!");
    }

    // 处理超时重传,只重传没收到Ack的
    private void timeoutHandle(int i) throws IOException {
        System.err.println(type + "Seq " + (i) + " Time Out!Resent!");
        int step = curSeq - i;
        step = step >= 0 ? step : step + SEQ_SIZE;
        int index = totalSeq - step;
        fileData[index][0] = (byte) (i);
        ack[i] = false;
        channel.send(ByteBuffer.wrap(fileData[index]), remoteAddr);
    }

    // 判断当前序列号是否可用
    private boolean SeqIsAvailable() {
        int step = curSeq - curAck;
        step = step >= 0 ? step : step + SEQ_SIZE;
        // 序列号是否在当前发送窗口之内
        if (step >= SEND_WIND_SIZE) {
            return false;
        }
        if (ack[curSeq]) {
            return true;
        }
        return false;
    }

    // 读取文件
    private void readFile() {
        try {
            File file = new File("src/lab3_sr_two/test.txt");
            InputStream fis = new FileInputStream(file);
            int size = fis.available(); // 记录文件总大小
            int i = 0;
            while ((fis.read(fileData[i], 1, 1024)) != -1) {
                i++;
                if (i >= 49) {
                    System.out.println("i = " + i);
                    break;
                }
            }
            totalPacket = i;
            System.out.printf("File size is %dB, each packet is 1024B, packet total num is %d\n", size, totalPacket);
            fis.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    
}
