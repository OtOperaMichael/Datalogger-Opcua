package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.ModuleType;
import com.lego.pojo.template.Table;

import com.lego.pojo.template.alarm.AlarmNode;
import com.lego.pojo.template.custom.DataType;
import com.lego.util.LogUtil;

public class AlarmOpcUaNodeGroup extends OpcUaNodeGroup {

    public AlarmOpcUaNodeGroup(String serverName, ModuleType moduleType, Table<AlarmNode> table) {
        super(serverName, moduleType, table);
        this.setFullTableName(moduleType.toString().toLowerCase() + "_" + "history");

        if (table.getNodeList() != null && !table.getNodeList().isEmpty()) {
            int successCount = 0;
            int failCount = 0;

            for (AlarmNode node : table.getNodeList()) {
                AlarmOpcUaNode alarmOpcUaNode = new AlarmOpcUaNode(
                        serverName,
                        node.getName(),
                        node.getNodeId(),
                        DataType.BOOL,
                        table.getName(),
                        node.getTriggerType(),
                        node.getDescription()
                );

                if (alarmOpcUaNode.getNodeId() == null) {
                    LogUtil.logWarning(true, serverName, "Skipping alarm node '{}' due to invalid NodeId: {}",
                            node.getName(), node.getNodeId());
                    failCount++;
                    continue;
                }

                this.getNodeList().add(alarmOpcUaNode);
                this.getNodeIdToNodeMap().put(alarmOpcUaNode.getNodeId(), alarmOpcUaNode);
                successCount++;
            }

            if (failCount > 0) {
                LogUtil.logWarning(true, serverName, "NodeGroup '{}_{}': {} nodes created successfully, {} nodes skipped due to invalid NodeId",
                        moduleType.toString().toLowerCase(), this.getName(), successCount, failCount);
            }
        }
    }

}
