package ticket_system.entity;

import java.io.Serializable;
import java.util.Date;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    private String orderId;
    private String requestId;
    private String trainNumber;
    private String seatType;
    private int passengerCount;
    private OrderStatus status;
    private Date createTime;
    private Date paidTime;
    private Date cancelTime;
    private Date timeoutTime;
    private String terminalId;

    public Order() {
        this.createTime = new Date();
        this.status = OrderStatus.PENDING_PAYMENT;
    }

    public Order(TicketRequest request) {
        this();
        this.requestId = request.getRequestId();
        this.trainNumber = request.getTrainNumber();
        this.seatType = request.getSeatType();
        this.passengerCount = request.getPassengerCount();
        this.terminalId = request.getTerminalId();
        this.orderId = generateOrderId();
    }

    private String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis() + "_" +
                (int)(Math.random() * 1000);
    }

    // Getter and Setter
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getPaidTime() { return paidTime; }
    public void setPaidTime(Date paidTime) { this.paidTime = paidTime; }
    public Date getCancelTime() { return cancelTime; }
    public void setCancelTime(Date cancelTime) { this.cancelTime = cancelTime; }
    public Date getTimeoutTime() { return timeoutTime; }
    public void setTimeoutTime(Date timeoutTime) { this.timeoutTime = timeoutTime; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", trainNumber='" + trainNumber + '\'' +
                ", seatType='" + seatType + '\'' +
                ", passengerCount=" + passengerCount +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }


}