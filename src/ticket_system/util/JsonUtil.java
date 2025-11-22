package ticket_system.util;

/**
 * 简单的JSON解析工具类
 */
public class JsonUtil {

    /**
     * 从JSON字符串中提取字段值
     */
    public static String extractValue(String json, String key) {
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
     * 构建简单的JSON响应
     */
    public static String buildResponse(boolean success, String message) {
        return "{\"status\":\"" + (success ? "SUCCESS" : "ERROR") + "\",\"message\":\"" + message + "\"}";
    }
}