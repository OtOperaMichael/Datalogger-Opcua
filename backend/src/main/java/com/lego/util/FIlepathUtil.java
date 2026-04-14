package com.lego.util;


import java.io.File;
import java.nio.file.Paths;

/**
 * ClassName: FIleUtil
 * Package: util
 * Description:
 * Author Michael Zhu
 * Create 2025/8/25 9:27
 * Version 1.0
 */


public final class FIlepathUtil {

    //全局唯一的  文件路径
    private static final String BASE_PATH;

    // 优先使用系统属性指定的路径（生产环境用）
    // 生产服务器	添加 JVM 参数：-Dlego.data.dir=/var/lib/lego
    static {
        // 1. 优先从 JVM 系统属性读取
        String dataDir = System.getProperty("lego.data.dir");

        if (dataDir != null && !dataDir.trim().isEmpty()) {
            BASE_PATH = Paths.get(dataDir).toString();
        } else {
            // 2. 默认使用用户主目录下的 lego-server-data/
            String userHome = System.getProperty("user.home");
            BASE_PATH = Paths.get(userHome, "lego-server-data").toString();
        }

        // 3. 确保父目录存在
        File configDir = new File(BASE_PATH);
        if (configDir != null) {
            configDir.mkdirs(); // mkdirs() 是幂等的，可安全多次调用
        }
    }

    // 私有构造函数，防止实例化
    private FIlepathUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 获取 database.properties 的完整路径
     */
    public static String getDatabaseConfigPath() {
        return Paths.get(BASE_PATH, "database.properties").toString();
    }

    /**
     * 获取 system 配置路径（可选）
     */
    public static String getSystemConfigPath() {
        return Paths.get(BASE_PATH, "system.properties").toString();
    }

    /**
     * 获取 templateList 配置路径（可选）
     */
    public static String getTemplateListPath() {
        return Paths.get(BASE_PATH, "templateList.json").toString();
    }

    /**
     * 获取 serverList 配置路径（可选）
     */
    public static String getServerListPath() {
        return Paths.get(BASE_PATH, "serverList.json").toString();
    }

}