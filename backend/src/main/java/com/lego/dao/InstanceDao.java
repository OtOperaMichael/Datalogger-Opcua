package com.lego.dao;

import java.util.List;
import java.util.Map;

/**
 * ClassName: InstanceDao
 * Package: lego.dao
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:42
 * @Version 1.0
 */
public interface InstanceDao {
    List<Map<String, Object>> queryFromDatabase(String serverName, String tableName, String startTime, String endTime);

    List<String> queryLogs(String lineId, int days);

    List<Map<String, Object>> queryAlarmByTopTimes(String serverName, String tableName, String startTime, String endTime);

    List<Map<String, Object>> queryAlarmByTopDuration(String serverName, String tableName, String startTime, String endTime);
}
