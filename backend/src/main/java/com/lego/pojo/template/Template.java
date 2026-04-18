package com.lego.pojo.template;

import com.lego.pojo.template.alarm.AlarmModule;
import com.lego.pojo.template.communication.CommModule;
import com.lego.pojo.template.custom.CustomModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ClassName: Template
 * Package: lego.pojo
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:18
 * @Version 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Template {

    private String id;
    private String name;
    private String port;
    private String postfix;
    private CustomModule custom;
    private AlarmModule alarm;
    private CommModule communication;

    /**
     * 根据模块类型获取对应的模块
     *
     * @param moduleType 模块类型
     * @return 对应的模块，如果类型无效则返回 null
     */
    public BaseModule<?> getModuleByType(ModuleType moduleType) {
        if (moduleType == null) {
            return null;
        }

        return switch (moduleType) {
            case CUSTOM -> custom;
            case ALARM -> alarm;
            case COMMUNICATION -> communication;
        };
    }
}
