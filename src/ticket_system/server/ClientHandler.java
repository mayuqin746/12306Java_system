package ticket_system.server;

import ticket_system.service.TicketService;
import ticket_system.service.TrainService;
import ticket_system.entity.TicketRequest;
import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private TicketService ticketService;
    private TrainService trainService;
    private String clientNo;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.ticketService = TicketService.getInstance();
        this.trainService = TrainService.getInstance();
        this.clientNo = "CLIENT_" + socket.getPort();
    }

    @Override
    public void run() {
        System.out.println("开始处理客户端连接: " + clientNo);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("收到客户端消息[" + clientNo + "]: " + inputLine);
                String response = processMessage(inputLine);
                out.println(response);
                System.out.println("发送响应[" + clientNo + "]: " + response);

                // 如果是关闭连接请求，则退出循环
                if ("EXIT".equalsIgnoreCase(inputLine.trim())) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("客户端连接异常[" + clientNo + "]: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("客户端连接关闭: " + clientNo);
            } catch (IOException e) {
                System.out.println("关闭客户端连接异常: " + e.getMessage());
            }
        }
    }

    /**
     * 处理消息格式: clientNo|msgType|msgPayload (msgPayload是JSON格式)
     */
    private String processMessage(String message) {
        try {
            // 解析消息格式
            String[] parts = message.split("\\|", 3);
            if (parts.length < 3) {
                return "ERROR|消息格式错误，需要：clientNo|msgType|msgPayload";
            }

            String receivedClientNo = parts[0];
            String msgType = parts[1];
            String msgPayload = parts[2];

            System.out.println("解析消息 - clientNo: " + receivedClientNo +
                    ", msgType: " + msgType +
                    ", msgPayload: " + msgPayload);

            // 根据消息类型处理
            switch (msgType) {
                case "200": // 返回所有车次信息
                    return handleGetAllTrains();

                case "201": // 新增车次
                    return handleAddTrain(msgPayload);

                case "202": // 给车次增加余票
                    return handleAddTickets(msgPayload);

                case "203": // 购票请求
                    return handlePurchase(msgPayload, receivedClientNo);

                case "204": // 确认支付
                    return handleConfirmPayment(msgPayload);

                case "205": // 取消订单
                    return handleCancelOrder(msgPayload);

                case "206": // 查询订单
                    return handleQueryOrder(msgPayload);

                case "207": // 系统状态
                    return "SUCCESS|系统状态:" + ticketService.getSystemState();

                case "208": // 订单统计
                    return ticketService.getOrderStatistics();

                default:
                    return "ERROR|不支持的消息类型: " + msgType;
            }
        } catch (Exception e) {
            return "ERROR|处理消息异常: " + e.getMessage();
        }
    }

    /**
     * 200：获取所有车次信息
     */
    private String handleGetAllTrains() {
        try {
            String trainsJson = trainService.getAllTrainsJson();
            return "SUCCESS|" + trainsJson;
        } catch (Exception e) {
            return "ERROR|获取车次信息失败: " + e.getMessage();
        }
    }

    /**
     * 201：新增车次
     * msgPayload格式: {"trainId":"G85","start":"北京","end":"上海","seatTypes":{"二等座":100,"一等座":10}}
     */
    private String handleAddTrain(String payload) {
        try {
            // 手动解析JSON
            String trainId = extractJsonValue(payload, "trainId");
            String start = extractJsonValue(payload, "start");
            String end = extractJsonValue(payload, "end");

            if (trainId == null || start == null || end == null) {
                return "ERROR|JSON格式错误，缺少必要字段";
            }

            // 创建车次基本信息
            String trainInfo = trainId + "|" + start + "|" + end;
            String result = trainService.addTrain(trainInfo);

            // 如果车次创建成功，添加座位库存
            if (result.startsWith("SUCCESS")) {
                // 解析座位类型信息
                String seatTypesJson = extractJsonObject(payload, "seatTypes");
                if (seatTypesJson != null) {
                    // 解析二等座
                    String secondClass = extractJsonValue(seatTypesJson, "二等座");
                    if (secondClass != null) {
                        String ticketInfo = trainId + "|二等座|" + secondClass;
                        trainService.addTickets(ticketInfo);
                    }

                    // 解析一等座
                    String firstClass = extractJsonValue(seatTypesJson, "一等座");
                    if (firstClass != null) {
                        String ticketInfo = trainId + "|一等座|" + firstClass;
                        trainService.addTickets(ticketInfo);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            return "ERROR|新增车次失败: " + e.getMessage();
        }
    }

    /**
     * 202：给车次增加余票
     * msgPayload格式: {"trainId":"G1002","seatType":"二等座","amount":10}
     */
    private String handleAddTickets(String payload) {
        try {
            // 手动解析JSON
            String trainId = extractJsonValue(payload, "trainId");
            String seatType = extractJsonValue(payload, "seatType");
            String amount = extractJsonValue(payload, "amount");

            if (trainId == null || seatType == null || amount == null) {
                return "ERROR|JSON格式错误，缺少必要字段";
            }

            String ticketInfo = trainId + "|" + seatType + "|" + amount;
            return trainService.addTickets(ticketInfo);
        } catch (Exception e) {
            return "ERROR|增加余票失败: " + e.getMessage();
        }
    }

    /**
     * 203：处理购票请求
     * msgPayload格式: {"trainId":"G1001","seatType":"二等座","amount":2}
     */
    private String handlePurchase(String payload, String clientNo) {
        try {
            // 手动解析JSON
            String trainId = extractJsonValue(payload, "trainId");
            String seatType = extractJsonValue(payload, "seatType");
            String amount = extractJsonValue(payload, "amount");

            if (trainId == null || seatType == null || amount == null) {
                return "ERROR|JSON格式错误，缺少必要字段";
            }

            // 验证座位类型
            if (!"一等座".equals(seatType) && !"二等座".equals(seatType)) {
                return "ERROR|不支持的座位类型，只支持：一等座、二等座";
            }

            TicketRequest request = new TicketRequest(
                    trainId, // trainNumber
                    seatType, // seatType
                    Integer.parseInt(amount), // passengerCount
                    clientNo // terminalId
            );

            return ticketService.handlePurchase(request);
        } catch (NumberFormatException e) {
            return "ERROR|人数必须是数字";
        } catch (Exception e) {
            return "ERROR|购票处理失败: " + e.getMessage();
        }
    }

    /**
     * 204：确认支付
     * msgPayload格式: {"orderId":"ORDER_123456"}
     */
    private String handleConfirmPayment(String payload) {
        try {
            String orderId = extractJsonValue(payload, "orderId");
            if (orderId == null) {
                return "ERROR|JSON格式错误，缺少orderId字段";
            }
            return ticketService.confirmPayment(orderId);
        } catch (Exception e) {
            return "ERROR|确认支付失败: " + e.getMessage();
        }
    }

    /**
     * 205：取消订单
     * msgPayload格式: {"orderId":"ORDER_123456"}
     */
    private String handleCancelOrder(String payload) {
        try {
            String orderId = extractJsonValue(payload, "orderId");
            if (orderId == null) {
                return "ERROR|JSON格式错误，缺少orderId字段";
            }
            return ticketService.cancelOrder(orderId);
        } catch (Exception e) {
            return "ERROR|取消订单失败: " + e.getMessage();
        }
    }

    /**
     * 206：查询订单
     * msgPayload格式: {"orderId":"ORDER_123456"}
     */
    private String handleQueryOrder(String payload) {
        try {
            String orderId = extractJsonValue(payload, "orderId");
            if (orderId == null) {
                return "ERROR|JSON格式错误，缺少orderId字段";
            }
            return ticketService.queryOrder(orderId);
        } catch (Exception e) {
            return "ERROR|查询订单失败: " + e.getMessage();
        }
    }

    /**
     * 从JSON字符串中提取字段值
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;

            startIndex += searchKey.length();
            int endIndex = json.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = json.indexOf("}", startIndex);
            }
            if (endIndex == -1) return null;

            String value = json.substring(startIndex, endIndex).trim();

            // 去除引号
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            return value;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从JSON字符串中提取对象
     */
    private String extractJsonObject(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;

            startIndex += searchKey.length();

            // 找到对象的开始位置
            int braceStart = json.indexOf("{", startIndex);
            if (braceStart == -1) return null;

            // 找到匹配的结束大括号
            int braceCount = 1;
            int currentIndex = braceStart + 1;

            while (braceCount > 0 && currentIndex < json.length()) {
                char c = json.charAt(currentIndex);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                currentIndex++;
            }

            if (braceCount == 0) {
                return json.substring(braceStart, currentIndex);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}