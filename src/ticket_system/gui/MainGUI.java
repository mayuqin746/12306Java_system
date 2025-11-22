package ticket_system.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import ticket_system.service.TicketService;
import ticket_system.service.OrderService;
import ticket_system.entity.Order;
import ticket_system.entity.OrderStatus;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainGUI extends Application {
    private TicketService ticketService;
    private OrderService orderService;

    private TabPane tabPane;
    private TableView<TrainData> trainTable;
    private TableView<Order> orderTable;
    private TextArea terminalArea;
    private Label statusLabel;

    private Timer refreshTimer;
    private static final long REFRESH_INTERVAL = 3000; // 3秒刷新一次

    // 用于Socket通信的客户端编号
    private final String CLIENT_NO = "GUI_CLIENT_001";

    @Override
    public void start(Stage primaryStage) {
        // 初始化服务
        ticketService = TicketService.getInstance();
        orderService = OrderService.getInstance();

        // 创建主布局
        BorderPane root = new BorderPane();

        // 创建顶部状态栏
        statusLabel = new Label("系统状态: " + ticketService.getSystemState());
        ToolBar toolBar = new ToolBar(statusLabel);
        root.setTop(toolBar);

        // 创建选项卡
        tabPane = new TabPane();
        createTrainTab();
        createOrderTab();
        createTerminalTab();

        root.setCenter(tabPane);

        // 创建场景
        Scene scene = new Scene(root, 1000, 700);
        primaryStage.setTitle("票务管理系统 - 实时监控");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 启动定时刷新
        startAutoRefresh();

        // 设置关闭事件
        primaryStage.setOnCloseRequest(e -> {
            stopAutoRefresh();
            Platform.exit();
        });
    }

    /**
     * 创建车次管理选项卡
     */
    private void createTrainTab() {
        Tab trainTab = new Tab("车次管理");
        trainTab.setClosable(false);

        VBox trainBox = new VBox(10);
        trainBox.setPadding(new Insets(10));

        // 创建车次表格
        trainTable = new TableView<>();

        TableColumn<TrainData, String> trainNumberCol = new TableColumn<>("车次号");
        trainNumberCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTrainNumber()));
        trainNumberCol.setPrefWidth(100);

        TableColumn<TrainData, String> departureCol = new TableColumn<>("出发站");
        departureCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDeparture()));
        departureCol.setPrefWidth(100);

        TableColumn<TrainData, String> destinationCol = new TableColumn<>("到达站");
        destinationCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDestination()));
        destinationCol.setPrefWidth(100);

        TableColumn<TrainData, String> secondClassCol = new TableColumn<>("二等座余票");
        secondClassCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(cellData.getValue().getSecondClassSeats())));
        secondClassCol.setPrefWidth(100);

        TableColumn<TrainData, String> firstClassCol = new TableColumn<>("一等座余票");
        firstClassCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(cellData.getValue().getFirstClassSeats())));
        firstClassCol.setPrefWidth(100);

        trainTable.getColumns().addAll(trainNumberCol, departureCol, destinationCol, secondClassCol, firstClassCol);

        // 按钮面板
        HBox buttonBox = new HBox(10);

        // 刷新按钮
        Button refreshBtn = new Button("刷新数据");
        refreshBtn.setOnAction(e -> refreshTrainData());

        buttonBox.getChildren().addAll(refreshBtn);

        trainBox.getChildren().addAll(trainTable, buttonBox);
        trainTab.setContent(trainBox);
        tabPane.getTabs().add(trainTab);

        // 初始加载数据
        refreshTrainData();
    }

    /**
     * 创建订单管理选项卡
     */
    private void createOrderTab() {
        Tab orderTab = new Tab("订单管理");
        orderTab.setClosable(false);

        VBox orderBox = new VBox(10);
        orderBox.setPadding(new Insets(10));

        // 创建订单表格
        orderTable = new TableView<>();

        TableColumn<Order, String> orderIdCol = new TableColumn<>("订单号");
        orderIdCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getOrderId()));
        orderIdCol.setPrefWidth(150);

        TableColumn<Order, String> trainNumberCol = new TableColumn<>("车次");
        trainNumberCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTrainNumber()));
        trainNumberCol.setPrefWidth(80);

        TableColumn<Order, String> seatTypeCol = new TableColumn<>("座位类型");
        seatTypeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSeatType()));
        seatTypeCol.setPrefWidth(80);

        TableColumn<Order, String> passengerCountCol = new TableColumn<>("人数");
        passengerCountCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(cellData.getValue().getPassengerCount())));
        passengerCountCol.setPrefWidth(60);

        TableColumn<Order, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus().toString()));
        statusCol.setPrefWidth(100);

        TableColumn<Order, String> createTimeCol = new TableColumn<>("创建时间");
        createTimeCol.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getCreateTime().toString()));
        createTimeCol.setPrefWidth(180);

        orderTable.getColumns().addAll(orderIdCol, trainNumberCol, seatTypeCol, passengerCountCol, statusCol, createTimeCol);

        // 操作按钮
        HBox buttonBox = new HBox(10);

        Button refreshBtn = new Button("刷新订单");
        refreshBtn.setOnAction(e -> refreshOrderData());

        Button statsBtn = new Button("订单统计");
        statsBtn.setOnAction(e -> showOrderStatistics());

        buttonBox.getChildren().addAll(refreshBtn, statsBtn);

        orderBox.getChildren().addAll(orderTable, buttonBox);
        orderTab.setContent(orderBox);
        tabPane.getTabs().add(orderTab);

        // 初始加载数据
        refreshOrderData();
    }

    /**
     * 创建终端信息选项卡
     */
    private void createTerminalTab() {
        Tab terminalTab = new Tab("终端信息");
        terminalTab.setClosable(false);

        VBox terminalBox = new VBox(10);
        terminalBox.setPadding(new Insets(10));

        terminalArea = new TextArea();
        terminalArea.setEditable(false);
        terminalArea.setPrefHeight(500);

        // 添加终端信息
        updateTerminalInfo();

        Button refreshBtn = new Button("刷新终端信息");
        refreshBtn.setOnAction(e -> updateTerminalInfo());

        terminalBox.getChildren().addAll(terminalArea, refreshBtn);
        terminalTab.setContent(terminalBox);
        tabPane.getTabs().add(terminalTab);
    }

    /**
     * 刷新车次数据 - 通过Socket获取实时数据
     */
    private void refreshTrainData() {
        new Thread(() -> {
            try {
                String trainData = getTrainsFromServer();
                Platform.runLater(() -> {
                    updateTrainTable(trainData);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("错误", "获取车次数据失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    /**
     * 从服务器获取车次数据
     */
    private String getTrainsFromServer() throws IOException {
        try (Socket socket = new Socket("127.0.0.1", 8888);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 发送200消息获取所有车次信息
            String message = CLIENT_NO + "|200|";
            out.println(message);

            // 接收响应
            String response = in.readLine();
            System.out.println("收到车次数据响应: " + response);

            return response;
        }
    }

    /**
     * 更新车次表格
     */
    private void updateTrainTable(String response) {
        if (response == null || !response.startsWith("SUCCESS")) {
            trainTable.getItems().clear();
            statusLabel.setText("系统状态: " + ticketService.getSystemState() + " | 获取车次数据失败");
            return;
        }

        try {
            // 解析响应：SUCCESS|JSON数据
            String jsonData = response.substring(8); // 去掉"SUCCESS|"
            trainTable.getItems().clear();

            // 简单解析JSON数据
            // 实际应该使用JSON解析库，这里简单处理
            java.util.List<TrainData> trains = parseTrainData(jsonData);

            for (TrainData train : trains) {
                trainTable.getItems().add(train);
            }

            statusLabel.setText("系统状态: " + ticketService.getSystemState() +
                    " | 车次数量: " + trains.size() +
                    " | 最后更新: " + new java.util.Date());

        } catch (Exception e) {
            System.err.println("解析车次数据失败: " + e.getMessage());
            trainTable.getItems().clear();
        }
    }

    /**
     * 简单解析车次数据
     */
    private java.util.List<TrainData> parseTrainData(String jsonData) {
        java.util.List<TrainData> trains = new java.util.ArrayList<>();

        try {
            // 简单的JSON解析，实际应该使用Jackson等库
            // 格式: [{"trainNumber":"G1001","departure":"北京","destination":"上海","seatInventory":{"二等座":100,"一等座":50}},...]

            String[] trainObjects = jsonData.split("\\},\\{");

            for (String trainObj : trainObjects) {
                // 清理字符串
                trainObj = trainObj.replace("[{", "").replace("}]", "").replace("{", "").replace("}", "").trim();

                String trainNumber = extractJsonValue(trainObj, "trainNumber");
                String departure = extractJsonValue(trainObj, "departure");
                String destination = extractJsonValue(trainObj, "destination");

                // 提取座位库存
                String seatInventory = extractJsonValue(trainObj, "seatInventory");
                int secondClass = 0;
                int firstClass = 0;

                if (seatInventory != null) {
                    String secondClassStr = extractJsonValue(seatInventory, "二等座");
                    String firstClassStr = extractJsonValue(seatInventory, "一等座");

                    if (secondClassStr != null) secondClass = Integer.parseInt(secondClassStr);
                    if (firstClassStr != null) firstClass = Integer.parseInt(firstClassStr);
                }

                trains.add(new TrainData(trainNumber, departure, destination, secondClass, firstClass));
            }

        } catch (Exception e) {
            System.err.println("解析车次JSON失败: " + e.getMessage());
        }

        return trains;
    }

    /**
     * 从JSON字符串中提取字段值
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;

            startIndex += searchKey.length();
            int endIndex = json.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = json.indexOf("}", startIndex);
            }
            if (endIndex == -1) return null;

            String value = json.substring(startIndex, endIndex).trim();

            // 去除引号
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            return value;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 刷新订单数据
     */
    private void refreshOrderData() {
        Platform.runLater(() -> {
            orderTable.getItems().clear();
            Map<String, Order> orders = orderService.getAllOrders();
            for (Order order : orders.values()) {
                orderTable.getItems().add(order);
            }
        });
    }

    /**
     * 更新终端信息
     */
    private void updateTerminalInfo() {
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 票务系统终端信息 ===\n\n");

            // 系统状态
            sb.append("系统状态: ").append(ticketService.getSystemState()).append("\n\n");

            // 车次统计（从表格获取）
            int trainCount = trainTable.getItems().size();
            sb.append("车次统计: ").append(trainCount).append(" 个车次\n");

            // 订单统计
            Map<String, Order> orders = orderService.getAllOrders();
            long pending = orders.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.PENDING_PAYMENT)
                    .count();
            long paid = orders.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.PAID)
                    .count();
            long cancelled = orders.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.CANCELLED)
                    .count();
            long timeout = orders.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.TIMEOUT)
                    .count();

            sb.append("订单统计:\n");
            sb.append("  待支付: ").append(pending).append("\n");
            sb.append("  已支付: ").append(paid).append("\n");
            sb.append("  已取消: ").append(cancelled).append("\n");
            sb.append("  已超时: ").append(timeout).append("\n");
            sb.append("  总计: ").append(orders.size()).append("\n\n");

            // 服务器信息
            sb.append("服务器信息:\n");
            sb.append("  监听端口: 8888\n");
            sb.append("  地址: 127.0.0.1\n");
            sb.append("  运行状态: 正常\n");
            sb.append("  最后更新: ").append(new java.util.Date()).append("\n");

            terminalArea.setText(sb.toString());
        });
    }

    /**
     * 显示订单统计
     */
    private void showOrderStatistics() {
        String stats = orderService.getOrderStatistics();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("订单统计");
        alert.setHeaderText(null);
        alert.setContentText(stats);
        alert.showAndWait();
    }

    /**
     * 显示提示框
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 启动自动刷新 - 使用 Timer
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer("GUI-Refresh-Timer", true); // 守护线程

        TimerTask refreshTask = new TimerTask() {
            @Override
            public void run() {
                refreshTrainData();  // 通过Socket获取最新车次数据
                refreshOrderData();  // 从内存获取订单数据
                updateTerminalInfo(); // 更新终端信息
            }
        };

        // 立即开始执行，然后每3秒执行一次
        refreshTimer.schedule(refreshTask, 0, REFRESH_INTERVAL);

        System.out.println("GUI自动刷新已启动，刷新间隔: " + REFRESH_INTERVAL + "ms");
    }

    /**
     * 停止自动刷新
     */
    private void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            System.out.println("GUI自动刷新已停止");
        }
        if (orderService != null) {
            orderService.shutdown();
        }
        if (ticketService != null) {
            ticketService.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * 车次数据类（用于表格显示）
     */
    public static class TrainData {
        private String trainNumber;
        private String departure;
        private String destination;
        private int secondClassSeats;
        private int firstClassSeats;

        public TrainData(String trainNumber, String departure, String destination,
                         int secondClassSeats, int firstClassSeats) {
            this.trainNumber = trainNumber;
            this.departure = departure;
            this.destination = destination;
            this.secondClassSeats = secondClassSeats;
            this.firstClassSeats = firstClassSeats;
        }

        // Getter方法
        public String getTrainNumber() { return trainNumber; }
        public String getDeparture() { return departure; }
        public String getDestination() { return destination; }
        public int getSecondClassSeats() { return secondClassSeats; }
        public int getFirstClassSeats() { return firstClassSeats; }
    }
}