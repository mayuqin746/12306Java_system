package ticket_system.entity;

import java.io.Serializable;

public enum OrderStatus implements Serializable {
    PENDING_PAYMENT,  // 待支付
    PAID,             // 已支付
    CANCELLED,        // 已取消
    TIMEOUT           // 已超时
}