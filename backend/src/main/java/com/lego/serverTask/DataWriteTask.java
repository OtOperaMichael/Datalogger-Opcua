package com.lego.serverTask;

/**
 * ClassName: DataWriteTask
 * Package: com.lego.serverTask
 * Description:
 *
 * @Author michael.zhu
 * @Create 3/25/2026 3:16 PM
 * @Version 1.0
 */

import lombok.Getter;

import java.time.Instant;
import java.util.LinkedHashMap;

/**
 * 内部类：数据写入任务
 * @author cn11taozh
 */
@Getter
public class DataWriteTask {
    private final String serverName;
    private final String tableName;
    private final Instant timestamp;
    private final LinkedHashMap<String, Double> data;

    public DataWriteTask(String serverName, String tableName,LinkedHashMap<String, Double> data) {
        this.serverName = serverName;
        this.tableName = tableName;
        this.data = data;
        this.timestamp = Instant.now();
    }
}
