package com.lego.pojo;

import com.lego.common.TagType;
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
    private String tagAddr;
    private TagType tagType;

    private List<String> tagNameList;
}
