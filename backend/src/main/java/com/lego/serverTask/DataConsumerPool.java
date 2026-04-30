package com.lego.serverTask;

/**
 * ClassName: DataConsumerPool
 * Package: com.lego.serverTask
 * Description:
 *
 * @Author michael.zhu
 * @Create 3/25/2026 11:10 AM
 * @Version 1.0
 */


import com.lego.util.DBUtil;
import com.lego.util.LogUtil;
import com.lego.util.SystemConfigUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 数据写入消费者线程池
 * 所有 Datalogger 共享同一个队列，由统一的消费者线程池处理
 *
 * @author michael.zhu
 */
public class DataConsumerPool {

    private final BlockingQueue<DataWriteTask> dataQueue;
    private final ScheduledExecutorService schedulerService;
    private final List<DataConsumer> consumers;

    // 消费者数量, 默认为1, 可从配置文件中读取修改
    private static final int CONSUMER_COUNT = SystemConfigUtil.getDataSaveThreads();

    // 批量写入参数
    private static final int BATCH_SIZE = SystemConfigUtil.getDataSaveBatchSize();
    private static final long FLUSH_INTERVAL_MS = SystemConfigUtil.getDataSaveInterval() * 1000L;

    // 消费者线程池状态
    private volatile boolean running = true;

    // 已处理的数据数量 用于统计 和日志
    private final AtomicInteger processedCount = new AtomicInteger(0);

    // 消费者执行状态跟踪（用于诊断）
    private final boolean[] consumerExecuting;

    public DataConsumerPool(BlockingQueue<DataWriteTask> queue) {
        this.dataQueue = queue;
        this.consumers = new ArrayList<>();
        this.consumerExecuting = new boolean[CONSUMER_COUNT];

        // 初始化数组为 false
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            consumerExecuting[i] = false;
        }

        // 创建固定大小的线程池
        this.schedulerService = Executors.newScheduledThreadPool(CONSUMER_COUNT, r -> {
            Thread t = new Thread(r, "DataWriter-Consumer-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });

        LogUtil.logInfo(true, "app", "DataConsumerPool created with {} consumers", CONSUMER_COUNT);
    }

    /**
     * 启动所有消费者
     */
    public void start() {
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            DataConsumer consumer = new DataConsumer(i);  // 直接传入索引
            consumers.add(consumer);
            // 定时执行写入任务
            schedulerService.scheduleAtFixedRate(consumer,
                    FLUSH_INTERVAL_MS,
                    FLUSH_INTERVAL_MS,
                    TimeUnit.MILLISECONDS);
        }
        running = true;

        LogUtil.logInfo(true, "app", "Started {} data consumers", CONSUMER_COUNT);
    }

    /**
     * 停止所有消费者
     */
    public void stop() {
        running = false;

        // 关闭调度线程池，停止接收新任务
        schedulerService.shutdown();
        try {
            // 等待所有任务完成，最多等待 5 秒
            if (!schedulerService.awaitTermination(5, TimeUnit.SECONDS)) {
                // 如果超时，强制中断所有任务
                schedulerService.shutdownNow();
                // 再给一点时间让任务响应中断
                if (!schedulerService.awaitTermination(2, TimeUnit.SECONDS)) {
                    LogUtil.logError(true,"app", "Scheduler did not terminate");
                }
            }
        } catch (InterruptedException e) {
            // 如果当前线程被中断，强制关闭
            schedulerService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 第四步：所有任务已停止，此时再清理队列中的剩余数据
        List<DataWriteTask> remainingTasks = new ArrayList<>();
        int drained = dataQueue.drainTo(remainingTasks);

        if (!remainingTasks.isEmpty()) {
            DataConsumer tempConsumer = new DataConsumer(CONSUMER_COUNT);
            tempConsumer.flushBatch(remainingTasks);
            LogUtil.logInfo(true, "app", "Flushed remaining {} tasks on pool stop", remainingTasks.size());
        }

        LogUtil.logInfo(true, "app", "DataConsumerPool stopped. Total processed: {}", processedCount.get());
    }

    public int getProcessingCount() {
        return processedCount.get();
    }

    /**
     * 获取线程池状态信息（用于诊断）
     */
    public String getPoolStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Consumer Pool Status:\n");
        sb.append("- Running: ").append(running).append("\n");
        sb.append("- Consumer Count: ").append(CONSUMER_COUNT).append("\n");
        sb.append("- Processed Count: ").append(processedCount.get()).append("\n");
        sb.append("- Queue Size: ").append(dataQueue.size()).append("\n");

        // 检查线程池状态
        if (schedulerService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) schedulerService;
            sb.append("- Scheduler Active Threads: ").append(executor.getActiveCount()).append("\n");
            sb.append("- Scheduler Pool Size: ").append(executor.getPoolSize()).append("\n");
            sb.append("- Scheduler Completed Tasks: ").append(executor.getCompletedTaskCount()).append("\n");
        }

        // 显示每个消费者的执行状态
        sb.append("- Consumer Execution Status:\n");
        for (int i = 0; i < consumerExecuting.length; i++) {
            sb.append("  * Consumer ").append(i).append(": ")
                    .append(consumerExecuting[i] ? "EXECUTING" : "IDLE").append("\n");
        }

        return sb.toString();
    }

    /**
     * 内部类：数据消费者
     */
    private class DataConsumer implements Runnable {

        private final int consumerIndex;

        // 通过构造函数显式传入索引
        public DataConsumer(int index) {
            this.consumerIndex = index;
        }

        @Override
        public void run() {
            // 标记为正在执行
            if (consumerIndex >= 0 && consumerIndex < consumerExecuting.length) {
                consumerExecuting[consumerIndex] = true;
            }

            try {
                consumeAndWrite();

            } catch (Throwable t) {

                // 捕获 Throwable 而不仅仅是 Exception，确保万无一失
                LogUtil.logError(true, "app", "FATAL: DataConsumer task failed unexpectedly. Consumer will attempt to continue. Error: {}", t.getMessage());
                t.printStackTrace();
                // 即使发生严重错误，也不抛出异常，让 run() 方法正常结束

            } finally {
                // 标记为执行完成
                if (consumerIndex >= 0 && consumerIndex < consumerExecuting.length) {
                    consumerExecuting[consumerIndex] = false;
                }
            }
        }


        /**
         * 执行实际的消费和批量写入
         */
        private void consumeAndWrite() throws InterruptedException {

            List<DataWriteTask> batch = new ArrayList<>(BATCH_SIZE);
            Long startTime = 0L, stopTime = 0L;

            startTime = System.currentTimeMillis();

            // 策略 1：先尝试阻塞获取一个数据（没有数据时会阻塞等待）
            DataWriteTask firstTask = dataQueue.poll(100, TimeUnit.MILLISECONDS);

            if (firstTask == null) {
                // 超时仍未获取到数据，直接返回
                return;
            }

//            LogUtil.logDebugL1(false, "app", "dequeue DataWriteTask: {}", firstTask);

            // 获取到第一个数据
            batch.add(firstTask);

            // 策略 2：使用 drainTo 批量获取剩余数据（最多 BATCH_SIZE - 1 个）
            int remaining = BATCH_SIZE - 1;
            List<DataWriteTask> tempBatch = new ArrayList<>(remaining);
            int drained = dataQueue.drainTo(tempBatch, remaining);

            if (drained > 0) {
                batch.addAll(tempBatch);
            }

            // 执行写入
            flushBatch(batch);

            stopTime = System.currentTimeMillis();
            
            // 使用占位符记录调试日志
            LogUtil.logDebugL1(true,"app", "queue size={}; consumed {} tasks in {}ms", 
                dataQueue.size(), batch.size(), (stopTime - startTime));

        }

        /**
         * 批量写入数据库
         */
        private void flushBatch(List<DataWriteTask> batch) {
            if (batch.isEmpty()) {
                return;
            }

            try {
                // 按 serverName 和 tableName 分组
                Map<String, Map<String, List<DataWriteTask>>> grouped = new HashMap<>();

                for (DataWriteTask task : batch) {
                    String serverName = task.getServerName();
                    String tableName = task.getTableName();

                    grouped.computeIfAbsent(serverName, k -> new HashMap<>())
                            .computeIfAbsent(tableName, k -> new ArrayList<>())
                            .add(task);
                }

                // 对每个 server 的每个表进行批量插入
                for (Map.Entry<String, Map<String, List<DataWriteTask>>> serverEntry : grouped.entrySet()) {
                    String serverName = serverEntry.getKey();

                    for (Map.Entry<String, List<DataWriteTask>> tableEntry : serverEntry.getValue().entrySet()) {
                        String tableName = tableEntry.getKey();
                        List<DataWriteTask> taskList = tableEntry.getValue();

                        DBUtil.insertBatchToTable(serverName, tableName, taskList);
                    }
                }

                processedCount.addAndGet(batch.size());

            } catch (Exception e) {
                LogUtil.logError(true, "app", "Batch write failed: {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

