package ticket_system.service;

import ticket_system.entity.Order;
import ticket_system.entity.OrderStatus;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class OrderService {
    private static OrderService instance;
    private Map<String, Order> orders = new ConcurrentHashMap<>();
    private Timer timeoutTimer;
    private static final long PAYMENT_TIMEOUT = 60 * 1000; // 1分钟
    private static final long CHECK_INTERVAL = 10 * 1000; // 10秒检查一次

    private OrderService() {
        this.timeoutTimer = new Timer("PaymentTimeoutChecker", true); // 守护线程
        startTimeoutChecker();
    }

    public static synchronized OrderService getInstance() {
        if (instance == null) {
            instance = new OrderService();
        }
        return instance;
    }

    /**
     * 创建订单
     */
    public Order createOrder(Order order) {
        orders.put(order.getOrderId(), order);
        System.out.println("创建订单: " + order.getOrderId() +
                ", 车次: " + order.getTrainNumber() +
                ", 座位: " + order.getSeatType() +
                ", 人数: " + order.getPassengerCount());
        return order;
    }

    /**
     * 根据ID获取订单
     */
    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }

    /**
     * 确认支付
     */
    public boolean confirmPayment(String orderId) {
        Order order = orders.get(orderId);
        if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.PAID);
            order.setPaidTime(new java.util.Date());
            System.out.println("订单支付确认: " + orderId);
            return true;
        }
        return false;
    }

    /**
     * 取消订单
     */
    public boolean cancelOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelTime(new java.util.Date());
            System.out.println("订单取消: " + orderId);
            return true;
        }
        return false;
    }

    /**
     * 处理支付超时
     */
    public void handlePaymentTimeout(String orderId) {
        Order order = orders.get(orderId);
        if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.TIMEOUT);
            order.setTimeoutTime(new java.util.Date());
            System.out.println("订单支付超时: " + orderId);

            // 这里可以添加超时后的清理逻辑
            // 比如通知相关系统释放资源等
        }
    }

    /**
     * 启动超时检查器 - 使用 Timer 替代 ScheduledExecutorService
     */
    private void startTimeoutChecker() {
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkAllOrdersTimeout();
            }
        }, CHECK_INTERVAL, CHECK_INTERVAL);
    }

    /**
     * 检查所有订单的超时状态
     */
    private void checkAllOrdersTimeout() {
        long currentTime = System.currentTimeMillis();
        int timeoutCount = 0;

        for (Order order : orders.values()) {
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT &&
                    currentTime - order.getCreateTime().getTime() > PAYMENT_TIMEOUT) {
                handlePaymentTimeout(order.getOrderId());
                timeoutCount++;
            }
        }

        if (timeoutCount > 0) {
            System.out.println("超时检查完成，处理了 " + timeoutCount + " 个超时订单");
        }
    }

    /**
     * 获取所有订单
     */
    public Map<String, Order> getAllOrders() {
        return new ConcurrentHashMap<>(orders);
    }

    /**
     * 根据状态获取订单
     */
    public Map<String, Order> getOrdersByStatus(OrderStatus status) {
        Map<String, Order> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, Order> entry : orders.entrySet()) {
            if (entry.getValue().getStatus() == status) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 获取订单统计信息
     */
    public String getOrderStatistics() {
        long pending = orders.values().stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING_PAYMENT)
                .count();
        long paid = orders.values().stream()
                .filter(order -> order.getStatus() == OrderStatus.PAID)
                .count();
        long cancelled = orders.values().stream()
                .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
                .count();
        long timeout = orders.values().stream()
                .filter(order -> order.getStatus() == OrderStatus.TIMEOUT)
                .count();

        return String.format("订单统计 - 待支付: %d, 已支付: %d, 已取消: %d, 已超时: %d, 总计: %d",
                pending, paid, cancelled, timeout, orders.size());
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            System.out.println("OrderService 超时检查器已关闭");
        }
    }
}