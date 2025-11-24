package ticket_client;

import common.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Random;

public class TicketClient extends Application {

    // --- 界面控件 ---
    private TextArea displayArea;
    private TextField trainIdInput;
    private TextField countInput;
    private DatePicker datePicker;
    private ComboBox<String> seatTypeCombo;

    // --- 按钮组 ---
    private Button connectBtn;  // 连接/刷新
    private Button buyButton;   // 发起抢票
    private Button payButton;   // 支付
    private Button cancelBtn;   // 取消订单 (对应 PPT 状态 T4)
    private Button refundBtn;   // 退票 (对应 PPT 需求)
    private Button exitBtn;     // 启停控制

    private Label statusLabel;

    // --- 通信相关 ---
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = true;

    // 自动生成一个 1-100 的随机 ID，方便多开测试
    private final int MY_CLIENT_ID = new Random().nextInt(100) + 1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("12306 购票终端 - ID:" + MY_CLIENT_ID);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-font-family: 'Microsoft YaHei';"); // 稍微美化一下字体

        // 1. 顶部状态栏 & 控制
        Label idLabel = new Label("终端编号: " + MY_CLIENT_ID);
        idLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

        exitBtn = new Button("人工关闭终端");
        exitBtn.setStyle("-fx-background-color: #ffcccc; -fx-border-color: red;");
        exitBtn.setOnAction(e -> {
            log("正在关闭系统...");
            isRunning = false;
            Platform.exit();
            System.exit(0);
        });

        HBox topBar = new HBox(20, idLabel, exitBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // 2. 连接部分
        connectBtn = new Button("1. 启动并同步数据");
        connectBtn.setMaxWidth(Double.MAX_VALUE);
        connectBtn.setStyle("-fx-background-color: #add8e6;");
        connectBtn.setOnAction(e -> connectAndSync());

        // 3. 购票参数
        trainIdInput = new TextField("G101"); // 默认填好，方便演示
        trainIdInput.setPromptText("车次 (如 G101)");

        datePicker = new DatePicker(LocalDate.now()); // 默认今天
        datePicker.setPromptText("乘车日期");

        seatTypeCombo = new ComboBox<>();
        seatTypeCombo.getItems().addAll("二等座 (有票)", "一等座 (有票)", "商务座 (无票)");
        seatTypeCombo.setValue("二等座 (有票)");
        seatTypeCombo.setMaxWidth(Double.MAX_VALUE);

        countInput = new TextField("1");
        countInput.setPromptText("人数 (1-5)");

        buyButton = new Button("2. 立即抢票");
        buyButton.setMaxWidth(Double.MAX_VALUE);
        buyButton.setStyle("-fx-background-color: #orange; -fx-text-fill: white; -fx-font-weight: bold;");
        buyButton.setDisable(true); // 连接前不可用
        buyButton.setOnAction(e -> handleBuyAction());

        // 4. 订单处理区域 (支付 / 取消 / 退票)
        payButton = new Button("支付订单");
        payButton.setStyle("-fx-background-color: #90ee90;");
        payButton.setDisable(true);

        cancelBtn = new Button("取消订单"); // T4 状态要求
        cancelBtn.setStyle("-fx-background-color: #ffeba1;");
        cancelBtn.setDisable(true);

        refundBtn = new Button("我要退票"); // PPT 额外要求
        refundBtn.setStyle("-fx-background-color: #d3d3d3;");
        refundBtn.setDisable(true);

        HBox orderActionBox = new HBox(10, payButton, cancelBtn, refundBtn);
        orderActionBox.setAlignment(Pos.CENTER);

        // 绑定按钮事件
        payButton.setOnAction(e -> {
            // 发送 Type=3 代表支付
            sendMessage(new Message(MY_CLIENT_ID, 3, "支付确认"));
            log(">>> 正在支付，请稍候...");
        });

        cancelBtn.setOnAction(e -> {
            // 发送 Type=4 代表取消 (PPT 状态 T4)
            sendMessage(new Message(MY_CLIENT_ID, 4, "请求取消"));
            log(">>> 发起取消请求...");
        });

        refundBtn.setOnAction(e -> {
            // 发送 Type=5 代表退票
            sendMessage(new Message(MY_CLIENT_ID, 5, "请求退票"));
            log(">>> 发起退票请求...");
            refundBtn.setDisable(true); // 防止重复点
        });

        // 5. 日志显示
        statusLabel = new Label("当前状态: [ 离线 ]");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: gray;");

        displayArea = new TextArea();
        displayArea.setEditable(false);
        displayArea.setPrefHeight(200);
        displayArea.setWrapText(true);

        // 组装
        layout.getChildren().addAll(
                topBar, new Separator(),
                new Label("【第一步：系统接入】"), connectBtn,
                new Label("【第二步：参数设置】"),
                new HBox(5, new Label("车次:"), trainIdInput, new Label("人数:"), countInput),
                new Label("日期:"), datePicker,
                new Label("席位:"), seatTypeCombo,
                buyButton,
                new Separator(),
                new Label("【第三步：订单处理】"), orderActionBox,
                new Separator(),
                statusLabel,
                new Label("系统日志:"), displayArea
        );

        Scene scene = new Scene(layout, 420, 750);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- 核心逻辑 ---

    private void connectAndSync() {
        new Thread(() -> {
            try {
                log("正在连接服务器...");
                // ★★★ 如果是在不同电脑测试，请修改 localhost 为服务器 IP ★★★
                socket = new Socket("localhost", 8888);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                log("连接成功！发送同步请求...");
                // 发送 Type=1 请求同步
                sendMessage(new Message(MY_CLIENT_ID, 1, "SYNC_REQUEST"));

                Platform.runLater(() -> {
                    connectBtn.setText("已连接 (点击可刷新)");
                    connectBtn.setStyle("-fx-background-color: #e0e0e0;");
                    buyButton.setDisable(false); // 允许买票
                    updateStatus("就绪 (T1)", "green");
                });

                // 启动监听线程
                startListening();

            } catch (Exception e) {
                log("连接失败: " + e.getMessage());
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "无法连接服务器，请确认Server已启动！");
                    alert.show();
                });
            }
        }).start();
    }

    private void handleBuyAction() {
        String train = trainIdInput.getText();
        String countStr = countInput.getText();
        LocalDate date = datePicker.getValue();
        String seat = seatTypeCombo.getValue();

        // 1. 本地拦截校验 (PPT Page 6)
        if (train.isEmpty() || date == null) { log("参数不完整！"); return; }
        if (seat.contains("无票")) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "该席位无票，请勿选择！");
            alert.show();
            return;
        }

        try {
            int count = Integer.parseInt(countStr);
            if (count < 1 || count > 5) {
                log("【拦截】人数必须在 1-5 人之间！");
                Alert alert = new Alert(Alert.AlertType.WARNING, "单次购票仅限 1-5 人");
                alert.show();
                return;
            }

            // 2. 发送购票请求 Type=2
            // 格式约定：车次,日期,席位,人数
            String payload = train + "," + date.toString() + "," + seat + "," + count;
            sendMessage(new Message(MY_CLIENT_ID, 2, payload));

            updateStatus("请求中...", "orange");
            buyButton.setDisable(true); // 防止重复点击

        } catch (Exception e) {
            log("请输入正确的人数！");
        }
    }

    // --- 监听服务器消息 (核心状态机流转) ---
    private void startListening() {
        new Thread(() -> {
            try {
                while (isRunning) {
                    Object obj = in.readObject();
                    if (obj instanceof Message) {
                        Message msg = (Message) obj;
                        String content = msg.getMsgPayload();

                        // 切换回 UI 线程更新界面
                        Platform.runLater(() -> handleServerMessage(content));
                    }
                }
            } catch (Exception e) {
                if(isRunning) log("与服务器断开连接");
            }
        }).start();
    }

    private void handleServerMessage(String content) {
        log("收到: " + content);

        // 1. 收到锁定成功 -> 进入 T2 待支付
        if (content.contains("锁定") || content.contains("待支付")) {
            updateStatus("待支付 (T2)", "red");
            payButton.setDisable(false);    // 允许支付
            cancelBtn.setDisable(false);    // 允许取消
            buyButton.setDisable(true);     // 锁定期间不能再买
            refundBtn.setDisable(true);
            log("【提示】席位已锁定，请在 1 分钟内支付！");
        }

        // 2. 收到出票成功 -> 进入 T3 已支付
        else if (content.contains("已出票") || content.contains("成功")) {
            updateStatus("购票成功 (T3)", "blue");
            payButton.setDisable(true);
            cancelBtn.setDisable(true);
            buyButton.setDisable(false);    // 可以买下一张
            refundBtn.setDisable(false);    // 允许退票

            // 视觉小花招：把刚买的席位改成“无票”
            updateSeatComboToSoldOut();

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "恭喜！购票成功！");
            alert.show();
        }

        // 3. 收到订单取消 -> 进入 T4 已取消
        else if (content.contains("已取消")) {
            updateStatus("已取消 (T4)", "gray");
            resetButtonsToReady();
            log("【提示】订单已成功取消，可以重新购票。");
        }

        // 4. 收到超时通知 -> 进入 T5 已超时
        else if (content.contains("超时")) {
            updateStatus("已超时 (T5)", "gray");
            resetButtonsToReady();
            Alert alert = new Alert(Alert.AlertType.WARNING, "支付超时，订单已自动关闭！");
            alert.show();
        }

        // 5. 收到退票成功
        else if (content.contains("已退票")) {
            updateStatus("已退票", "black");
            refundBtn.setDisable(true);
            log("【提示】退票成功，票额已释放。");
        }

        // 6. 抢票失败/无票
        else if (content.contains("无票") || content.contains("失败")) {
            updateStatus("抢票失败", "black");
            buyButton.setDisable(false); // 允许重试
            payButton.setDisable(true);
            cancelBtn.setDisable(true);
            Alert alert = new Alert(Alert.AlertType.ERROR, "抢票失败：" + content);
            alert.show();
        }
    }

    // 恢复按钮到初始就绪状态
    private void resetButtonsToReady() {
        payButton.setDisable(true);
        cancelBtn.setDisable(true);
        refundBtn.setDisable(true);
        buyButton.setDisable(false);
    }

    // 视觉效果：把选中的席位变成“售罄”
    private void updateSeatComboToSoldOut() {
        String current = seatTypeCombo.getValue();
        if (current != null && current.contains("有票")) {
            String soldOutText = current.replace("有票", "售罄");
            seatTypeCombo.getItems().remove(current);
            seatTypeCombo.getItems().add(soldOutText);
            seatTypeCombo.setValue(soldOutText);
        }
    }

    private void sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            log("发送 -> Type:" + msg.getMsgType() + " 内容:" + msg.getMsgPayload());
        } catch (Exception e) {
            log("发送失败: " + e.getMessage());
        }
    }

    private void updateStatus(String text, String color) {
        statusLabel.setText("当前状态: [ " + text + " ]");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    private void log(String text) {
        // 保证日志始终滚动到最下方
        Platform.runLater(() -> {
            displayArea.appendText(text + "\n");
            displayArea.setScrollTop(Double.MAX_VALUE);
        });
    }
}