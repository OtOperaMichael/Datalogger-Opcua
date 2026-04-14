package com.lego.serverTask;

import com.lego.pojo.Table;
import com.lego.common.TagType;
import com.lego.pojo.Server;
import com.lego.pojo.Template;
import com.lego.serverTask.protocols.libplctag.CIPTagGroup;
import com.lego.util.DBUtil;
import com.lego.util.TemplateUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: PlcMonitoringTask
 * Package: lego.serverTask
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 19:28
 * @Version 1.0
 */
public class Datalogger implements Runnable {

    private final String serverName;
    private final String templateName;
    private final String hostIP;
    private final Template template;

    private ArrayList<CIPTagGroup> cipTagGroups = new ArrayList<>();

    private int minute;
    private int second;
    private long startTime;
    private long stopTime;
    private long costTime;
    private boolean isTagGroupChanged;

    // 引用全局队列
    private final GlobalDataQueue globalDataQueue;

    //构造器，初始化tagGroups
    public Datalogger(Server server) {
        serverName = server.getName();
        templateName = server.getTemplateName();
        hostIP = server.getHostIP();
        template = TemplateUtil.resolveTemplate(server);

        // 获取全局队列实例
        globalDataQueue = GlobalDataQueue.getInstance();

        //如果需要的template被配置了
        if (template != null) {

            //create database 为每一个server instance, e.g. P86B
            try {
                DBUtil.createSchemaIfNotExists(serverName);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            //遍历template中的所有table
            for (int index = 0; index < template.getTableList().size(); index++) {

                Table table = template.getTableList().get(index);
                //从前端传来的tagGroup有可能为空，为空则跳过
                if (!table.getName().isEmpty()) {
                    CIPTagGroup tagGroup = createCipTagGroup(serverName, table);
                    cipTagGroups.add(tagGroup);
                    //create hyper table for each tagGroup
                    DBUtil.createHyperTable(serverName, tagGroup);

                } else {
                    break;
                }
            }

        }
    }


    private CIPTagGroup createCipTagGroup(String serverName, Table table) {

        List<String> tagNameList = table.getTagNameList();

        String tagAddr = "";
        TagType tagType;
        ArrayList<String> tagNames = new ArrayList<>();

        for (int i = 0; i < tagNameList.size(); i++) {
            //从前端输入的tagname有可能为空，为空则为无效
            if ((tagNameList.get(i) != null) && !tagNameList.get(i).isEmpty()) {
                tagNames.add(table.getTagNameList().get(i));
            } else {
                break;
            }
        }

        StringBuilder str = new StringBuilder();
        str.append("protocol=ab-eip&gateway=");
        str.append(hostIP);
        str.append("&path=");
        str.append(template.getHostCpuSlot());
        str.append("&plc=ControlLogix&elem_count=");
        str.append(tagNames.size());
        str.append("&name=");
        str.append(table.getTagAddr());
        tagAddr = str.toString();
        tagType = table.getTagType();

        return new CIPTagGroup(serverName, table.getName(),  tagAddr, tagType, tagNames);
    }

    /**
     * 循环执行的任务
     */
    @Override
    public void run() {

        minute = LocalDateTime.now().getMinute();
        second = LocalDateTime.now().getSecond();

        for (CIPTagGroup cipTagGroup : cipTagGroups) {

            //log reading time, if time is too long
            //or every 20 minutes, as heart beat for each instance
            startTime = System.currentTimeMillis();
            isTagGroupChanged = cipTagGroup.isTagGroupChanged();
            stopTime = System.currentTimeMillis();
            costTime = stopTime - startTime;
            if ((costTime > 50) || (minute % 20 == 0 && second == 0)) {
                DBUtil.writeLogToDB(serverName, serverName + " " + " read tags taking time: " + (stopTime - startTime) + "ms");
            }

            if (isTagGroupChanged) {
                // 将数据写入全局队列，由消费者线程池统一处理， 使用InfluxDB3数据库
                globalDataQueue.enqueueCustomData(serverName, cipTagGroup);
            }

        }
    }

    /**
     * 销毁所有的 tag group
     */
    public void destroyAllTags() {
        System.out.println(serverName + ": Destroying all tag groups for server");
        DBUtil.writeLogToDB(serverName, "Destroying all tag groups for server: ");

        for (CIPTagGroup tagGroup : cipTagGroups) {
            try {
                tagGroup.destoryTagGroup();
            } catch (Exception e) {
                System.err.println("Error destroying tag group: " + e.getMessage());
                DBUtil.writeLogToDB(serverName, "Error destroying tag group: " + e.getMessage());
            }
        }
        cipTagGroups.clear();
        System.out.println(serverName + ": All tag groups destroyed for server");
        DBUtil.writeLogToDB(serverName, "All tag groups destroyed for server");
    }

}
