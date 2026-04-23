package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.custom.DataType;
import lombok.Getter;
import lombok.Setter;

/**
 * ClassName: CommOpcUaNode
 * Package: com.lego.serverTask.protocols.opcua
 * Description: Communication module OPC UA node for monitoring communication-related nodes
 *
 * @Author michael.zhu
 * @Create 4/21/2026 9:29 AM
 * @Version 1.0
 */
public class CommOpcUaNode extends OpcUaNode {

    @Getter
    @Setter
    private boolean newRecord;

    public CommOpcUaNode(String serverName, String name, String nodeIdStr, DataType dataType) {
        super(serverName, name, nodeIdStr, dataType);
        this.newRecord = false;
    }

    /**
     * 更新节点值
     * 自动保存旧值，用于变化检测
     * 只有当值发生变化时才标记为新记录
     *
     * @param value 新值
     */
    @Override
    public void updateValue(Object value) {
        Object oldValue = this.getNewValue();
        
        // 调用父类方法更新值（会自动将 newValue 移到 oldValue）
        super.updateValue(value);
        
        // 判断值是否发生变化
        if (oldValue != null) {
            // 有旧值，比较是否变化
            if (!oldValue.equals(value)) {
                this.newRecord = true;
            } else {
                this.newRecord = false;
            }
        } else {
            // 第一次接收数据，也标记为新记录
            this.newRecord = true;
        }
    }

    /**
     * 重置节点状态
     * 在数据入队后调用
     */
    public void reset() {
        this.newRecord = false;
    }

}

