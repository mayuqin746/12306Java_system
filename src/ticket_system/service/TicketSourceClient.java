package ticket_system.service;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TicketSourceClient {
    private String host;
    private int port;
    private String clientNo;
    private Map<String, Socket> connectionPool = new ConcurrentHashMap<>();

    public TicketSourceClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.clientNo = "TICKET_SYSTEM_" + System.currentTimeMillis();
    }

    /**
     * 发送消息到票源系统
     * 格式: clientNo|msgType|msgPayload
     */
    private String sendMessage(String message) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("发送到票源系统: " + message);
            out.println(message);

            String response = in.readLine();
            System.out.println("票源系统响应: " + response);
            return response;

        } catch (IOException e) {
            System.out.println("连接票源系统失败: " + e.getMessage());
            return "ERROR|连接票源系统失败";
        }
    }

    /**
     * 200：获取所有车次信息
     */
    public String getAllTrains() {
        String message = clientNo + "|200|";
        return sendMessage(message);
    }

    /**
     * 201：新增车次
     * msgPayload格式: 车次号|出发站|到达站|出发时间|到达时间
     */
    public String addTrain(String trainInfo) {
        String message = clientNo + "|201|" + trainInfo;
        return sendMessage(message);
    }

    /**
     * 202：给车次增加余票
     * msgPayload格式: 车次号|座位类型|数量
     */
    public String addTickets(String ticketInfo) {
        String message = clientNo + "|202|" + ticketInfo;
        return sendMessage(message);
    }

    /**
     * 203：锁定票源（需要与票源系统约定）
     * msgPayload格式: 车次号|座位类型|数量
     */
    public String lockTickets(String trainNumber, String seatType, int count) {
        String payload = trainNumber + "|" + seatType + "|" + count;
        String message = clientNo + "|203|" + payload;
        return sendMessage(message);
    }

    /**
     * 204：释放票源（需要与票源系统约定）
     * msgPayload格式: 车次号|座位类型|数量
     */
    public String releaseTickets(String trainNumber, String seatType, int count) {
        String payload = trainNumber + "|" + seatType + "|" + count;
        String message = clientNo + "|204|" + payload;
        return sendMessage(message);
    }

    /**
     * 205：查询余票（需要与票源系统约定）
     * msgPayload格式: 车次号|座位类型
     */
    public String queryAvailableTickets(String trainNumber, String seatType) {
        String payload = trainNumber + "|" + seatType;
        String message = clientNo + "|205|" + payload;
        return sendMessage(message);
    }

    /**
     * 检查响应是否成功
     */
    public boolean isSuccess(String response) {
        return response != null && response.startsWith("SUCCESS");
    }

    /**
     * 从响应中提取数据部分
     */
    public String extractData(String response) {
        if (response == null) return "";
        String[] parts = response.split("\\|", 2);
        return parts.length > 1 ? parts[1] : "";
    }
}