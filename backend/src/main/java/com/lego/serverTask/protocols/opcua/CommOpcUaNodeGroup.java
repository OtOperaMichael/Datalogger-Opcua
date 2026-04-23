package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.ModuleType;
import com.lego.pojo.template.Table;
import com.lego.pojo.template.communication.CommNode;
import com.lego.pojo.template.custom.CustomNode;
import com.lego.pojo.template.custom.DataType;
import com.lego.util.LogUtil;

public class CommOpcUaNodeGroup extends OpcUaNodeGroup {

    public CommOpcUaNodeGroup(String serverName, ModuleType moduleType, Table<CommNode> table) {
        super(serverName, moduleType, table);
        this.setFullTableName(moduleType.toString().toLowerCase() + "_" + "history");

        if (table.getNodeList() != null && !table.getNodeList().isEmpty()) {
            int successCount = 0;
            int failCount = 0;

            for (CommNode node : table.getNodeList()) {
                CommOpcUaNode customOpcUaNode = new CommOpcUaNode(
                        serverName,
                        node.getName(),
                        node.getNodeId(),
                        DataType.STRING
                );

                if (customOpcUaNode.getNodeId() == null) {
                    LogUtil.logWarning(true, serverName, "Skipping node '{}' due to invalid NodeId: {}",
                            node.getName(), node.getNodeId());
                    failCount++;
                    continue;
                }

                this.getNodeList().add(customOpcUaNode);
                this.getNodeIdToNodeMap().put(customOpcUaNode.getNodeId(), customOpcUaNode);
                successCount++;
            }

            if (failCount > 0) {
                LogUtil.logWarning(true, serverName, "NodeGroup '{}': {} nodes created successfully, {} nodes skipped due to invalid NodeId",
                        this.getName(), successCount, failCount);
            }
        }
    }

}
