package com.lego.pojo.template;

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
public class Module<T> {

    private boolean enable;
    private List<T> tableList;

}
