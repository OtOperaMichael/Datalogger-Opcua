package com.lego.service.Impl;

import com.lego.dao.Impl.InstanceDaoImpl;
import com.lego.dao.InstanceDao;
import com.lego.pojo.QueryLoad;
import com.lego.service.InstanceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ClassName: InstanceServiceImpl
 * Package: lego.service.Impl
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:57
 * @Version 1.0
 */
public class InstanceServiceImpl implements InstanceService {
    private InstanceDao instanceDao = new InstanceDaoImpl();

    @Override
    public List<Map<String, Object>> query(QueryLoad queryLoad) {
        List<Map<String, Object>> results = new ArrayList<>();
        //tableName == moduleType + "_" + tableName, e.g. custom_group1, to avoid duplicate name across modules
        String tableName = queryLoad.getModuleType().toString().toLowerCase() + "_" + queryLoad.getTableName();
        results = instanceDao.queryFromDatabase(queryLoad.getServerName(), tableName, queryLoad.getStartTime(), queryLoad.getEndTime());
        return results;
    }

    @Override
    public List<String> queryLogs(String lineId, int days) {
        List<String> results = new ArrayList<>();

        results = instanceDao.queryLogs(lineId, days);

        return results;
    }


}
