package com.lego.pojo.template.custom;

import com.lego.pojo.template.BaseModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ClassName: Module
 * Package: com.lego.pojo.template.custom
 * Description:
 *
 * @Author michael.zhu
 * @Create 4/15/2026 2:43 PM
 * @Version 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomModule implements BaseModule<Table> {

    private boolean enable;
    private List<Table> tableList;

}
