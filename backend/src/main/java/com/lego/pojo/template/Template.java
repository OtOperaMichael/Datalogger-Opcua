package com.lego.pojo.template;

import com.lego.pojo.template.custom.CustomModule;
import com.lego.pojo.template.custom.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private Integer sampleInterval;
    private String port;
    private String postfix;
    private CustomModule custom;
    private CustomModule alarm;
    private CustomModule communication;

}
