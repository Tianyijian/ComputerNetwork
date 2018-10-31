package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.Socket;
import java.sql.Date;

public class MyClient {

    private String host = "localhost";
    private int port = 8888;
    private Socket socket;
    
    
    public MyClient() throws IOException {
//        socket = new Socket(host, port);
        socket = new Socket("nstool.netease.com", 80);
    }
    
    // 向socket输出流写数据，向对方发数据
    private PrintWriter getWriter(Socket socket) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        return new PrintWriter(outputStream, true);
    }

    // 从socket输入流读数据，接收对方的数据
    private BufferedReader getReader(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    public void start() {
        try {
            BufferedReader br = getReader(socket);
            PrintWriter pw = getWriter(socket);
            
            StringBuffer sb = new StringBuffer();
            String URL = "http://nstool.netease.com/";
            sb.append("GET "+URL+" HTTP/1.1\r\n");
            sb.append("Accept: */*\r\n");
            sb.append("Accept-Encoding: gzip, deflate, br\r\n");
            sb.append("Accept-Language: zh-Hans-CN, zh-Hans; q=0.5\r\n");
            sb.append("Cache-Control: max-age=0\r\n");
            sb.append("Connection: Keep-Alive\r\n");
            sb.append("Host: nstool.netease.com\r\n");
            sb.append("Upgrade-Insecure-Requests: 1\r\n");
            sb.append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/17.17134\r\n\r\n");     //此处两个\r\n表示数据终止
          
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(sb.toString().getBytes());
            
            String data;

            while((data = br.readLine())!=null) {
                System.out.println(data);
            }
            System.err.println("Connectioned terminated!");
            socket.close();
            
//            BufferedReader localReader = new BufferedReader(new InputStreamReader(System.in));
//            String msg = null;
//            while((msg = localReader.readLine()) != null) {
//                pw.println(msg);
//                System.out.println(br.readLine());
//                
//                if (msg.equals("bye")) {
//                    break;
//                }
//            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
    
    public static void main(String[] args) throws IOException {
        new MyClient().start();
//        String host = "CONNECT ssl.gstatic.com:443 HTTP/1.1".split(" ")[1];
//        int port = 80;
//        int n = host.indexOf("//");
//        int index =  host.indexOf("//");
//        if (index != -1) {
//            host = host.substring(index+2);       
//        }
//        index = host.indexOf("/");
//        if (index != -1) {
//            host = host.substring(0, index);
//        }
//        index = host.indexOf(":");
//        if (index != -1) {
//            port = Integer.parseInt(host.substring(index+1));
//            host = host.substring(0, index);
//        }
//        System.out.println(host);
//        System.out.println(port);
    }
    
    private Socket DiaoYu() throws IOException {
        System.err.println("You are prohibited from accessing this website: " + host);
        StringBuffer sb = new StringBuffer();
        String URL = "http://today.hit.edu.cn/";
        sb.append("GET "+URL+" HTTP/1.1\r\n");
        sb.append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/17.17134\r\n");
        sb.append("Accept-Language: zh-Hans-CN, zh-Hans; q=0.5\r\n");
        sb.append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n");
        sb.append("Upgrade-Insecure-Requests: 1\r\n");
        sb.append("Accept-Encoding: gzip, deflate\r\n");
        sb.append("Host: today.hit.edu.cn\r\n");
        sb.append("Proxy-Connection: Keep-Alive\r\n");
        
        sb.append("\r\n");
        Socket socket = new Socket("today.hit.edu.cn", port);
        System.out.println("Request Information:  ");   //正常访问
        System.out.println(sb.toString());
        socket.getOutputStream().write(sb.toString().getBytes());
        socket.getOutputStream().flush();
        return socket;
    }
    
    // if (type.equalsIgnoreCase("CONNECT")) { //判断是否为https CONNECT 请求
    // System.out.println(line);
    //// socket.getOutputStream().write("HTTP/1.1 200 Connection
    // Established\r\n\r\n".getBytes());
    //// socket.getOutputStream().flush();
    // return;
    // } else {
    //
    // }
}
