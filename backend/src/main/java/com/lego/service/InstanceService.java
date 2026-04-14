package com.lego.service;

import com.lego.pojo.QueryLoad;

import java.util.List;
import java.util.Map;

/**
 * ClassName: InstanceService
 * Package: lego.service
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:48
 * @Version 1.0
 */
public interface InstanceService {
    List<Map<String, Object>> query(QueryLoad queryLoad);

    List<String> queryLogs(String lineId, int days);
}
