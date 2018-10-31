package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpProxy {

    private int port = 8888;
    private ServerSocket serverSocket;
    private ExecutorService executorService;    //线程池
    private final int POOL_SIZE = 4;            //单个CPU时线程池中工作线程的数目
    private int count = 0;        //线程计数
    
    public HttpProxy() throws IOException {
        serverSocket = new ServerSocket(port);
        //根据CPU数目创建线程池
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);
        System.out.println("Start server...");
    }


    public void service() {
        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                executorService.execute(new ClientHandle(socket, count++));  
//                new SingleClient(socket).start();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                
            }
        }

    }
    
    //启动代理服务器
    public static void main(String[] args) throws IOException {
        new HttpProxy().service();
    }
    

}
