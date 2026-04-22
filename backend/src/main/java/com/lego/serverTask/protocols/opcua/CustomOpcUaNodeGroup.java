package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.ModuleType;
import com.lego.pojo.template.Table;
import com.lego.pojo.template.custom.CustomNode;
import com.lego.util.LogUtil;

public class CustomOpcUaNodeGroup extends OpcUaNodeGroup {

    public CustomOpcUaNodeGroup(String serverName, ModuleType moduleType, Table<CustomNode> table) {
        super(serverName, moduleType, table);
        this.setFullTableName(moduleType.toString().toLowerCase()+"_"+table.getName());

        if (table.getNodeList() != null && !table.getNodeList().isEmpty()) {
            int successCount = 0;
            int failCount = 0;

            for (CustomNode node : table.getNodeList()) {
                CustomOpcUaNode customOpcUaNode = new CustomOpcUaNode(
                        serverName,
                        node.getName(),
                        node.getNodeId(),
                        node.getDataType()
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
