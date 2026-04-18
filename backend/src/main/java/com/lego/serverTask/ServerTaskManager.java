package com.lego.serverTask;

import com.lego.pojo.Server;
import com.lego.util.LogUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerTaskManager {

    private static volatile ServerTaskManager instance;

    // serverId -> Datalogger instance
    private final Map<String, OpcuaDatalogger> dataloggerInstances = new ConcurrentHashMap<>();

    // 线程池 处理server 启动和停止
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("OPC-UA-StartStop-" + thread.getId());
        thread.setDaemon(true);
        return thread;
    });

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
                LogUtil.logWarning(true, server.getName(), "Task for server {} is already running", id);
                return;
            }
            // 如果存在但未运行，先清理
            dataloggerInstances.remove(id);
        }

        OpcuaDatalogger datalogger = new OpcuaDatalogger(server);
        dataloggerInstances.put(id, datalogger);

        taskExecutor.submit(() -> {
            try {
                datalogger.start();

                if (datalogger.isRunning()) {
                    LogUtil.logInfo(true, server.getName(), "Started monitoring task for server: {}", id);
                } else {
                    dataloggerInstances.remove(id);
                    LogUtil.logWarning(true, server.getName(), "Failed to start monitoring task for server: {}", id);
                }
            } catch (Exception e) {
                dataloggerInstances.remove(id);
                LogUtil.logError(true, server.getName(), "Error starting monitoring task for server {}: {}", id, e.getMessage());
            }
        });

        LogUtil.logInfo(true, server.getName(), "Start request submitted for server: {} (async)", id);
    }

    /**
     * 停止监控任务
     *
     * @param server 服务器配置
     */
    public void stopMonitoring(Server server) {
        String id = server.getId();
        
        OpcuaDatalogger datalogger = dataloggerInstances.remove(id);
        
        if (datalogger != null) {
            taskExecutor.submit(() -> {
                try {
                    datalogger.shutdown();
                    LogUtil.logInfo(true, server.getName(), "Stopped monitoring task for server: {}", id);
                } catch (Exception e) {
                    LogUtil.logError(true, server.getName(), "Stopping monitoring task for server {}: {}", id, e.getMessage());
                }
            });
            
            LogUtil.logInfo(true, server.getName(), "Stop request submitted for server: {} (async)", id);
        } else {
            LogUtil.logWarning(true, server.getName(), "No running task found for server: {}", id);
        }
    }

    /**
     * 停止所有任务（Tomcat 关闭时调用）
     */
    public void shutdown() {
        LogUtil.logInfo(true, "app", "Shutting down all OPC UA monitoring tasks...");

        taskExecutor.shutdown();
        
        for (Map.Entry<String, OpcuaDatalogger> entry : dataloggerInstances.entrySet()) {
            String serverId = entry.getKey();
            OpcuaDatalogger datalogger = entry.getValue();

            try {
                if (datalogger != null) {
                    datalogger.shutdown();
                }
            } catch (Exception e) {
                LogUtil.logError(true, "app", "Shutting down datalogger for server {}: {}", serverId, e.getMessage());
            }
        }

        // 清空映射
        dataloggerInstances.clear();

        LogUtil.logInfo(true, "app", "All OPC UA monitoring tasks shut down complete.");
    }

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
