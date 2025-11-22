package ticket_system.entity;

import java.io.Serializable;

public enum SystemState implements Serializable {
    READY,           // 就绪状态
    PROCESSING       // 处理中状态
}