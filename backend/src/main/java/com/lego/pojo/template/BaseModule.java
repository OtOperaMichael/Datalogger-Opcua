package com.lego.pojo.template;


import java.util.List;

/**
 * ClassName: BaseModule
 * Package: com.lego.pojo.template
 * Description: 所有模块的通用接口
 *
 * @Author michael.zhu
 * @Create 4/17/2026
 * @Version 1.0
 */
public interface BaseModule<T> {

    /**
     * 获取模块是否启用
     * @return true if enabled
     */
    boolean isEnable();

    /**
     * 获取模块中的表格列表
     * @return list of tables
     */
    List<T> getTablelist();

}