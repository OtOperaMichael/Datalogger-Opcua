package com.lego.serverTask.protocols.libplctag;

import com.lego.util.DBUtil;
import io.github.libplctag.Tag;
import com.lego.common.TagType;
import lombok.Getter;

import java.util.ArrayList;

/**
 * ClassName: CIPTagGroup
 * Package: commdrivers
 * Description:
 * Author Michael Zhu
 * Create 2025/7/29 10:48
 * Version 1.0
 *
 * @author michael
 */


public class CIPTagGroup {

    //table name
    @Getter
    private String name;

    //tag address
    private String tagAddr;

    //tag type
    @Getter
    private TagType tagType;

    @Getter
    private ArrayList<String> tagNames;

    private Tag tagGroup;

    @Getter
    private ArrayList<CIPTag> cipTagArrayList;

    private String serverName;

    //read tag timeout
    private final int timeout = 500;

    private boolean tagGroupChanged = false;
    private ArrayList<Boolean> tagChangedList = new ArrayList<Boolean>();

    public CIPTagGroup() {
    }

    public CIPTagGroup(String serverName,String name, String tagAddr, TagType tagType, ArrayList<String> tagNames) {
        this.serverName = serverName;
        this.name = name;
        this.tagAddr = tagAddr;
        this.tagType = tagType;
        this.tagNames = tagNames;
        this.cipTagArrayList = new ArrayList<>();

        try {
            //tag group create
            tagGroup = new Tag(tagAddr, timeout);
            //temp variables, only used in method
            int tagGroupStatus = tagGroup.getStatus();
            if (tagGroupStatus != Tag.PLCTAG_STATUS_OK) {
                DBUtil.writeLogToDB(serverName, "Unable to create the tag group: " + this.tagAddr + ", got error " + Tag.decodeError(tagGroupStatus) + "!");
                System.out.println("Unable to create the tag group: " + this.tagAddr + ", got error " + Tag.decodeError(tagGroupStatus) + "!");
            } else {
                DBUtil.writeLogToDB(serverName, "tag group created: " + this.name);
                System.out.println("tag group created: " + this.name);
            }
        } catch (Exception e) {
            DBUtil.writeLogToDB(serverName, e.getMessage());
            e.printStackTrace();
        }

        //tags create
        for (int i = 0; i < tagNames.size(); i++) {
            cipTagArrayList.add(new CIPTag(serverName,tagGroup, i, tagNames.get(i), tagType));
        }
    }

    /**
     * 检查组内是否有任意一个 Tag 的值发生了变化。
     * 调用每个 tag.isTagChanged()，会同时更新它们的 lastVal。
     *
     * @return true 如果至少有一个 Tag 发生了变化
     */
//    public boolean isTagGroupChanged() {
//        tagGroupChanged = false;
//        updateTagGroup();
//        for (CIPTag tag : cipTagArrayList) {
//            tagGroupChanged = tagGroupChanged || tag.isTagChanged();
//        }
//        return tagGroupChanged;
//    }

    public boolean isTagGroupChanged() {
        tagGroupChanged = false;

        updateTagGroup();
        for (Boolean tagChanged : tagChangedList) {
            tagGroupChanged = tagGroupChanged || tagChanged;
        }

        return tagGroupChanged;
    }

    /**
     * 更新组内所有 Tag 的值。
     * 调用每个 tag.updateTag()，会同时更新它们的 lastVal。
     */
    private void updateTagGroup() {
        tagChangedList.clear();
        tagGroup.read(timeout);
        for (CIPTag cipTag : cipTagArrayList) {
            cipTag.updateTag();
            tagChangedList.add(cipTag.isTagChanged());
        }
    }

    /**
     * 销毁组内所有 Tag。
     */
    public void destoryTagGroup() {
        DBUtil.writeLogToDB(this.serverName, "tag group destroyed: " + this.name);
        tagGroup.close();
    }

}
