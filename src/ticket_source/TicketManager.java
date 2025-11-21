package ticket_source;

public class TicketManager {

    // 请求所有车次（展示余票）
    public String requestAllTrains(TicketSourceClient client) throws Exception {
        Message msg = new Message(200, "null");
        return client.sendMessage(msg);
    }

    // 新增车次
    public String addTrain(TicketSourceClient client,
                           String trainId, String start, String end,
                           int secondClass, int firstClass) throws Exception {

        String payload = "{"
                + "\"trainId\":\"" + trainId + "\","
                + "\"start\":\"" + start + "\","
                + "\"end\":\"" + end + "\","
                + "\"seatTypes\":{"
                + "\"二等座\":" + secondClass + ","
                + "\"一等座\":" + firstClass
                + "}"
                + "}";

        Message msg = new Message(201, payload);
        return client.sendMessage(msg);
    }

    // 给某个车次放票
    public String releaseTickets(TicketSourceClient client,
                                 String trainId,
                                 String seatType,
                                 int amount) throws Exception {

        String payload = "{"
                + "\"trainId\":\"" + trainId + "\","
                + "\"seatType\":\"" + seatType + "\","
                + "\"amount\":" + amount
                + "}";

        Message msg = new Message(202, payload);
        return client.sendMessage(msg);
    }
}
