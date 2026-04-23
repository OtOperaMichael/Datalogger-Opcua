package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.custom.DataType;

/**
 * ClassName: CustomNode
 * Package: com.lego.serverTask.protocols.opcua
 * Description:
 *
 * @Author michael.zhu
 * @Create 4/21/2026 9:29 AM
 * @Version 1.0
 */
public class CommOpcUaNode extends OpcUaNode {
    public CommOpcUaNode(String serverName, String name, String nodeIdStr, DataType dataType) {
        super(serverName, name, nodeIdStr, dataType);
    }
}

