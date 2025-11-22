package ticket_system;

import ticket_system.server.TicketSystemServer;
import ticket_system.service.TicketService;
import ticket_system.gui.MainGUI;
import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && "gui".equals(args[0])) {
            // 启动图形界面模式
            System.out.println("启动图形界面模式...");
            Application.launch(MainGUI.class, args);
        } else {
            // 启动控制台模式
            System.out.println("正在启动票务系统（控制台模式）...");

            TicketSystemServer server = new TicketSystemServer();

            // 添加关闭钩子，确保资源正确释放
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n收到关闭信号，正在关闭票务系统...");
                server.stop();
                TicketService.getInstance().shutdown();
                System.out.println("票务系统已安全关闭");
            }));

            // 启动服务器
            server.start();
        }
    }
}