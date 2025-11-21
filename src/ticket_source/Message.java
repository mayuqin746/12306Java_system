package ticket_source;

public class Message {
    private int clientNo = 999;   // 票源系统的固定编号
    private int msgType;          // 消息类型
    private String msgPayload;    // JSON 或字符串内容

    public Message(int msgType, String msgPayload) {
        this.msgType = msgType;
        this.msgPayload = msgPayload;
    }

    @Override
    public String toString() {
        return clientNo + "|" + msgType + "|" + msgPayload;
    }
}
