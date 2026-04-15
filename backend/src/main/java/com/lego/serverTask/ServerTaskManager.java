package com.lego.serverTask;

import com.lego.pojo.Server;
import com.lego.util.DBUtil;
import com.lego.util.SystemConfigUtil;
import com.lego.util.TemplateUtil;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * ClassName: ServerTaskManager
 * Package: lego.serverTask
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 19:28
 * @Version 1.0
 */
public class ServerTaskManager {

    private static volatile ServerTaskManager instance;

    private static final int MAX_THREADS = SystemConfigUtil.getDataCollectionThreads();

    // 线程池：建议 core=CPU数，max=10~20
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(
                    Math.min(MAX_THREADS, Runtime.getRuntime().availableProcessors() + 2),
                    r -> {
                        Thread t = new Thread(r, "PLC-Monitor-Thread");
                        t.setDaemon(false); // 非守护线程，确保 Tomcat 关闭前能处理 shutdown
                        return t;
                    }
            );

    // serverId -> Future
    private final Map<String, ScheduledFuture<?>> runningTasks = new ConcurrentHashMap<>();

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

    // 启动监控任务
    public void startMonitoring(Server server) {
        String id = server.getId();
        if (runningTasks.containsKey(id)) {
            System.out.println("Task for server " + id + " is already running");
            DBUtil.writeLogToDB(server.getName(), "Task for server " + id + " is already running");

            return;
        }

        // 创建任务
        OpcuaDatalogger task = new OpcuaDatalogger(server);

        // 保存 datalogger 实例引用
        dataloggerInstances.put(id, task);

        // 获取sampleInterval
        int sampleInterval = Objects.requireNonNull(TemplateUtil.resolveTemplate(server)).getSampleInterval();

        // 按照 template.samplingInterval 执行一次
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                task,
                0,
                sampleInterval,
                TimeUnit.MILLISECONDS
        );

        runningTasks.put(id, future);
        System.out.println("Started monitoring task for server: " + id + ", sampleInterval:" + sampleInterval);
        DBUtil.writeLogToDB(server.getName(), "Started monitoring task for server: " + id + ", sampleInterval:" + sampleInterval);

    }

    // 停止监控任务
    public void stopMonitoring(Server server) {
        String id = server.getId();
        ScheduledFuture<?> future = runningTasks.remove(id);
        if (future != null) {
            // 中断线程
            future.cancel(true);

            // 销毁该 server 的所有 tag 资源
            OpcuaDatalogger datalogger = dataloggerInstances.remove(id);
            if (datalogger != null) {
                datalogger.destroyAllTags();
            }

            System.out.println("Stopped monitoring task for server: " + id);
            DBUtil.writeLogToDB(server.getName(), "Stopped monitoring task for server: " + id);
        }
    }

    // 停止所有任务（Tomcat 关闭时调用）
    public void shutdown() {
        System.out.println("Shutting down all PLC monitoring tasks...");
        DBUtil.writeLogToDB("app", "Shutting down all PLC monitoring tasks...");

        // 先停止所有任务并销毁 tags
        for (Map.Entry<String, ScheduledFuture<?>> entry : runningTasks.entrySet()) {
            String serverId = entry.getKey();
            ScheduledFuture<?> future = entry.getValue();

            future.cancel(true);

            // 销毁 tag 资源
            OpcuaDatalogger datalogger = dataloggerInstances.get(serverId);
            if (datalogger != null) {
                datalogger.destroyAllTags();
            }
        }

        runningTasks.clear();
        dataloggerInstances.clear();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("All PLC monitoring tasks shut down complete.");
        DBUtil.writeLogToDB("app", "All PLC monitoring tasks shut down complete.");
    }

    // 检查是否正在运行
    public boolean isRunning(String serverId) {
        ScheduledFuture<?> future = runningTasks.get(serverId);
        return future != null && !future.isCancelled() && !future.isDone();
    }

}
