package lab1_httpproxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class ClientHandle implements Runnable {

    private Socket socket; // 服务器accept到连接时返回的socket
    private URL url; 
    private String host = ""; // 客户端请求的host
    private int port = 80; // 客户端请求服务器的默认端口号
    private String requestLine; // 请求首行
    private final int TIMEOUT = 60000; // 等待超时时间
    private int order; // 该线程在所有线程中的顺序
    private String foldName, fileName;  //文件夹名，文件名
    private boolean fishSuccess = false;    //钓鱼是否成功
    private String fishHeader;        //钓鱼成功的请求头
    private boolean foundCache = false;     //是否找到缓存
    private String ifModifiedSince;     //上次修改时间
    private OutputStream sos = null, cos = null;
    private InputStream sis = null, cis = null;
    private BufferedReader sbr = null;
    private Socket cSocket = null; // 服务器端作为客户端向目标服务器发送的socket
    private static final Set<String> FishingHosts = new HashSet<String>() { // 要钓鱼的网站
        //
        private static final long serialVersionUID = 1L;

        {
            add("www.baidu.com");
            add("www.taobao.com");
            add("www.toutiao.com");
        }
    };
    private static final Set<String> ForbiddenHosts = new HashSet<String>() { // 被禁止访问的网站
        //
        private static final long serialVersionUID = 1L;

        {
            add("www.jd.com");
            add("www.360.cn");
        }
    };
    private static final Set<String> ForbiddenUsers = new HashSet<String>() { // 被禁止访问的用户ip
        //
        private static final long serialVersionUID = 1L;

        {
            add("/127.0.0.1");
        }
    };

    public ClientHandle(Socket socket, int count) {
        try {
            this.socket = socket; // 服务器端和客户端通信的socket
            this.order = count;
            sos = this.socket.getOutputStream();
            sis = this.socket.getInputStream();
            sbr = new BufferedReader(new InputStreamReader(sis));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void run() {
        try {
            System.err.println("Socket " + order + " accepted: " + socket.getInetAddress() + ":" + socket.getPort());

//            if (userFilter()) {
//                return; // 进行用户过滤
//            }

            requestLine = sbr.readLine();
            parseHostAndPort(requestLine); // 获取端口号和主机

            if (netFilter()) {
                return; // 屏蔽某些网站
            }

            Fishing();      //钓鱼
            FindCache();    //寻找缓存
            sendToRealServer(); // 向真正的服务器发送请求
            returnDataToClient(); // 获得目标服务器的数据返回给用户

        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (socket != null) { // 关闭连接
                    socket.close();
                }
                if (cSocket != null) {
                    cSocket.close();
                }
                System.err.println("Socket " + order + " is done.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 从请求行中解析主机和端口号
    // 解析主机和端口号
    private void parseHostAndPort(String requestLine) throws MalformedURLException {
        // e.g. GET http://www.baidu.com/ HTTP/1.1
        String[] tokens = requestLine.split(" "); 
        url = new URL(tokens[1]);
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
        // System.out.println("host: " + host);
        // System.out.println("port: " + port);
    }

    // 将要访问的网页引导向另一个网页,成功引导返回true
    // 钓鱼
    private void Fishing() throws IOException {
        if (!FishingHosts.contains(host)) {
            fishSuccess = false;            // 钓鱼失败，置全局变量为false
            return;
        }
        fishSuccess = true;     //钓鱼成功，置全局变量为true
        System.err.println("Fishing success! From " + host + " to http://nstool.netease.com/");
        host = "nstool.netease.com";        //修改 host
        StringBuffer sb = new StringBuffer();   //构建钓鱼使用的报文
        String URL = "http://nstool.netease.com/";
        sb.append("GET " + URL + " HTTP/1.1\r\n");
        sb.append("Accept: */*\r\n");
        sb.append("Accept-Encoding: gzip, deflate, br\r\n");
        sb.append("Accept-Language: zh-Hans-CN, zh-Hans; q=0.5\r\n");
        sb.append("Cache-Control: max-age=0\r\n");
        sb.append("Connection: Keep-Alive\r\n");
        sb.append("Host: nstool.netease.com\r\n");
        sb.append("Upgrade-Insecure-Requests: 1\r\n");
        sb.append(
                "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36 Edge/17.17134\r\n\r\n"); // 此处两个\r\n表示数据终止
        
        fishHeader = sb.toString();
        return;
    }

    // 进行网站过滤,过滤成功返回true
    // 屏蔽某些网站
    private boolean netFilter() throws IOException {
        if (ForbiddenHosts.contains(host)) {
            System.err.println("This site is forbidden to visit: " + host);
            sos.write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
            String HtmlNetForbidden = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>403 Forbidden</title></head>"
                    + "<body><h1 align=\"center\">403 Forbidden</h1><p align=\"center\">This site is forbidden to visit!</p></body></html>";
            sos.write(HtmlNetForbidden.getBytes());
            sos.flush();
            return true;
        }
        return false;
    }

    // 进行用户过滤
    private boolean userFilter() throws IOException {

        if (ForbiddenUsers.contains(socket.getInetAddress().toString())) {
            System.err.println("Current user is forbidden to access!" + socket.getInetAddress());
            sos.write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
            String HtmlUserForbidden = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>403 Forbidden</title></head>"
                    + "<body><h1 align=\"center\">403 Forbidden</h1><p align=\"center\">Current user is forbidden to access!</p></body></html>";

            sos.write(HtmlUserForbidden.getBytes());
            sos.flush();
            return true;
        }
        return false;
    }

    // 向真正的服务器发送请求
    private void sendToRealServer() throws IOException {
        cSocket = new Socket(host, port); // 代理服务器作为客户端，向真正的服务器发送请求
        cSocket.setSoTimeout(TIMEOUT); // 设置超时
        cos = cSocket.getOutputStream();
        cis = cSocket.getInputStream();
        System.out.println("Request Information:  "); // 正常访问
        if (fishSuccess) {      //钓鱼成功，则发送指定的HTTP 请求报文
            System.out.println(fishHeader);
            cos.write(fishHeader.getBytes());
            cos.flush();
            return;
        }
        StringBuffer sb = new StringBuffer(); // 从客户端接收报文发给真服务器
        String line = requestLine;
        while (!line.isEmpty()) {
            sb.append(line + "\r\n");
            line = sbr.readLine();
        }
        if (foundCache) {   //如果cache存在，修改请求报文，添加If-Modified-Since字段
            sb.append("If-Modified-Since: "+ ifModifiedSince +"\r\n");
        }
        sb.append("\r\n");
        System.out.println(sb.toString());
        cos.write(sb.toString().getBytes());
        cos.flush();
    }

    // 获得目标服务器的数据返回给用户
    private void returnDataToClient() throws IOException {
        File file = new File(foldName + fileName);
        FileOutputStream fpos = new FileOutputStream(file);
        boolean cacheIsNew = false;
        byte[] buff = new byte[1024]; 
        int len = -1;
        while ((len = cis.read(buff)) != -1) {  // 从真服务器获取数据发给客户端
            if (new String(buff).contains("304 Not Modified")) {    //如果服务端返回304，表示本地 cache 是新的
                cacheIsNew = true;
                break;
            }
            sos.write(buff, 0, len);
            fpos.write(buff);
        }
        sos.flush();
        fpos.close();
        if (cacheIsNew) {   //缓存是最新的，直接将本地数据返回给客户端
            InputStream fis = new FileInputStream(file);
            System.err.println("The cache is up to data!Return local data!");
            while ((len = fis.read(buff)) != -1) {  
                sos.write(buff, 0, len);
            }
            sos.flush();
            fis.close();
        }
    }
    
    // 寻找本地是否存在缓存，通过相应文件是否存在来判断
    private void FindCache() throws IOException {
        foldName = "src/lab1_httpproxy/cache/" + url.getHost() + "/";
        fileName = url.getPath().replace("/", "_").substring(1) + ".txt";
        if (fileName.equals(".txt")) {
            fileName = "root.txt";
        }
        File fold = new File(foldName);
        //如果文件夹不存在则创建
        if (!fold.exists()) {
            fold.mkdir();
            System.err.println("Cache does not exist.");
            return;
        } else {
            File file = new File(foldName + fileName);
            if (!file.exists()) {
                file.createNewFile();   //如果文件不存在，创建
                System.err.println("Cache does not exist!");
                return;
            }
            foundCache = true;      //该文件存在，代表有缓存 
            System.err.println("Find Cache");
            // 从缓存中获得最新时间
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            while((line = reader.readLine())!=null) {
                if (line.contains("Date")) {
                    ifModifiedSince = line.trim().substring(line.indexOf(" ")+1);
//                    System.err.println("-----------------" + ifModifiedSince);
                }
            }
            reader.close();
            return;
        }
    }
}
