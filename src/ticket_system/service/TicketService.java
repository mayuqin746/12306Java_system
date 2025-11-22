package ticket_system.service;

import ticket_system.entity.Order;
import ticket_system.entity.OrderStatus;
import ticket_system.entity.SystemState;
import ticket_system.entity.TicketRequest;

public class TicketService {
    private static TicketService instance;
    private OrderService orderService;
    private TrainService trainService;
    private SystemState currentState = SystemState.READY;

    private TicketService() {
        this.orderService = OrderService.getInstance();
        this.trainService = TrainService.getInstance();
    }

    public static synchronized TicketService getInstance() {
        if (instance == null) {
            instance = new TicketService();
        }
        return instance;
    }

    /**
     * 处理购票请求
     */
    public String handlePurchase(TicketRequest request) {
        System.out.println("处理购票请求: " + request);

        // 参数验证
        if (!validateRequest(request)) {
            return "ERROR|请求参数不合法";
        }

        // T1 → T2: 就绪状态 → 处理中状态
        if (currentState == SystemState.READY) {
            synchronized (this) {
                currentState = SystemState.PROCESSING;
                System.out.println("系统状态: READY → PROCESSING");
            }
        }

        try {
            // 向票源系统锁定票源
            String lockResult = trainService.lockTickets(
                    request.getTrainNumber(),
                    request.getSeatType(),
                    request.getPassengerCount()
            );

            if (!lockResult.startsWith("SUCCESS")) {
                return lockResult; // 直接返回错误信息
            }

            // 创建订单
            Order order = new Order(request);
            orderService.createOrder(order);

            // 返回成功响应
            return "SUCCESS|订单创建成功:" + order.getOrderId() +
                    "|请在1分钟内完成支付";

        } catch (Exception e) {
            return "ERROR|系统处理异常: " + e.getMessage();
        }
    }

    /**
     * 确认支付
     */
    public String confirmPayment(String orderId) {
        try {
            boolean success = orderService.confirmPayment(orderId);
            if (success) {
                updateSystemState();
                return "SUCCESS|支付确认成功";
            } else {
                return "ERROR|支付确认失败，订单不存在或状态不正确";
            }
        } catch (Exception e) {
            return "ERROR|支付确认异常: " + e.getMessage();
        }
    }

    /**
     * 取消订单
     */
    public String cancelOrder(String orderId) {
        try {
            Order order = orderService.getOrder(orderId);
            if (order == null) {
                return "ERROR|订单不存在";
            }

            // 向票源系统释放票源
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                String releaseResult = trainService.releaseTickets(
                        order.getTrainNumber(),
                        order.getSeatType(),
                        order.getPassengerCount()
                );

                if (!releaseResult.startsWith("SUCCESS")) {
                    return releaseResult;
                }
            }

            boolean success = orderService.cancelOrder(orderId);
            if (success) {
                updateSystemState();
                return "SUCCESS|订单取消成功";
            } else {
                return "ERROR|订单取消失败";
            }
        } catch (Exception e) {
            return "ERROR|取消订单异常: " + e.getMessage();
        }
    }

    /**
     * 查询订单状态
     */
    public String queryOrder(String orderId) {
        try {
            Order order = orderService.getOrder(orderId);
            if (order == null) {
                return "ERROR|订单不存在";
            }

            return "SUCCESS|订单状态:" + order.getStatus() +
                    "|车次:" + order.getTrainNumber() +
                    "|座位:" + order.getSeatType() +
                    "|人数:" + order.getPassengerCount() +
                    "|创建时间:" + order.getCreateTime();
        } catch (Exception e) {
            return "ERROR|查询订单异常: " + e.getMessage();
        }
    }

    /**
     * 获取订单统计信息
     */
    public String getOrderStatistics() {
        try {
            return "SUCCESS|" + orderService.getOrderStatistics();
        } catch (Exception e) {
            return "ERROR|获取订单统计失败: " + e.getMessage();
        }
    }

    /**
     * 验证请求参数
     */
    private boolean validateRequest(TicketRequest request) {
        if (request.getPassengerCount() < 1 || request.getPassengerCount() > 5) {
            System.out.println("参数验证失败: 人数范围错误 " + request.getPassengerCount());
            return false;
        }
        if (request.getTrainNumber() == null || request.getTrainNumber().trim().isEmpty()) {
            System.out.println("参数验证失败: 车次号为空");
            return false;
        }
        if (request.getSeatType() == null || request.getSeatType().trim().isEmpty()) {
            System.out.println("参数验证失败: 座位类型为空");
            return false;
        }
        return true;
    }

    /**
     * 更新系统状态
     */
    private void updateSystemState() {
        // 检查是否所有订单都已处理完成
        boolean allProcessed = orderService.getAllOrders().values().stream()
                .noneMatch(order -> order.getStatus() == OrderStatus.PENDING_PAYMENT);

        if (allProcessed && currentState != SystemState.READY) {
            synchronized (this) {
                currentState = SystemState.READY;
                System.out.println("系统状态: PROCESSING → READY");
            }
        }
    }

    /**
     * 获取系统状态
     */
    public SystemState getSystemState() {
        return currentState;
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        orderService.shutdown();
        System.out.println("TicketService 已关闭");
    }
}