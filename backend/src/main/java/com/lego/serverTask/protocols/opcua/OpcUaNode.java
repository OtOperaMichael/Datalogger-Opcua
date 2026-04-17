package com.lego.serverTask.protocols.opcua;

import com.lego.pojo.template.custom.DataType;
import com.lego.util.LogUtil;
import lombok.Getter;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

/**
 * ClassName: Tag
 * Package: lego.serverTask.protocols.opcua
 * Description: OPC UA 节点封装类
 *
 * @Author michael.zhu
 * @Create 2026/2/16 10:25
 * @Version 1.0
 */
public class OpcUaNode {

    // server name
    @Getter
    private String serverName;

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

    public OpcUaNode(String serverName, String name, String nodeIdStr, DataType dataType) {
       this.serverName = serverName;
        this.name = name;
        this.nodeId = parseNodeId(nodeIdStr);
        this.dataType = dataType;
        this.newValue = null;
        this.oldValue = null;
    }

    /**
     * 解析 NodeId 字符串
     * 如果解析失败，记录日志并返回 null，不会中断应用
     *
     * @param nodeIdStr NodeId 字符串
     * @return 解析后的 NodeId，失败则返回 null
     */
    private NodeId parseNodeId(String nodeIdStr) {
        if (nodeIdStr == null || nodeIdStr.trim().isEmpty()) {
            LogUtil.logWarning(true,serverName, "NodeId string is null or empty");
            return null;
        }
        
        try {
            return NodeId.parse(nodeIdStr);
        } catch (Exception e) {
            LogUtil.logError(true,serverName, "Invalid NodeId format: {}. Expected format: ns=<namespace>;s=<identifier> or ns=<namespace>;i=<numeric>", nodeIdStr);
            return null;
        }
    }

    /**
     * 更新节点值
     * 自动保存旧值，用于变化检测
     *
     * @param value 新值
     */
    public void updateValue(Object value) {
        this.oldValue = this.newValue;
        this.newValue = value;
    }


}
