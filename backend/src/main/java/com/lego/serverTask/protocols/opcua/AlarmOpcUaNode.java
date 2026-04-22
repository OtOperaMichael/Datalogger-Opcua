package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.alarm.TriggerType;
import com.lego.pojo.template.custom.DataType;
import lombok.Getter;
import lombok.Setter;

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
public class AlarmOpcUaNode extends OpcUaNode {

    @Getter
    private String deviceName;

    @Getter
    private TriggerType triggerType;

    @Getter
    private String Description;

    @Getter
    private LocalDateTime startTime;

    @Getter
    private LocalDateTime endTime;

    @Getter
    private Integer duration;

    private int state;

    @Getter
    @Setter
    private boolean newRecord;

    public AlarmOpcUaNode(String serverName, String name, String nodeIdStr, DataType dataType, String deviceName, TriggerType triggerType, String description) {

        super(serverName, name, nodeIdStr, dataType);
        this.deviceName = deviceName;
        this.triggerType = triggerType;
        this.Description = description;
        this.state = 0;
        this.newRecord = false;

    }

    /**
     * 更新节点值
     * 自动保存旧值，用于变化检测
     *
     * @param value 新值
     */
    @Override
    public void updateValue(Object value) {
        super.updateValue(value);

        /**
         * 1. 如果trigger_type == Rising, 则当getNewValue()=1时 记录startTime, state=10
         * 当state=10, getNewValue()=0时, 记录endTime, 并计算出duration, newRecord=true, state=0
         *
         * 2. 如果trigger_type == Falling, 则当getNewValue()=0时 记录startTime, state=10
         * 当state=10, getNewValue()=1时, 记录endTime, 并计算出duration, newRecord=true, state=0
         */

        if (this.getNewValue() == null) {
            return;
        }

        boolean currentValue = toBoolean(this.getNewValue());

        if (triggerType == TriggerType.RISING) {
            if (currentValue && state == 0) {
                startTime = LocalDateTime.now();
                state = 10;
                newRecord = false;
            } else if (!currentValue && state == 10) {
                endTime = LocalDateTime.now();
                if (startTime != null) {
                    duration = (int) java.time.Duration.between(startTime, endTime).getSeconds();
                }
                newRecord = true;
                state = 0;
            }
        } else if (triggerType == TriggerType.FALLING) {
            if (!currentValue && state == 0) {
                startTime = LocalDateTime.now();
                state = 10;
                newRecord = false;
            } else if (currentValue && state == 10) {
                endTime = LocalDateTime.now();
                if (startTime != null) {
                    duration = (int) java.time.Duration.between(startTime, endTime).getSeconds();
                }
                newRecord = true;
                state = 0;
            }
        }
    }

    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else if (value instanceof String) {
            String str = ((String) value).trim().toLowerCase();
            return "true".equals(str) || "1".equals(str);
        }
        return false;
    }

    public void reset() {
        this.startTime = null;
        this.endTime = null;
        this.duration = null;
        this.state = 0;
        this.newRecord = false;
    }

}




