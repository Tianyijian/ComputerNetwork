package lab3_gbn_two;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class sendThread implements Runnable{

    private static int SEQ_SIZE = 20; // 序列号个数
    private static int SEND_WIND_SIZE = 10; // 发送窗口大小
    private byte[][] fileData = new byte[50][1026]; // 数据缓存
    private boolean[] ack = new boolean[SEQ_SIZE]; // 收到ACK的情况，对应1~20
    private int curSeq = 0; // 当前数据包的seq
    private int curAck = 0; // 当前等待确认的ack
    private int totalSeq = 0; // 收到的包的总数
    private int totalPacket; // 需要发送的包的总数    
    private static int BUFFER_LENGTH = 1026; // 缓冲区大小
    private ByteBuffer buffer; // 缓冲区
    private DatagramChannel channel;
    private SocketAddress remoteAddr;
    private String type;    //标识该进程调用方
    

    public sendThread(String type, DatagramChannel channel, SocketAddress remoteAddr) {
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
            int waitCount = 0;
            while (true) {
                if (SeqIsAvailable()) { // 判断当前窗口是否可以发送数据包
                    fileData[totalSeq][0] = (byte) (curSeq); // 构建发送数据包并发送
                    ack[curSeq] = false;
                    System.out.printf(type + "Send pkt%d\n", curSeq);
                    channel.send(ByteBuffer.wrap(fileData[totalSeq]), remoteAddr);
                    curSeq++;
                    curSeq %= SEQ_SIZE;
                    totalSeq++;
                    Thread.sleep(500);
                }
                buffer.clear();
                SocketAddress remoteAddr1 = channel.receive(buffer); // 非阻塞模式接收确认帧
                if (remoteAddr1 == null) { // 未收到，计数+1
                    waitCount++;
                    if (waitCount > 10) {
                        timeoutHandle(); // 触发超时重传
                        waitCount = 0;
                    }
                } else { // 收到ACK,窗口进行滑动
                    ackHandle(buffer.array()[0]);
                    waitCount = 0;
                }
                Thread.sleep(500);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(e.getMessage());
        }        
    }

    
    // 处理ack，累积确认，取数据帧的第一个字节
    private void ackHandle(byte a) {
        int index = (int) a; 
        System.out.println(type + "rcv ack" + a);
        if (curAck <= index) {  //收到的ACK前所有的序列号均设为收到
            for (int i = curAck; i <= index; i++) {
                ack[i] = true;
            }
            curAck = (index + 1) % SEQ_SIZE;
        } else {
            // ack超过了最大值，回到curAck的左边
            for (int i = curAck; i < SEQ_SIZE; i++) {
                ack[i] = true;
            }
            for (int i = 0; i <= index; i++) {
                ack[i] = true;
            }
            curAck = index + 1;
        }
    }

    // 处理超时重传，滑动窗口内的都要重传
    private void timeoutHandle() {
        System.err.println(type + "pkt" + curAck + " timeout!ReSend!");
        int index;
        for (int i = 0; i < SEND_WIND_SIZE; i++) {
            index = (i + curAck) % SEQ_SIZE;
            ack[index] = true;
        }
        totalSeq -= SEND_WIND_SIZE;
        curSeq = curAck;
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
            File file = new File("src/lab3_gbn_two/test.txt");
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
            fis.close();
            System.out.printf("File size is %dB, each packet is 1024B, packet total num is %d\n", size, totalPacket);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        // for (int i = 0; i < 20; i++) {
        // System.out.println(ack[i]);
        // }
    }

}
