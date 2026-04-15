package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.custom.DataType;
import lombok.Getter;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * ClassName: Tag
 * Package: lego.serverTask.protocols.opcua
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/16 10:25
 * @Version 1.0
 */


public class OpcUaNode {

    @Getter
    private String name;

    @Getter
    private NodeId nodeId;

    @Getter
    private DataType dataType;

    @Getter
    private Object newValue;

    @Getter
    private Object oldValue;

    public OpcUaNode(String name, String nodeIdStr, DataType dataType) {
        this.name = name;
        this.nodeId = parseNodeId(nodeIdStr);
        this.dataType = dataType;
        this.newValue = null;
        this.oldValue = null;
    }

    private NodeId parseNodeId(String nodeIdStr) {
        if (nodeIdStr == null || nodeIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("NodeId string cannot be null or empty");
        }
        
        try {
            return NodeId.parse(nodeIdStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid NodeId format: " + nodeIdStr + 
                ". Expected format: ns=<namespace>;s=<identifier> or ns=<namespace>;i=<numeric>", e);
        }
    }

    public void updateValue(Object value) {
        this.oldValue = this.newValue;
        this.newValue = value;
    }


}
