package ticket_system.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Train implements Serializable {
    private static final long serialVersionUID = 1L;
    private String trainNumber;    // 车次号
    private String departure;      // 出发站
    private String destination;    // 到达站
    private Map<String, Integer> seatInventory; // 座位库存：座位类型 -> 数量

    public Train() {
        this.seatInventory = new HashMap<>();
    }

    public Train(String trainNumber, String departure, String destination) {
        this();
        this.trainNumber = trainNumber;
        this.departure = departure;
        this.destination = destination;
    }

    // Getter and Setter
    public String getTrainNumber() { return trainNumber; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
    public String getDeparture() { return departure; }
    public void setDeparture(String departure) { this.departure = departure; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Map<String, Integer> getSeatInventory() { return seatInventory; }
    public void setSeatInventory(Map<String, Integer> seatInventory) { this.seatInventory = seatInventory; }

    /**
     * 添加座位库存
     */
    public void addSeatInventory(String seatType, int quantity) {
        this.seatInventory.put(seatType, this.seatInventory.getOrDefault(seatType, 0) + quantity);
    }

    /**
     * 获取指定座位类型的库存
     */
    public int getSeatInventory(String seatType) {
        return this.seatInventory.getOrDefault(seatType, 0);
    }

    @Override
    public String toString() {
        return "Train{" +
                "trainNumber='" + trainNumber + '\'' +
                ", departure='" + departure + '\'' +
                ", destination='" + destination + '\'' +
                ", seatInventory=" + seatInventory +
                '}';
    }

    /**
     * 转换为JSON格式
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"trainNumber\":\"").append(trainNumber).append("\",");
        sb.append("\"departure\":\"").append(departure).append("\",");
        sb.append("\"destination\":\"").append(destination).append("\",");
        sb.append("\"seatInventory\":{");

        boolean first = true;
        for (Map.Entry<String, Integer> entry : seatInventory.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            first = false;
        }
        sb.append("}}");

        return sb.toString();
    }
}