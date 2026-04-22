package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.ModuleType;
import com.lego.pojo.template.NodeGroupType;
import com.lego.pojo.template.Table;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpcUaNodeGroup {

    // server name
    @Getter
    @Setter
    private String serverName;

    // module type: custom, alarm, communication
    @Getter
    private ModuleType moduleType;

    // table name
    @Getter
    @Setter
    private String name;

    // sample interval
    @Getter
    @Setter
    private Integer sampleInterval;

    // node type: scalar, array
    @Getter
    @Setter
    private NodeGroupType nodeType;

    // node list
    @Getter
    @Setter
    private List<OpcUaNode> nodeList;

    @Getter
    @Setter
    private String fullTableName;

    // nodeId 到 OpcUaNode 的映射，用于快速查找
    @Getter
    private Map<NodeId, OpcUaNode> nodeIdToNodeMap;

    public OpcUaNodeGroup() {
    }

    public OpcUaNodeGroup(String serverName, ModuleType moduleType, Table<?> table) {
        this.serverName = serverName;
        this.moduleType = moduleType;
        this.name = table.getName();
        this.sampleInterval = table.getSampleInterval();
        this.nodeType = table.getNodeGroupType();
        this.nodeList = new ArrayList<>();
        this.nodeIdToNodeMap = new HashMap<>();
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
