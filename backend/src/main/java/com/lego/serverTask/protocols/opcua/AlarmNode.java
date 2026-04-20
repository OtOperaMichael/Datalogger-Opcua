package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.alarm.TriggerType;
import com.lego.pojo.template.custom.DataType;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ClassName: alarmNode
 * Package: com.lego.serverTask.protocols.opcua
 * Description:
 *
 * @Author michael.zhu
 * @Create 4/20/2026 1:03 PM
 * @Version 1.0
 */
public class AlarmNode extends OpcUaNode {

    @Getter
    private String deviceName;

    @Getter
    private TriggerType triggerType;

    @Getter
    private String Description;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;

    private int state;

    public AlarmNode(String serverName, String name, String nodeIdStr, DataType dataType, String deviceName, TriggerType triggerType, String description) {

        super(serverName, name, nodeIdStr, dataType);
        this.deviceName = deviceName;
        this.triggerType = triggerType;
        this.Description = description;

    }



}
