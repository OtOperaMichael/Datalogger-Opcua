package com.lego.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ClassName: LogUtil
 * Package: com.lego.util
 * Description:
 *
 * @Author michael.zhu
 * @Create 4/16/2026 4:24 PM
 * @Version 1.0
 */


public class LogUtil {
    // 日志级别
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int LOG_LEVEL = SystemConfigUtil.getLogLevel();


    private static void logInfo(String msg) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.out.println("[" + timestamp + "] " + msg);
    }

    private static void logError(String msg) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        System.err.println("[" + timestamp + "] " + msg);
    }

    public static void logInfo(Boolean dbSave, String schema, String msg) {
        String formattedLog = "[Info]: " + schema + ", " + msg;
        String logDbMsg = "[Info]: " + msg;

        // 调用原有的 logInfo 方法
        logInfo(formattedLog);

        // 如果需要保存到数据库，则调用 saveToDB 方法
        if (dbSave) {
            DBUtil.logInfo(schema, logDbMsg);
        }

    }


    public static void logInfo(Boolean dbSave,String schema, String format, Object... args) {
        String formattedLog = "[Info]: " + schema + ", " + formatLogMessage(format, args);
        String logDbMsg = "[Info]: " + formatLogMessage(format, args);

        // 调用原有的 logInfo 方法
        logInfo(formattedLog);

        // 如果需要保存到数据库，则调用 saveToDB 方法
        if (dbSave) {
            DBUtil.logInfo(schema, logDbMsg);
        }
    }

    public static void logWarning(Boolean dbSave,String schema, String format, Object... args) {
        // 格式化日志消息
        String formattedLog = "[Warning]: " + schema + ", " + formatLogMessage(format, args);
        String logDbMsg = "[Warning]: " + formatLogMessage(format, args);

        // 调用原有的 logInfo 方法
        logInfo(formattedLog);

        // 如果需要保存到数据库，则调用 saveToDB 方法
        if (dbSave) {
            DBUtil.logInfo(schema, logDbMsg);
        }
    }

    public static void logError(Boolean dbSave,String schema, String format, Object... args) {
        // 格式化日志消息
        String formattedLog = "[Error]: " + schema + ", " + formatLogMessage(format, args);
        String logDbMsg = "[Error]: " + formatLogMessage(format, args);

        // 调用原有的 logError 方法
        logError(formattedLog);

        // 如果需要保存到数据库，则调用 saveToDB 方法
        if (dbSave) {
            DBUtil.logInfo(schema, logDbMsg);
        }
    }

    public static void logDebugL1(Boolean dbSave,String schema, String format, Object... args) {
        if (LOG_LEVEL >= 1) {
            // 格式化日志消息
            String formattedLog = "[Debug]: " + schema + ", " + formatLogMessage(format, args);
            String logDbMsg = "[Debug]: " + formatLogMessage(format, args);

            // 调用原有的 logInfo 方法
            logInfo(formattedLog);

            // 如果需要保存到数据库，则调用 saveToDB 方法
            if (dbSave) {
                DBUtil.logInfo(schema, logDbMsg);
            }
        }
    }

    public static void logDebugL2(Boolean dbSave,String schema, String format, Object... args) {
        if (LOG_LEVEL >= 2) {
            // 格式化日志消息
            String formattedLog = "Debug: "+ schema + formatLogMessage(format, args);
            String logDbMsg = "[Debug]: " + formatLogMessage(format, args);

            // 调用原有的 logInfo 方法
            logInfo(formattedLog);

            // 如果需要保存到数据库，则调用 saveToDB 方法
            if (dbSave) {
                DBUtil.logInfo(schema, logDbMsg);
            }
        }
    }


    private static String formatLogMessage(String format, Object... args) {
        if (format == null) {
            return "null";
        }

        if (args == null || args.length == 0) {
            return format;
        }

        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int formatIndex = 0;

        while (formatIndex < format.length()) {
            // 查找下一个 {}
            int placeholderStart = format.indexOf("{}", formatIndex);

            if (placeholderStart == -1) {
                // 没有更多占位符，追加剩余部分
                result.append(format.substring(formatIndex));
                break;
            }

            // 追加占位符之前的文本
            result.append(format, formatIndex, placeholderStart);

            // 替换占位符为参数值
            if (argIndex < args.length) {
                result.append(args[argIndex] == null ? "null" : args[argIndex].toString());
                argIndex++;
            } else {
                // 参数不足，保留占位符
                result.append("{}");
            }

            // 移动到下一个位置
            formatIndex = placeholderStart + 2;
        }

        // 如果还有多余的参数，追加到末尾
        if (argIndex < args.length) {
            result.append(" [");
            for (int i = argIndex; i < args.length; i++) {
                if (i > argIndex) {
                    result.append(", ");
                }
                result.append(args[i]);
            }
            result.append("]");
        }

        return result.toString();
    }
}
