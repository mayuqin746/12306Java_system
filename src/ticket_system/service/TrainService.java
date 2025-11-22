package ticket_system.service;

import ticket_system.entity.Train;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TrainService {
    private static TrainService instance;
    private Map<String, Train> trainMap;

    private TrainService() {
        this.trainMap = new ConcurrentHashMap<>();
        initializeSampleData();
    }

    public static synchronized TrainService getInstance() {
        if (instance == null) {
            instance = new TrainService();
        }
        return instance;
    }

    /**
     * 初始化示例数据
     */
    private void initializeSampleData() {
        // 添加示例车次 - 只有一等座和二等座
        Train train1 = new Train("G1001", "北京", "上海");
        train1.addSeatInventory("二等座", 200);
        train1.addSeatInventory("一等座", 100);
        trainMap.put("G1001", train1);

        Train train2 = new Train("G1002", "北京", "上海");
        train2.addSeatInventory("二等座", 180);
        train2.addSeatInventory("一等座", 80);
        trainMap.put("G1002", train2);

        Train train3 = new Train("G2001", "北京", "广州");
        train3.addSeatInventory("二等座", 150);
        train3.addSeatInventory("一等座", 60);
        trainMap.put("G2001", train3);

        System.out.println("初始化车次数据完成，共 " + trainMap.size() + " 个车次");
    }

    /**
     * 刷新数据 - 重新加载数据
     */
    public void refreshData() {
        System.out.println("刷新车次数据...");
        // 这里可以添加从票源系统重新加载数据的逻辑
        // 目前先保持现有数据
    }

    /**
     * 获取所有车次信息（JSON格式）
     */
    public String getAllTrainsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        boolean first = true;
        for (Train train : trainMap.values()) {
            if (!first) sb.append(",");
            sb.append(train.toJson());
            first = false;
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * 新增车次
     */
    public String addTrain(String trainInfo) {
        try {
            String[] parts = trainInfo.split("\\|");
            if (parts.length != 3) {
                return "ERROR|车次信息格式错误，需要：车次号|出发站|到达站";
            }

            String trainNumber = parts[0];
            if (trainMap.containsKey(trainNumber)) {
                return "ERROR|车次已存在：" + trainNumber;
            }

            Train train = new Train(trainNumber, parts[1], parts[2]);
            trainMap.put(trainNumber, train);

            return "SUCCESS|车次添加成功：" + trainNumber;
        } catch (Exception e) {
            return "ERROR|添加车次失败：" + e.getMessage();
        }
    }

    /**
     * 给车次增加余票
     */
    public String addTickets(String ticketInfo) {
        try {
            String[] parts = ticketInfo.split("\\|");
            if (parts.length != 3) {
                return "ERROR|票务信息格式错误，需要：车次号|座位类型|数量";
            }

            String trainNumber = parts[0];
            String seatType = parts[1];
            int quantity = Integer.parseInt(parts[2]);

            if (!"一等座".equals(seatType) && !"二等座".equals(seatType)) {
                return "ERROR|不支持的座位类型，只支持：一等座、二等座";
            }

            Train train = trainMap.get(trainNumber);
            if (train == null) {
                return "ERROR|车次不存在：" + trainNumber;
            }

            if (quantity <= 0) {
                return "ERROR|数量必须大于0";
            }

            train.addSeatInventory(seatType, quantity);

            return "SUCCESS|余票添加成功：" + trainNumber + " " + seatType + " " + quantity + "张";
        } catch (NumberFormatException e) {
            return "ERROR|数量格式错误，必须是整数";
        } catch (Exception e) {
            return "ERROR|添加余票失败：" + e.getMessage();
        }
    }

    /**
     * 根据车次号获取车次信息
     */
    public Train getTrain(String trainNumber) {
        return trainMap.get(trainNumber);
    }

    /**
     * 获取所有车次
     */
    public Map<String, Train> getAllTrains() {
        return new ConcurrentHashMap<>(trainMap);
    }

    /**
     * 锁定票源
     */
    public String lockTickets(String trainNumber, String seatType, int quantity) {
        try {
            Train train = trainMap.get(trainNumber);
            if (train == null) {
                return "ERROR|车次不存在：" + trainNumber;
            }

            if (!"一等座".equals(seatType) && !"二等座".equals(seatType)) {
                return "ERROR|不支持的座位类型，只支持：一等座、二等座";
            }

            int available = train.getSeatInventory(seatType);
            if (available < quantity) {
                return "ERROR|余票不足，需要：" + quantity + "，可用：" + available;
            }

            // 锁定票源（减少库存）
            train.addSeatInventory(seatType, -quantity);

            return "SUCCESS|票源锁定成功：" + trainNumber + " " + seatType + " " + quantity + "张";
        } catch (Exception e) {
            return "ERROR|锁定票源失败：" + e.getMessage();
        }
    }

    /**
     * 释放票源
     */
    public String releaseTickets(String trainNumber, String seatType, int quantity) {
        try {
            Train train = trainMap.get(trainNumber);
            if (train == null) {
                return "ERROR|车次不存在：" + trainNumber;
            }

            if (!"一等座".equals(seatType) && !"二等座".equals(seatType)) {
                return "ERROR|不支持的座位类型，只支持：一等座、二等座";
            }

            // 释放票源（增加库存）
            train.addSeatInventory(seatType, quantity);

            return "SUCCESS|票源释放成功：" + trainNumber + " " + seatType + " " + quantity + "张";
        } catch (Exception e) {
            return "ERROR|释放票源失败：" + e.getMessage();
        }
    }
}