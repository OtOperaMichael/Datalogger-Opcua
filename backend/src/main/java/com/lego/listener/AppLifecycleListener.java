package com.lego.listener;

import com.lego.util.LogUtil;
import com.lego.util.SystemConfigUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import com.lego.pojo.Server;
import com.lego.serverTask.GlobalDataQueue;
import com.lego.serverTask.ServerTaskManager;
import com.lego.service.Impl.ServerServiceImpl;
import com.lego.service.Impl.TemplateServiceImpl;
import com.lego.util.DBUtil;

import java.lang.reflect.Method;

/**
 * ClassName: AppLifecycleListener
 * Package: lego.listener
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 18:22
 * @Version 1.0
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LogUtil.logInfo("app", "Application starting...");

        // 初始化数据库
        if (DBUtil.init()) {
            // 初始化系统配置（从文件加载，如果不存在则使用默认值）
            SystemConfigUtil.getInstance();
            // 加载配置数据
            TemplateServiceImpl.getInstance();
            ServerServiceImpl serverService = ServerServiceImpl.getInstance();

            // 可选：自动启动标记为 autoStart 的 Server
            for (Server s : serverService.getAllServers()) {
                if (s.isAutoStart()) {
                    serverService.startServer(s.getId());
                }
            }

            LogUtil.logInfo("app", "Application started.");
            DBUtil.logInfo("app", "Application started.");
            return;
        }
        
        LogUtil.logWarning("app", "Application started, but failed to initialize database.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LogUtil.logInfo("app", "Application shutting down...");

        // 1. 关闭数据源（如果是 Druid）
        DBUtil.close();

        // 2. 关闭任务管理器
        ServerTaskManager.getInstance().shutdown();

        // 3. 关闭全局数据队列
        GlobalDataQueue.getInstance().shutdown();

        // 4. 清理 MySQL 废弃连接清理线程
        try {
            Class<?> clazz = Class.forName("com.mysql.cj.jdbc.AbandonedConnectionCleanupThread");
            Method method = clazz.getMethod("shutdown");
            method.invoke(null);
        } catch (Exception e) {
            LogUtil.logError("app", "Failed to shutdown MySQL cleanup thread: {}", e.getMessage());
            DBUtil.logInfo("app", "Failed to shutdown MySQL cleanup thread: {}", e.getMessage());
        }

        LogUtil.logInfo("app", "Application shut down.");
        DBUtil.logInfo("app", "Application shut down.");
    }
}