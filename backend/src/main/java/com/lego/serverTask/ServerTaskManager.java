package com.lego.serverTask;

import com.lego.pojo.Server;
import com.lego.util.DBUtil;
import com.lego.util.LogUtil;
import com.lego.util.TemplateUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ClassName: ServerTaskManager
 * Package: lego.serverTask
 * Description: OPC UA 服务器任务管理器（纯异步模式）
 *
 * @Author michael.zhu
 * @Create 2026/2/10 19:28
 * @Version 1.0
 */
public class ServerTaskManager {

    private static volatile ServerTaskManager instance;

    // serverId -> Datalogger instance
    private final Map<String, OpcuaDatalogger> dataloggerInstances = new ConcurrentHashMap<>();

    private ServerTaskManager() {
    }

    public static ServerTaskManager getInstance() {
        if (instance == null) {
            synchronized (ServerTaskManager.class) {
                if (instance == null) {
                    instance = new ServerTaskManager();
                }
            }
        }
        return instance;
    }

    /**
     * 启动监控任务（异步模式）
     * 该方法会创建 OpcuaDatalogger 实例并调用 start()，然后立即返回
     * OPC UA 连接和订阅在后台异步运行
     *
     * @param server 服务器配置
     */
    public void startMonitoring(Server server) {
        String id = server.getId();
        
        // 检查是否已经在运行
        if (dataloggerInstances.containsKey(id)) {
            OpcuaDatalogger existingLogger = dataloggerInstances.get(id);
            if (existingLogger != null && existingLogger.isRunning()) {
                DBUtil.logWarning(server.getName(), "Task for server {} is already running", id);
                return;
            }
            // 如果存在但未运行，先清理
            dataloggerInstances.remove(id);
        }

        try {
            // 创建 datalogger 实例
            OpcuaDatalogger datalogger = new OpcuaDatalogger(server);

            // 保存实例引用
            dataloggerInstances.put(id, datalogger);

            // 启动 datalogger
            datalogger.start();

            // 检查是否启动成功
            if (datalogger.isRunning()) {
                DBUtil.logInfo(server.getName(), "Started monitoring task for server: {}", id);
            } else {
                // 启动失败，清理
                dataloggerInstances.remove(id);
                DBUtil.logWarning(server.getName(), "Failed to start monitoring task for server: {}", id);
            }

        } catch (Exception e) {
            // 启动异常，清理资源
            dataloggerInstances.remove(id);
            DBUtil.logError(server.getName(), "Error starting monitoring task for server {}: {}", id, e.getMessage());
        }
    }

    /**
     * 停止监控任务
     *
     * @param server 服务器配置
     */
    public void stopMonitoring(Server server) {
        String id = server.getId();
        
        // 移除并获取 datalogger 实例
        OpcuaDatalogger datalogger = dataloggerInstances.remove(id);
        
        if (datalogger != null) {
            try {
                // 调用 shutdown 清理资源
                datalogger.shutdown();
                LogUtil.logInfo(server.getName(), "Stopped monitoring task for server: {}", id);
                DBUtil.logInfo(server.getName(), "Stopped monitoring task for server: {}", id);
            } catch (Exception e) {
                DBUtil.logError(server.getName(), "Stopping monitoring task for server {}: {}", id, e.getMessage());
            }
        } else {
            DBUtil.logWarning(server.getName(), "No running task found for server: {}", id);
        }
    }

    /**
     * 停止所有任务（Tomcat 关闭时调用）
     */
    public void shutdown() {
        LogUtil.logInfo("app", "Shutting down all OPC UA monitoring tasks...");
        DBUtil.logInfo("app", "Shutting down all OPC UA monitoring tasks...");

        // 遍历所有 datalogger 实例并关闭
        for (Map.Entry<String, OpcuaDatalogger> entry : dataloggerInstances.entrySet()) {
            String serverId = entry.getKey();
            OpcuaDatalogger datalogger = entry.getValue();

            try {
                if (datalogger != null) {
                    datalogger.shutdown();
                }
            } catch (Exception e) {
                DBUtil.logError("app", "Shutting down datalogger for server {}: {}", serverId, e.getMessage());
            }
        }

        // 清空映射
        dataloggerInstances.clear();

        DBUtil.logInfo("app", "All OPC UA monitoring tasks shut down complete.");
    }

    /**
     * 检查指定服务器是否正在运行
     *
     * @param serverId 服务器ID
     * @return true 表示正在运行
     */
    public boolean isRunning(String serverId) {
        OpcuaDatalogger datalogger = dataloggerInstances.get(serverId);
        return datalogger != null && datalogger.isRunning();
    }

    /**
     * 获取所有正在运行的服务器ID列表
     *
     * @return 正在运行的服务器ID集合
     */
    public java.util.Set<String> getRunningServerIds() {
        return dataloggerInstances.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().isRunning())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

}
