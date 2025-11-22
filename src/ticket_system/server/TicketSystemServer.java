package ticket_system.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TicketSystemServer {
    private ServerSocket serverSocket;
    private boolean running = false;
    private final int PORT = 8888;
    private ExecutorService threadPool;

    public TicketSystemServer() {
        this.threadPool = Executors.newFixedThreadPool(10); // 最大10个并发连接
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;

            System.out.println("====================================");
            System.out.println("   票务系统服务器启动成功!");
            System.out.println("   监听端口: " + PORT);
            System.out.println("   服务器地址: 127.0.0.1:" + PORT);
            System.out.println("   支持的消息类型:");
            System.out.println("   200 - 获取所有车次信息");
            System.out.println("   201 - 新增车次");
            System.out.println("   202 - 给车次增加余票");
            System.out.println("   203 - 购票请求");
            System.out.println("   204 - 确认支付");
            System.out.println("   205 - 取消订单");
            System.out.println("   206 - 查询订单");
            System.out.println("   207 - 系统状态");
            System.out.println("====================================");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("接受新的客户端连接: " +
                            clientSocket.getInetAddress().getHostAddress() + ":" +
                            clientSocket.getPort());

                    // 使用线程池处理客户端连接
                    threadPool.execute(new ClientHandler(clientSocket));

                } catch (IOException e) {
                    if (running) {
                        System.out.println("接受客户端连接失败: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("启动服务器失败: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        if (threadPool != null) {
            threadPool.shutdown();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("关闭服务器Socket异常: " + e.getMessage());
            }
        }
        System.out.println("票务系统服务器已停止");
    }
}