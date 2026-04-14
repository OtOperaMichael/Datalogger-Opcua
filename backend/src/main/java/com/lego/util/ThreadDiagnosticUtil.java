package com.lego.util;

import com.lego.serverTask.GlobalDataQueue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * 线程诊断工具类
 */
public class ThreadDiagnosticUtil {

    /**
     * 检测死锁
     */
    public static long[] detectDeadlock() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return threadMXBean.findDeadlockedThreads();
    }

    /**
     * 获取特定名称模式的线程信息（返回 StringBuilder）
     * @param pattern 线程名称模式
     * @return 包含线程信息的 StringBuilder
     */
    public static StringBuilder getThreadsByNamePattern(String pattern) {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        StringBuilder sb = new StringBuilder("===== Threads matching pattern: ").append(pattern).append(" =====\n");
        int count = 0;
        
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo.getThreadName().contains(pattern)) {
                count++;
                sb.append("Thread: ").append(threadInfo.getThreadName())
                  .append(", State: ").append(threadInfo.getThreadState())
                  .append(", ID: ").append(threadInfo.getThreadId())
                  .append(", Blocked: ").append(threadInfo.getBlockedTime()).append("ms")
                  .append(", Waited: ").append(threadInfo.getWaitedTime()).append("ms\n");
                
                StackTraceElement[] stackTrace = threadInfo.getStackTrace();
                if (stackTrace.length > 0) {
                    sb.append("  Top of Stack: ").append(stackTrace[0]).append("\n");
                }
            }
        }
        
        if (count == 0) {
            sb.append("No threads found matching pattern: ").append(pattern).append("\n");
        }
        
        return sb;
    }

    /**
     * 获取线程状态
     * @param includeFullDump 是否包含完整的线程转储
     * @return
     */
    public static StringBuilder getThreadStatus(boolean includeFullDump) {
        StringBuilder diagnosticInfo = new StringBuilder();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        diagnosticInfo.append("=== [" + now.format(formatter) + "] ===\n");

        try {
            if (includeFullDump) {
                // 1. 完整线程 dump
                diagnosticInfo.append(getFullThreadDump());
                diagnosticInfo.append("\n");
            }
            
            // 2. 特定线程模式查询
            diagnosticInfo.append("=== Thread Status ===\n");
            diagnosticInfo.append(getThreadsByNamePattern("DataWriter"));
            diagnosticInfo.append(getThreadsByNamePattern("PLC-Monitor"));
            diagnosticInfo.append(getThreadsByNamePattern("Queue-Monitor"));

            // 3. 队列状态
            diagnosticInfo.append("\n=== Queue Status ===\n");
            GlobalDataQueue queue = GlobalDataQueue.getInstance();
            diagnosticInfo.append(queue.getQueueStatus()).append("\n");
            diagnosticInfo.append(queue.getConsumerPoolStatus()).append("\n");

            // 4. 死锁检测
            diagnosticInfo.append("\n=== Deadlock Detection ===\n");
            long[] deadlockedThreads = detectDeadlock();
            if (deadlockedThreads != null) {
                diagnosticInfo.append("DEADLOCK DETECTED! Thread IDs: ");
                for (long id : deadlockedThreads) {
                    diagnosticInfo.append(id).append(" ");
                }
            } else {
                diagnosticInfo.append("No deadlock detected\n");
            }

        } catch (Exception e) {
            diagnosticInfo.append("\n=== ERROR OCCURRED ===\n");
            diagnosticInfo.append("Error Type: ").append(e.getClass().getName()).append("\n");
            diagnosticInfo.append("Error Message: ").append(e.getMessage()).append("\n");
            diagnosticInfo.append("\nStack Trace:\n");

            // 将完整的堆栈信息追加到 StringBuilder
            for (StackTraceElement element : e.getStackTrace()) {
                diagnosticInfo.append("    at ").append(element.toString()).append("\n");
            }

            diagnosticInfo.append("\n=== Partial Diagnostic Info (before error) ===\n");
            diagnosticInfo.append("Note: Error occurred during diagnosis, above information may be incomplete.\n");

        }

        return diagnosticInfo;
    }
    
    /**
     * 获取线程状态（默认不包含完整转储）
     * @return
     */
    public static StringBuilder getThreadStatus() {
        return getThreadStatus(false);
    }
    
    /**
     * 获取完整的线程转储（返回 StringBuilder 格式）
     * @return
     */
    public static StringBuilder getFullThreadDump() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

        StringBuilder sb = new StringBuilder("===== Full Thread Dump =====\n");
        for (ThreadInfo threadInfo : threadInfos) {
            sb.append("Thread ID: ").append(threadInfo.getThreadId())
              .append(", Name: ").append(threadInfo.getThreadName())
              .append(", State: ").append(threadInfo.getThreadState())
              .append(", Blocked Time: ").append(threadInfo.getBlockedTime())
              .append("ms, Waited Time: ").append(threadInfo.getWaitedTime())
              .append("ms\n");

            if (threadInfo.getLockName() != null) {
                sb.append("  Lock: ").append(threadInfo.getLockName()).append("\n");
            }

            StackTraceElement[] stackTrace = threadInfo.getStackTrace();
            if (stackTrace.length > 0) {
                sb.append("  Stack Trace:\n");
                for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                    sb.append("    at ").append(stackTrace[i]).append("\n");
                }
            }
            sb.append("\n");
        }
        
        return sb;
    }

}
