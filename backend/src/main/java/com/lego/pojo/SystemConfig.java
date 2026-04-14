package com.lego.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName: SystemConfig
 * Package: com.lego.pojo
 * Description:
 *
 * @Author michael.zhu
 * @Create 3/30/2026 10:04 AM
 * @Version 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SystemConfig {

    // 前端管理员密码
    private String adminPassword;

    // 前端最大server数量
    private Integer maxServerCount;

    // 前端模版最大table数量
    private Integer maxTableCount;

    // 前端模版table最大字段数量
    private Integer maxTableFieldCount;

    // 数据采集线程数量
    private Integer dataCollectionThreads;

    // 数据保存线程数量
    private Integer dataSaveThreads;

    // 数据保存间隔时间（秒）
    private Integer dataSaveInterval;

    // 数据保存批量写入大小
    private Integer dataSaveBatchSize;

    // 日志级别
    private Integer logLevel;
}
