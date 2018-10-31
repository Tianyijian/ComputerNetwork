package test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

import org.w3c.dom.css.ElementCSSInlineStyle;

public class SingleClient {

    private Socket socket; // 服务器accept到连接时返回的socket
    private String host = "nstool.netease.com"; // 客户端请求的host
    private int port = 80; // 客户端请求服务器的端口号

    public SingleClient(Socket socket) {
        this.socket = socket;
    }

    public void start() {
        try {
            System.err.println("New connection accepted: " + socket.getInetAddress() + ":" + socket.getPort());
            BufferedReader sbr = getReader(socket);
            PrintWriter spw = getWriter(socket);
            String line = sbr.readLine();
            if (!parseHostAndPort(line)) {
                return;
            } // 获取主机和端口号
            Socket clientSocket = new Socket(host, port); // 向真正的服务器发送请求
            System.err.println("Request Information:  ");

            // PrintWriter cpw = getWriter(clientSocket); //从客户端接收报文发给真服务器
            StringBuffer sb = new StringBuffer();
            while (line != null && !line.isEmpty()) {
                System.out.println(line);
                sb.append(line + "\r\n");
                line = sbr.readLine();
            }
            sb.append("\r\n");
            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(sb.toString().getBytes());

            System.err.println("\nGet data from the true Server: ");

//            BufferedReader cbr = getReader(clientSocket); // 从真服务器接收数据发给客户端
//            StringBuffer sb1 = new StringBuffer();
//            String data = cbr.readLine();
//            while (!data.isEmpty()) {       //此处有问题，没有获得空行后的数据
//                System.out.println(data);
//                sb1.append(data + "\r\n");
//                data = cbr.readLine();
//            }
//            sb1.append("\r\n");
//            socket.getOutputStream().write(sb1.toString().getBytes());

            OutputStream sos = socket.getOutputStream();
            InputStream cis = clientSocket.getInputStream();
            byte[] buff = new byte[1024];
            int len = -1;
            while((len=cis.read(buff))!=-1) {
                sos.write(buff, 0, len);
            }
            spw.write("\r\n");
            spw.flush();

            System.err.println("Connectioned terminated!");

            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) { // 关闭连接
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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

    // 从请求行中解析主机和端口号
    private boolean parseHostAndPort(String requestLine) {
        String[] tokens = requestLine.split(" "); // e.g. GET
                                                  // http://www.baidu.com/
                                                  // HTTP/1.1
        System.out.println(requestLine);
        if (tokens[0].equals("CONNECT")) {
            return false;
        }
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
            port = Integer.parseInt(host.substring(index));
            host = host.substring(0, index);
        }
        System.out.println("host: " + host);
        System.out.println("port: " + port);
        return true;
    }

}
