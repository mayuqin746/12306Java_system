package ticket_system;

import ticket_system.gui.MainGUI;
import javafx.application.Application;

/**
 * 专门的GUI启动类
 */
public class GUIStarter {
    public static void main(String[] args) {
        System.out.println("启动票务系统图形界面...");
        try {
            // 直接启动JavaFX应用
            Application.launch(MainGUI.class, args);
        } catch (Exception e) {
            System.err.println("启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}