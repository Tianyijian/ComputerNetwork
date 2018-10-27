package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ClientHandle implements Runnable {

    private Socket socket; // 服务器accept到连接时返回的socket
    private String host = "nstool.netease.com"; // 客户端请求的host
    private int port = 80; // 客户端请求服务器的默认端口号
    private final int TIMEOUT = 60000;      //等待超时时间 
    private int order;  //该线程在所有线程中的顺序
    private String type;    //判断http连接还是 https连接
    private static final Set<String> DiaoYuHosts = new HashSet<String>(){{add("www.baidu.com"); add("www.taobao.com");add("www.toutiao.com");}};
    private static final Set<String> ForbiddenHosts = new HashSet<String>(){{add("www.jd.com"); add("www.360.cn");}}; 
    private static final Set<String> ForbiddenUsers = new HashSet<String>(){{add("/127.0.0.1");}};
    private String HtmlUserForbidden = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>403 Forbidden</title></head><body><h1>403 Forbidden</h1><p>Current user is forbidden to access!</p></body></html>";
    private String HtmlNetForbidden = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>403 Forbidden</title></head><body><h1>403 Forbidden</h1><p>This site is forbidden to visit!</p></body></html>";
    private OutputStream sos = null;
    public ClientHandle(Socket socket, int count) {
        try {
            this.socket = socket;
            this.order = count;
            sos = this.socket.getOutputStream();
            
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void run() {
        try {

            System.err.println("Socket " + order + " accepted: " + socket.getInetAddress() + ":" + socket.getPort());
            // 屏蔽当前用户
            if (ForbiddenUsers.contains(socket.getInetAddress().toString())) {
                System.err.println("Current user is forbidden to access!" +  socket.getInetAddress());
                sos.write("HTTP/1.1 403 Forbidden\r\nDate: Wed, 21 Oct 2015 07:28:00 GMT\r\n\r\n".getBytes());
                sos.write(HtmlUserForbidden.getBytes());
                sos.flush();
                return;
            }
            BufferedReader sbr = getReader(socket);
            PrintWriter spw = getWriter(socket);
            String line = sbr.readLine();
            parseHostAndPort(line);     //获取端口号和主机
            Socket clientSocket = null;
            
//            if (type.equalsIgnoreCase("CONNECT")) { //判断是否为https CONNECT 请求
//                System.out.println(line);
////                socket.getOutputStream().write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
////                socket.getOutputStream().flush();
//                return;
//            } else {
//             
//            }
            //屏蔽某些网站
            if (ForbiddenHosts.contains(host)) {
                System.err.println("This site is forbidden to visit: " + host);
                sos.write("HTTP/1.1 403 Forbidden\r\nDate: Wed, 21 Oct 2015 07:28:00 GMT\r\n\r\n".getBytes());
                sos.write(HtmlNetForbidden.getBytes());
                return;
            }
            if (DiaoYuHosts.contains(host)) {   //钓鱼
                clientSocket = DiaoYu2();
            } else {
                clientSocket = new Socket(host, port); // 代理服务器作为客户端，向真正的服务器发送请求
                clientSocket.setSoTimeout(TIMEOUT);     //设置超时
                System.out.println("Request Information:  ");   //正常访问
                
                StringBuffer sb = new StringBuffer();   //从客户端接收报文发给真服务器
                while (!line.isEmpty()) {
                    System.out.println(line);
                    sb.append(line + "\r\n");
                    line = sbr.readLine();
                }
                sb.append("\r\n");
                OutputStream outputStream = clientSocket.getOutputStream();
                outputStream.write(sb.toString().getBytes());
                outputStream.flush();
            }
//            System.err.println("\nGet data from the true Server: ");

                                                                   
            InputStream cis = clientSocket.getInputStream();    //从真服务器获取数据发给客户端
            byte[] buff = new byte[1024];
            int len = -1;
            while((len=cis.read(buff))!=-1) {
                sos.write(buff, 0, len);
            }
            spw.write("\r\n");
            spw.flush();
            
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
//            e.printStackTrace();
        } finally {
            try {
                if (socket != null) { // 关闭连接
                    socket.close();
                }
                System.err.println("Socket " + order + " is done.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    // 向socket输出流写数据，向对方发数据
    private PrintWriter getWriter(Socket socket) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        return new PrintWriter(outputStream);
    }

    // 从socket输入流读数据，接收对方的数据
    private BufferedReader getReader(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    // 从请求行中解析主机和端口号
    private boolean parseHostAndPort(String requestLine) {
        String[] tokens = requestLine.split(" "); // e.g. GET
                                                  // http://www.baidu.com/
                                                  // HTTP/1.1
        type = tokens[0];
        host = tokens[1];
        int index = host.indexOf("//");
        if (index != -1) {
            host = host.substring(index + 2);
        }
        index = host.indexOf("/");
        if (index != -1) {
            host = host.substring(0, index);
        }
        index = host.indexOf(":");
        if (index != -1) {
            port = Integer.parseInt(host.substring(index + 1));
            host = host.substring(0, index);
        }
//        System.out.println("host: " + host);
//        System.out.println("port: " + port);
        return true;
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
    
    private Socket DiaoYu2() throws IOException {
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
        
        Socket socket1 = new Socket("nstool.netease.com", port);
        System.out.println("Request Information: ----------------------------------- ");   //正常访问
        System.out.println(sb.toString());
        socket1.getOutputStream().write(sb.toString().getBytes());
        socket1.getOutputStream().flush();
        return socket1;
    }
}
