package com.lego.dao.Impl;

import com.lego.dao.InstanceDao;
import com.lego.util.DBUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ClassName: InstanceDaoImpl
 * Package: lego.dao.Impl
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:43
 * @Version 1.0
 */
public class InstanceDaoImpl implements InstanceDao {
    @Override
    public List<Map<String, Object>> queryFromDatabase(String serverName, String tableName, String startTime, String endTime) {
        List<Map<String, Object>> results = new ArrayList<>();
        results = DBUtil.queryData(serverName, tableName, startTime, endTime);

        return results;
    }



    @Override
    public List<String> queryLogs(String schemaName, int days) {
        List<String> results = new ArrayList<>();
        results = DBUtil.queryLogs(schemaName, days);
        return results;
    }
}
