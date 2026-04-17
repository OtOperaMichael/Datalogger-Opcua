package com.lego.pojo;

import com.lego.pojo.template.ModuleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName: QueryLoad
 * Package: lego.pojo
 * Description:
 *
 * @Author michael.zhu
 * @Create 2/27/2026 11:15 AM
 * @Version 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class QueryLoad {
    private String serverName;
    private ModuleType moduleType;
    private String tableName;
    private String startTime;
    private String endTime;
}
