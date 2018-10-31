package lab1_httpproxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.print.attribute.standard.Sides;

public class ClientHandle implements Runnable {

    private Socket socket; // 服务器accept到连接时返回的socket
    private String host = "nstool.netease.com"; // 客户端请求的host
    private int port = 80; // 客户端请求服务器的默认端口号
    private String requestLine; // 请求首行
    private final int TIMEOUT = 60000; // 等待超时时间
    private int order; // 该线程在所有线程中的顺序
    private String type; // 判断http连接还是 https连接
    private OutputStream sos = null, cos = null;
    private InputStream sis = null, cis = null;
    private BufferedReader sbr = null;
    private PrintWriter spw = null;
    private Socket cSocket = null; // 服务器端作为客户端向目标服务器发送的socket
    private static final Set<String> FishingHosts = new HashSet<String>() { // 要钓鱼的网站
        {
            add("www.baidu.com");
            add("www.taobao.com");
            add("www.toutiao.com");
        }
    };
    private static final Set<String> ForbiddenHosts = new HashSet<String>() { // 被禁止访问的网站
        {
            add("www.jd.com");
            add("www.360.cn");
        }
    };
    private static final Set<String> ForbiddenUsers = new HashSet<String>() { // 被禁止访问的用户ip
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
            spw = new PrintWriter(sos);

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

            if (!Fishing()) { // 钓鱼未成功，正常进行
                sendToRealServer(); // 向真正的服务器发送请求
            }

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
    private void parseHostAndPort(String requestLine) {
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
        // System.out.println("host: " + host);
        // System.out.println("port: " + port);
    }

    // 将要访问的网页引导向另一个网页,成功引导返回true
    // 钓鱼
    private boolean Fishing() throws IOException {
        if (!FishingHosts.contains(host)) {
            return false;
        }
        StringBuffer sb = new StringBuffer();
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

        cSocket = new Socket("nstool.netease.com", port);
        cSocket.setSoTimeout(TIMEOUT); // 设置超时
        System.out.println("Request Information: ----------------------------------- "); // 正常访问
        System.out.println(sb.toString());
        cos = cSocket.getOutputStream();
        cis = cSocket.getInputStream();
        cos.write(sb.toString().getBytes());
        cos.flush();
        return true;
    }

    // 进行用户过滤,过滤成功返回true
    // 屏蔽某些网站
    private boolean netFilter() throws IOException {
        if (ForbiddenHosts.contains(host)) {
            System.err.println("This site is forbidden to visit: " + host);
            sos.write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
            String HtmlNetForbidden = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>403 Forbidden</title></head>"
                    + "<body><h1 align=\"center\">403 Forbidden</h1><p align=\"center\">This site is forbidden to visit!</p></body></html>";
            sos.write(HtmlNetForbidden.getBytes());
            return true;
        }
        return false;
    }

    // 进行用户过滤
    private boolean userFilter() throws IOException {

        // 屏蔽某些网站
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
    // 向真正的服务器发送请求
    private void sendToRealServer() throws IOException {
        cSocket = new Socket(host, port); // 代理服务器作为客户端，向真正的服务器发送请求
        cSocket.setSoTimeout(TIMEOUT); // 设置超时
        cos = cSocket.getOutputStream();
        cis = cSocket.getInputStream();
        System.out.println("Request Information:  "); // 正常访问

        StringBuffer sb = new StringBuffer(); // 从客户端接收报文发给真服务器
        String line = requestLine;
        while (!line.isEmpty()) {
            System.out.println(line);
            sb.append(line + "\r\n");
            line = sbr.readLine();
        }
        sb.append("\r\n");
        cos.write(sb.toString().getBytes());
        cos.flush();
    }
    //// 获得目标服务器的数据返回给用户

    // 获得目标服务器的数据返回给用户
    private void returnDataToClient() throws IOException {
        byte[] buff = new byte[1024]; // 从真服务器获取数据发给客户端
        int len = -1;
        while ((len = cis.read(buff)) != -1) {
            sos.write(buff, 0, len);
        }
        spw.write("\r\n");
        spw.flush();
    }
}
