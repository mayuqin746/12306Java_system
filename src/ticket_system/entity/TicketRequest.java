package ticket_system.entity;

import java.io.Serializable;

public class TicketRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String trainNumber;
    private String seatType;
    private int passengerCount;
    private String terminalId;

    public TicketRequest() {}

    public TicketRequest(String trainNumber, String seatType, int passengerCount, String terminalId) {
        this.trainNumber = trainNumber;
        this.seatType = seatType;
        this.passengerCount = passengerCount;
        this.terminalId = terminalId;
        this.requestId = generateRequestId();
    }

    private String generateRequestId() {
        return "REQ_" + System.currentTimeMillis() + "_" +
                (int)(Math.random() * 1000);
    }

    // Getter and Setter
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
    public int getPassengerCount() { return passengerCount; }
    public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }

    @Override
    public String toString() {
        return "TicketRequest{" +
                "requestId='" + requestId + '\'' +
                ", trainNumber='" + trainNumber + '\'' +
                ", seatType='" + seatType + '\'' +
                ", passengerCount=" + passengerCount +
                ", terminalId='" + terminalId + '\'' +
                '}';
    }
}