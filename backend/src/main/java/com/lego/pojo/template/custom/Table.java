package com.lego.pojo.template.custom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ClassName: Table
 * Package: lego.pojo
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 10:22
 * @Version 1.0
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Table {

    private String name;
    private Integer sampleInterval;
    private NodeGroupType nodeGroupType;
    private List<Node> nodeList;

}
