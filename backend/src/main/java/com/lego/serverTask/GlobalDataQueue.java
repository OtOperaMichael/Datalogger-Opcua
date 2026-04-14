package com.lego.serverTask;

/**
 * ClassName: GlobalDataQueue
 * Package: com.lego.serverTask
 * Description:
 *
 * @Author michael.zhu
 * @Create 3/25/2026 11:14 AM
 * @Version 1.0
 */

import com.lego.serverTask.protocols.libplctag.CIPTag;
import com.lego.serverTask.protocols.libplctag.CIPTagGroup;
import com.lego.util.DBUtil;
import com.lego.util.SystemConfigUtil;
import com.lego.util.ThreadDiagnosticUtil;

import javax.sound.midi.Soundbank;
import java.util.LinkedHashMap;
import java.util.concurrent.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 全局数据写入队列管理器
 * 所有 Datalogger 共享同一个队列，由 DataConsumerPool 统一处理
 *
 * @author cn11taozh
 */
public class GlobalDataQueue {

    private static volatile GlobalDataQueue instance;

    // 全局唯一的数据队列
    private final BlockingQueue<DataWriteTask> dataQueue;

    // 队列容量
    private static final int QUEUE_CAPACITY = 50000;

    // 监控阈值（队列使用率超过 80% 时告警）
    private static final double WARNING_THRESHOLD = 0.8;

    // 监控间隔（每 10 秒检查一次）
    private static final long MONITOR_INTERVAL_SECONDS = 10;

    // 正常状态下每 6 次记录一次（60 秒）
    private static final int NORMAL_LOG_INTERVAL = 6;

    // 线程状态打印间隔：每 2 次（20 秒）
    private static final int THREAD_STATUS_INTERVAL = 2;

    // 消费者线程池
    private final DataConsumerPool consumerPool;

    // 监控线程
    private final ScheduledExecutorService monitorScheduler;

    // 监控循环计数器
    private final AtomicInteger monitorCycleCount = new AtomicInteger(0);

    // 日志级别
    private static final int LOG_LEVEL = SystemConfigUtil.getLogLevel();

    private GlobalDataQueue() {
        this.dataQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.consumerPool = new DataConsumerPool(dataQueue);

        // 初始化监控线程
        this.monitorScheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "Queue-Monitor-Thread");
            t.setDaemon(true);
            return t;
        });

        // 启动消费者线程池
        consumerPool.start();

        // 启动定时监控任务
        startMonitoring();

        DBUtil.writeLogToDB("app", "Global data queue initialized with capacity: " + QUEUE_CAPACITY);
        System.out.println("Global data queue initialized with capacity: " + QUEUE_CAPACITY);
    }

    public static GlobalDataQueue getInstance() {
        if (instance == null) {
            synchronized (GlobalDataQueue.class) {
                if (instance == null) {
                    instance = new GlobalDataQueue();
                }
            }
        }
        return instance;
    }

    /**
     * 添加custom监控数据到队列
     */
    public boolean enqueueCustomData(String serverName, CIPTagGroup tagGroup) {

        try {
            String tableName = tagGroup.getName();
            LinkedHashMap<String, Double> data = new LinkedHashMap<>();
            for (CIPTag tag : tagGroup.getCipTagArrayList()) {
                data.put(tag.getTagName(), tag.getValAsDouble());
            }

            DataWriteTask task = new DataWriteTask(serverName, tableName, data);
            boolean success = dataQueue.offer(task);

            if (!success) {
                // 队列已满
                DBUtil.writeLogToDB("app", "Err:: Global data queue is full! Server: " + serverName +
                        ", Queue size: " + dataQueue.size());
                System.err.println("Global data queue is full! Server: " + serverName);
            }

            return success;
        } catch (Exception e) {
            DBUtil.writeLogToDB("app", "Err:: Failed to enqueue data for server " + serverName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 启动定时监控任务
     */
    private void startMonitoring() {
        monitorScheduler.scheduleAtFixedRate(() -> {
            try {
                int currentSize = getQueueSize();
                double usagePercent = (double) currentSize / QUEUE_CAPACITY;

                // 循环计数器 +1
                int cycleCount = monitorCycleCount.incrementAndGet();

                if (usagePercent < WARNING_THRESHOLD) {
                    // 正常状态：每 6 次记录一次（约 60 秒）
                    if (cycleCount % NORMAL_LOG_INTERVAL == 0) {
                        String logMessage = String.format(
                                "Queue Status - Total: %d, Current: %d, Usage: %.2f%%, Processed: %d",
                                QUEUE_CAPACITY,
                                currentSize,
                                usagePercent * 100,
                                getProcessingCount()
                        );
                        DBUtil.writeLogToDB("app", logMessage);
                    }

                    // 每 2 次(20s)打印一次线程状态
                    if (cycleCount % THREAD_STATUS_INTERVAL == 0) {
                        // 每10秒打印线程状态
                        if (LOG_LEVEL > 1) {
                            // 每隔 6 次打印一次线程状态
                            String threadStatus = ThreadDiagnosticUtil.getThreadStatus(true).toString();
                            System.out.println(threadStatus);
                            DBUtil.writeLogToDB("app", threadStatus);
                        }
                    }

                } else {
                    // 过载时立即写入告警日志（每次都写）
                    String warningMessage = String.format(
                            "WARNING: Queue usage high! Total: %d, Current: %d, Usage: %.2f%%, Processed: %d",
                            QUEUE_CAPACITY,
                            currentSize,
                            usagePercent * 100,
                            getProcessingCount()
                    );
                    DBUtil.writeLogToDB("app", warningMessage);
                    System.err.println(warningMessage);
                }

            } catch (Exception e) {
                DBUtil.writeLogToDB("app", "Error in queue monitoring: " + e.getMessage());
                e.printStackTrace();
            }
        }, MONITOR_INTERVAL_SECONDS, MONITOR_INTERVAL_SECONDS, TimeUnit.SECONDS);

        DBUtil.writeLogToDB("app", "Queue monitoring started with interval: " + MONITOR_INTERVAL_SECONDS + "s");
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return dataQueue.size();
    }

    public int getProcessingCount() {
        return consumerPool.getProcessingCount();
    }

    /**
     * 获取队列状态信息（用于诊断）
     */
    public String getQueueStatus() {
        int currentSize = getQueueSize();
        double usagePercent = (double) currentSize / QUEUE_CAPACITY;
        return String.format(
                "Queue Status - Capacity: %d, Current: %d, Usage: %.2f%%, Processed: %d",
                QUEUE_CAPACITY,
                currentSize,
                usagePercent * 100,
                getProcessingCount()
        );
    }

    /**
     * 获取消费者线程池状态（用于诊断）
     */
    public String getConsumerPoolStatus() {
        return consumerPool.getPoolStatus();
    }

    /**
     * 关闭队列和消费者线程池
     */
    public void shutdown() {
        System.out.println("Shutting down global data queue...");
        DBUtil.writeLogToDB("app", "Shutting down global data queue...");

        // 先停止监控线程
        if (monitorScheduler != null) {
            monitorScheduler.shutdown();
            try {
                if (!monitorScheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    monitorScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitorScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 停止消费者线程池
        if (consumerPool != null) {
            consumerPool.stop();
        }

        System.out.println("Global data queue shut down complete. Remaining items: " + dataQueue.size());
        DBUtil.writeLogToDB("app", "Global data queue shut down complete. Remaining items: " + dataQueue.size());
    }


}

