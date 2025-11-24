package common; // 注意：包名变了

import java.io.Serializable; // 1. 引入这个“邮寄许可证”

// 2. 加上 implements Serializable，这样才能网传
public class Message implements Serializable {

    private int clientNo;       // 3. 去掉了 = 999，变成灵活的
    private int msgType;        // 消息类型
    private String msgPayload;  // 内容

    // 构造方法：创建消息时，必须指定是谁发的(clientNo)
    public Message(int clientNo, int msgType, String msgPayload) {
        this.clientNo = clientNo;
        this.msgType = msgType;
        this.msgPayload = msgPayload;
    }

    //为了让别人能读取数据，需要加上Get方法
    public int getClientNo() { return clientNo; }
    public int getMsgType() { return msgType; }
    public String getMsgPayload() { return msgPayload; }

    @Override
    public String toString() {
        return clientNo + "|" + msgType + "|" + msgPayload;
    }
}