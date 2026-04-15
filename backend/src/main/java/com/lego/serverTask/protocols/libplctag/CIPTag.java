package com.lego.serverTask.protocols.libplctag;

import com.lego.util.DBUtil;
import io.github.libplctag.Tag;
import lombok.Getter;
//import util.InfluxDBUtil;

/**
 * ClassName: CIPTag
 * Package: commdrivers
 * Description:
 * Author Michael Zhu
 * Create 2025/7/25 11:42
 * Version 1.0
 * @author michael
 */


public class CIPTag {

    //tag group instance
    private Tag tag;

    //index of tag in the tag group
    private int index;

    // tag name
    @Getter
    private String tagName;

    //tag type in studio5000, int, dint, or real...
    private TagType tagType;

    private Object val;

    private Object lastVal = null;

    private boolean tagChanged;

    public CIPTag() {
    }

    public CIPTag(String serverName, Tag tag, int index, String tagName, TagType tagType) {
        this.tag = tag;
        this.index = index;
        this.tagName = tagName;
        this.tagType = tagType;
        DBUtil.writeLogToDB(serverName,  "tag created : " + this.tagName);
    }

    public void updateTag() {
        switch (tagType) {
            case INT:
                val = tag.getInt16(index * 2);
                break;
            case DINT:
                val = tag.getInt32(index * 4);
                break;
            case REAL:
                val = tag.getFloat32(index * 4);
                break;
            case BOOL:
                val = tag.getBit(index);
                break;
        }
    }

    public boolean isTagChanged() {
        tagChanged = !val.equals(lastVal);
        lastVal = val;
        return tagChanged;
    }

    public double getValAsDouble() {
        if (val == null) {
            return 0.0; // 或抛异常，根据需求
        }

        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        } else if (val instanceof Boolean) {
            return (Boolean) val ? 1.0 : 0.0;
        } else {
            // 如果是字符串或其他类型，可尝试 parse，或返回默认值
            try {
                return Double.parseDouble(val.toString());
            } catch (NumberFormatException e) {
                return 0.0; // 或 throw new RuntimeException("Cannot convert to double: " + val);
            }
        }
    }

}


