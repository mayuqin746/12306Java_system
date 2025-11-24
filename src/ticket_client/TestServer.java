package ticket_client;

import common.Message;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8888);
        System.out.println("=== 纯净版假服务器：无死锁，极速响应 ===");

        while (true) {
            Socket client = serverSocket.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    private static void handleClient(Socket client) {
        try {
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());

            while (true) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    Message msg = (Message) obj;
                    int type = msg.getMsgType();
                    int clientID = msg.getClientNo();
                    String content = msg.getMsgPayload();

                    System.out.println("终端[" + clientID + "] 说: " + content);

                    // === 演戏逻辑 (现在对谁都一视同仁) ===

                    if (type == 1) { // 查询/同步
                        Thread.sleep(200); // 稍微模拟一点点网络延迟
                        String reply = "基础数据同步完毕：当前G101余票充足";
                        out.writeObject(new Message(0, 99, reply));
                        out.flush();
                    }
                    else if (type == 2) { // 买票
                        Thread.sleep(500);

                        // ★★★ 注意：之前那个“if (clientID == 3)”的死锁代码已经被我删干净了 ★★★
                        // 现在不管谁来，都直接成功

                        String reply = "锁定成功！状态变为[待支付]";
                        out.writeObject(new Message(0, 99, reply));
                        out.flush();
                    }
                    else if (type == 3) { // 支付
                        Thread.sleep(500);
                        String reply = "支付成功！状态变为[已出票]";
                        out.writeObject(new Message(0, 99, reply));
                        out.flush();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(">>> 终端断开");
        }
    }
}