package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.ModuleType;
import com.lego.pojo.template.custom.Node;
import com.lego.pojo.template.custom.NodeGroupType;
import com.lego.pojo.template.custom.Table;
import com.lego.util.LogUtil;
import lombok.Getter;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpcUaNodeGroup {

    // server name
    @Getter
    private String serverName;

    // module type: custom, alarm, communication
    @Getter
    private ModuleType moduleType;

    // table name
    @Getter
    private String name;

    // sample interval
    @Getter
    private Integer sampleInterval;

    // node type: scalar, array
    @Getter
    private NodeGroupType nodeType;

    // node list
    @Getter
    private List<OpcUaNode> nodeList;

    @Getter
    private String fullTableName;

    // nodeId 到 OpcUaNode 的映射，用于快速查找
    private Map<NodeId, OpcUaNode> nodeIdToNodeMap;


    public OpcUaNodeGroup(String serverName, ModuleType moduleType, Table table) {

        this.serverName = serverName;
        this.moduleType = moduleType;
        this.name = table.getName();
        this.sampleInterval = table.getSampleInterval();
        this.nodeType = table.getNodeGroupType();
        this.nodeList = new ArrayList<>();
        this.nodeIdToNodeMap = new HashMap<>();

        // 遍历 Table 中的 Node 列表，创建对应的 OpcUaNode 对象
        if (table.getNodeList() != null && !table.getNodeList().isEmpty()) {
            int successCount = 0;
            int failCount = 0;
            
            for (Node node : table.getNodeList()) {
                OpcUaNode opcUaNode = new OpcUaNode(
                        serverName,
                        node.getName(),      // 节点名称
                        node.getNodeId(),    // 节点 ID 字符串
                        node.getDataType()   // 数据类型
                );
                
                // 如果 NodeId 解析失败，跳过该节点
                if (opcUaNode.getNodeId() == null) {
                    LogUtil.logWarning(true,"serverName", "Skipping node '{}' due to invalid NodeId: {}",
                        node.getName(), node.getNodeId());
                    failCount++;
                    continue;
                }
                
                this.nodeList.add(opcUaNode);
                // 建立 NodeId 到节点的映射
                this.nodeIdToNodeMap.put(opcUaNode.getNodeId(), opcUaNode);
                successCount++;
            }
            
            if (failCount > 0) {
                LogUtil.logWarning(true,"serverName", "NodeGroup '{}': {} nodes created successfully, {} nodes skipped due to invalid NodeId",
                    this.name, successCount, failCount);
            }
        }
    }

    /**
     * 根据 NodeId 快速查找节点
     *
     * @param nodeId OPC UA NodeId
     * @return 匹配的 OpcUaNode，未找到返回 null
     */
    public OpcUaNode getNodeByNodeId(NodeId nodeId) {
        return nodeIdToNodeMap.get(nodeId);
    }

}
