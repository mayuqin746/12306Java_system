//package ticket_source;
//
//import java.io.*;
//import java.net.Socket;
//
//public class TicketSourceClient {
//
//    private Socket socket;
//    private BufferedReader in;
//    private PrintWriter out;
//
//    public TicketSourceClient(String host, int port) throws Exception {
//        socket = new Socket(host, port);
//        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//        out = new PrintWriter(socket.getOutputStream(), true);
//        System.out.println("票源系统已连接票务系统服务器");
//    }
//
//    // 发送消息并等待票务系统回复
//    public String sendMessage(Message msg) throws Exception {
//        out.println(msg.toString());
//        return in.readLine();  // 阻塞等待
//    }
//
//    public void close() {
//        try {
//            if (socket != null) socket.close();
//        } catch (Exception ignored) {}
//    }
//}


package ticket_source;

import java.io.*;
import java.net.Socket;

public class TicketSourceClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public TicketSourceClient(String host, int port) {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("票源系统已连接票务系统服务器");
        } catch (Exception e) {
            System.out.println("❌ 无法连接票务系统服务器，请先启动票务系统！");
            socket = null;
        }
    }

    // 发送消息
    public String sendMessage(Message msg) throws Exception {
        if (socket == null) {
            return "❌ 未连接到票务系统服务器";
        }
        out.println(msg.toString());
        return in.readLine();
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
    }
}
