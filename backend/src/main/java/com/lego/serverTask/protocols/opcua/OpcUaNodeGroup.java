package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.custom.Node;
import com.lego.pojo.template.custom.NodeType;
import com.lego.pojo.template.custom.Table;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * ClassName: OpcUaNodeGroup
 * Package: com.lego.serverTask.protocols.opcua
 * Description:
 *
 * @Author michael.zhu
 * @Create 4/15/2026 3:45 PM
 * @Version 1.0
 */
public class OpcUaNodeGroup {

    // server name
    @Getter
    private String serverName;

    // table name
    @Getter
    private String name;

    // node type: scalar, array
    @Getter
    private NodeType nodeType;

    // node list
    @Getter
    private List<OpcUaNode> nodeList;




    public OpcUaNodeGroup(String serverName, Table table) {

        this.serverName = serverName;
        this.name = table.getName();
        this.nodeType = table.getNodeType();
        this.nodeList = new ArrayList<>();

        // 遍历 Table 中的 Node 列表，创建对应的 OpcUaNode 对象
        if (table.getNodeList() != null && !table.getNodeList().isEmpty()) {
            for (Node node : table.getNodeList()) {
                OpcUaNode opcUaNode = new OpcUaNode(
                        node.getName(),      // 节点名称
                        node.getNodeId(),    // 节点 ID 字符串
                        node.getDataType()   // 数据类型
                );
                this.nodeList.add(opcUaNode);
            }
        }
    }

}
