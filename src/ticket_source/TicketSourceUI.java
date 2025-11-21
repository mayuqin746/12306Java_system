package ticket_source;

import java.util.Scanner;

public class TicketSourceUI {

    public static void main(String[] args) throws Exception {

        // 连接票务系统
        TicketSourceClient client = new TicketSourceClient("127.0.0.1", 8888);
        TicketManager manager = new TicketManager();

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n====================");
            System.out.println("     票源管理系统");
            System.out.println("====================");
            System.out.println("1. 查看全部车次");
            System.out.println("2. 新增车次");
            System.out.println("3. 给车次放票");
            System.out.println("4. 退出系统");
            System.out.print("请选择： ");

            int choice = sc.nextInt();
            sc.nextLine(); // 处理回车

            switch (choice) {
                case 1:
                    System.out.println("请求车次列表...");
                    System.out.println(manager.requestAllTrains(client));
                    break;

                case 2:
                    System.out.print("车次号：");
                    String trainId = sc.nextLine();

                    System.out.print("始发站：");
                    String start = sc.nextLine();

                    System.out.print("终点站：");
                    String end = sc.nextLine();

                    System.out.print("二等座票数：");
                    int second = sc.nextInt();

                    System.out.print("一等座票数：");
                    int first = sc.nextInt();
                    sc.nextLine();

                    System.out.println(manager.addTrain(client, trainId, start, end, second, first));
                    break;

                case 3:
                    System.out.print("车次号：");
                    String tId = sc.nextLine();

                    System.out.print("席位类型（如：二等座）：");
                    String seat = sc.nextLine();

                    System.out.print("放票数量：");
                    int amount = sc.nextInt();
                    sc.nextLine();

                    System.out.println(manager.releaseTickets(client, tId, seat, amount));
                    break;

                case 4:
                    System.out.println("退出系统...");
                    client.close();
                    return;

                default:
                    System.out.println("输入错误，请重新选择。");
            }
        }
    }
}
