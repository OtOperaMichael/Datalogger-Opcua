package com.lego.serverTask;

import com.lego.pojo.template.ModuleType;
import com.lego.pojo.template.NodeGroupType;
import com.lego.pojo.Server;
import com.lego.pojo.template.Table;
import com.lego.pojo.template.Template;
import com.lego.serverTask.protocols.opcua.AlarmOpcUaNodeGroup;
import com.lego.serverTask.protocols.opcua.OpcUaNode;
import com.lego.serverTask.protocols.opcua.CustomOpcUaNodeGroup;
import com.lego.serverTask.protocols.opcua.OpcUaNodeGroup;
import com.lego.util.DBUtil;
import com.lego.util.LogUtil;
import com.lego.util.TemplateUtil;
import lombok.Getter;
import org.eclipse.milo.opcua.sdk.client.DiscoveryClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName: PlcMonitoringTask
 * Package: lego.serverTask
 * Description:
 *
 * @Author michael.zhu
 * @Create 2026/2/10 19:28
 * @Version 1.0
 */
public class OpcuaDatalogger {

    private final String serverName;
    private final String templateName;
    private final String serverUrl;
    private final Template template;
    private final boolean customModuleIsEnabled;
    private final boolean alarmModuleIsEnabled;
    private final boolean communicationModuleIsEnabled;

    private final ArrayList<CustomOpcUaNodeGroup> customModuleNodeGroupList = new ArrayList<>();
    private final ArrayList<AlarmOpcUaNodeGroup> alarmModuleNodeGroupList = new ArrayList<>();
    private final ArrayList<CustomOpcUaNodeGroup> commModuleNodeGroupList = new ArrayList<>();

    // 引用全局队列
    private final GlobalDataQueue globalDataQueue;

    // 存储每个 nodeGroup 对应的 subscription，用于分组管理
    private Map<String, OpcUaSubscription> subscriptionMap = new HashMap<>();

    // 保存 client 引用，用于关闭时断开连接
    private OpcUaClient client;

    // 标记任务是否正在运行
    @Getter
    private volatile boolean isRunning = false;

    //构造器，初始化tagGroups
    public OpcuaDatalogger(Server server) {
        serverName = server.getName();
        templateName = server.getTemplateName();
        template = TemplateUtil.resolveTemplate(server);

        // 获取全局队列实例
        globalDataQueue = GlobalDataQueue.getInstance();

        if (template == null) {
            LogUtil.logWarning(true, serverName, "Template not found for server: {}", serverName);
            customModuleIsEnabled = false;
            alarmModuleIsEnabled = false;
            communicationModuleIsEnabled = false;
            serverUrl = null;
            return;
        }

        customModuleIsEnabled = template.getCustom().isEnable();
        alarmModuleIsEnabled = template.getAlarm().isEnable();
        communicationModuleIsEnabled = template.getCommunication().isEnable();

        //create database 为每一个server instance, e.g. P86B
        if (customModuleIsEnabled || alarmModuleIsEnabled || communicationModuleIsEnabled) {
            try {
                DBUtil.createSchemaIfNotExists(serverName);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        String hostIp = server.getHostIP();
        String port = (template.getPort() != null) ? template.getPort() : "4840";
        String postfix = (template.getPostfix() != null) ? template.getPostfix() : "";
        serverUrl = "opc.tcp://" + hostIp + ":" + port + postfix;

        //Module1: custom, 遍历template中的所有table
        if (customModuleIsEnabled) {

            for (int index = 0; index < template.getCustom().getTableList().size(); index++) {

                Table table = template.getCustom().getTableList().get(index);
                //从前端传来的tagGroup有可能为空，为空则跳过
                if (!table.getName().isEmpty()) {
                    CustomOpcUaNodeGroup nodeGroup = new CustomOpcUaNodeGroup(serverName, ModuleType.CUSTOM, table);
                    customModuleNodeGroupList.add(nodeGroup);
                    //create hyper table for each tagGroup
                    DBUtil.createHyperTable(serverName, nodeGroup);

                } else {
                    break;
                }
            }

        }

        //Module2: alarm
        if (alarmModuleIsEnabled) {

            for (int index = 0; index < template.getAlarm().getTableList().size(); index++) {

                Table table = template.getAlarm().getTableList().get(index);
                //从前端传来的tagGroup有可能为空，为空则跳过
                if (!table.getName().isEmpty()) {
                    AlarmOpcUaNodeGroup nodeGroup = new AlarmOpcUaNodeGroup(serverName, ModuleType.ALARM, table);
                    alarmModuleNodeGroupList.add(nodeGroup);

                } else {
                    break;
                }
            }

            //create table, non-hyper, one single table for alarm module, called "alarm_history"
            DBUtil.createTable(serverName, alarmModuleNodeGroupList.get(0));

        }

        //Module3: communication


    }

    /**
     * 启动 OPC UA 连接和订阅
     */
    public void start() {

        // 检查是否已经在运行
        if (isRunning) {
            LogUtil.logWarning(true, serverName, "Server {} is already running, skipping start", serverName);
            return;
        }

        try {
            // 获取服务端点描述
            List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(serverUrl).get();
            EndpointDescription endpoint = endpoints.stream()
                    .filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getUri()))
                    .findFirst()
                    .orElse(null);

            if (endpoint == null) {
                LogUtil.logError(true, serverName, "No endpoint with SecurityPolicy.None found for server: {}", serverUrl);
                return;
            }

            String endpointUrl = endpoint.getEndpointUrl();
            LogUtil.logInfo(true, serverName, "Original endpoint URL from server: {}", endpointUrl);

            String fixedEndpointUrl = fixEndpointUrl(endpointUrl, serverUrl);
            LogUtil.logInfo(true, serverName, "Fixed endpoint URL: {}", fixedEndpointUrl);

            EndpointDescription fixedEndpoint = new EndpointDescription(
                    fixedEndpointUrl,
                    endpoint.getServer(),
                    endpoint.getServerCertificate(),
                    endpoint.getSecurityMode(),
                    endpoint.getSecurityPolicyUri(),
                    endpoint.getUserIdentityTokens(),
                    endpoint.getTransportProfileUri(),
                    endpoint.getSecurityLevel()
            );

            OpcUaClientConfig config = OpcUaClientConfig.builder().setEndpoint(fixedEndpoint).build();
            client = OpcUaClient.create(config);

            client.connect();
            LogUtil.logInfo(true, serverName, "Connected to OPC UA Server: {}", serverUrl);

            // 创建订阅
            if (customModuleIsEnabled) {
                for (CustomOpcUaNodeGroup nodeGroup : customModuleNodeGroupList) {
                    createSubscriptionForNodeGroup(client, nodeGroup);
                }
            }

            if (alarmModuleIsEnabled) {
                for (AlarmOpcUaNodeGroup nodeGroup : alarmModuleNodeGroupList) {
                    createSubscriptionForNodeGroup(client, nodeGroup);
                }
            }

            isRunning = true;
            LogUtil.logInfo(true, serverName, "Server {} started successfully", serverName);

        } catch (Exception e) {
            isRunning = false;
            // 启动失败，记录日志并抛出异常
            LogUtil.logError(true, serverName, "Failed to start server: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private String fixEndpointUrl(String endpointUrl, String originalUrl) {
        try {
            java.net.URI originalUri = new java.net.URI(originalUrl.replace("opc.tcp://", "http://"));
            java.net.URI endpointUri = new java.net.URI(endpointUrl.replace("opc.tcp://", "http://"));

            String originalHost = originalUri.getHost();
            String endpointHost = endpointUri.getHost();

            if (originalHost != null && endpointHost != null && !originalHost.equals(endpointHost)) {
                LogUtil.logInfo(true, serverName, "Replacing endpoint host '{}' with '{}'", endpointHost, originalHost);

                int port = endpointUri.getPort();
                String path = endpointUri.getPath();

                String fixedUrl = "opc.tcp://" + originalHost;
                if (port > 0) {
                    fixedUrl += ":" + port;
                }
                if (path != null && !path.isEmpty()) {
                    fixedUrl += path;
                }

                return fixedUrl;
            }
        } catch (Exception e) {
            LogUtil.logWarning(true, serverName, "Failed to parse URLs, using original endpoint: {}", e.getMessage());
        }

        return endpointUrl;
    }

    /**
     * 统一的资源清理方法
     * 包括：删除所有 subscription、断开 OPC UA 连接、清空本地缓存
     * 由 ServerTaskManager.stopMonitoring() 或 AppLifecycleListener.contextDestroyed() 调用
     */
    public void shutdown() {
        // 如果未运行，无需关闭
        if (!isRunning) {
            LogUtil.logWarning(true, serverName, "Server {} is not running, skipping shutdown", serverName);
            return;
        }

        LogUtil.logInfo(true, serverName, "Starting shutdown process for server: {}", serverName);
        isRunning = false;

        // 1. 删除所有 subscription
        if (!subscriptionMap.isEmpty()) {
            LogUtil.logInfo(true, serverName, "Deleting {} subscriptions...", subscriptionMap.size());
            for (Map.Entry<String, OpcUaSubscription> entry : subscriptionMap.entrySet()) {
                try {
                    entry.getValue().delete();
                    LogUtil.logInfo(true, serverName, "Deleted subscription for nodeGroup: {}", entry.getKey());
                } catch (Exception e) {
                    LogUtil.logError(true, serverName, "Deleting subscription for {}: {}", entry.getKey(), e.getMessage());
                }
            }
            subscriptionMap.clear();
            LogUtil.logInfo(true, serverName, "All subscriptions deleted and map cleared");
        }

        // 2. 断开 OPC UA 客户端连接
        if (client != null) {
            try {
                client.disconnect();
                LogUtil.logInfo(true, serverName, "OPC UA client disconnected successfully");
            } catch (Exception e) {
                LogUtil.logError(true, serverName, "Disconnecting OPC UA client: {}", e.getMessage());
            }
        }

        // 3. 清空 nodeGroup 列表
        if (customModuleIsEnabled) {
            if (!customModuleNodeGroupList.isEmpty()) {
                int size = customModuleNodeGroupList.size();
                customModuleNodeGroupList.clear();
                LogUtil.logInfo(true, serverName, "Cleared {} node groups from local cache of custom module", size);
            }
        }
        if (alarmModuleIsEnabled) {
            if (!alarmModuleNodeGroupList.isEmpty()) {
                int size = alarmModuleNodeGroupList.size();
                alarmModuleNodeGroupList.clear();
                LogUtil.logInfo(true, serverName, "Cleared {} node groups from local cache of alarm module", size);
            }
        }

        LogUtil.logInfo(true, serverName, "Shutdown process completed for server: {}", serverName);
    }

    /**
     * 为单个 nodeGroup 创建独立的 subscription
     *
     * @param client    OPC UA 客户端
     * @param nodeGroup 节点组
     */
    private void createSubscriptionForNodeGroup(OpcUaClient client, OpcUaNodeGroup nodeGroup) throws Exception {
        String groupName = nodeGroup.getModuleType().toString().toLowerCase() + "_" + nodeGroup.getName();
        Integer sampleInterval = nodeGroup.getSampleInterval();
        LogUtil.logInfo(true, serverName, "Creating subscription for nodeGroup: {}", groupName);

        // 首先读取节点组中的所有节点并更新，防止有数据永远不变导致获取不到真实值

        // 创建订阅
        OpcUaSubscription subscription = new OpcUaSubscription(client);
        subscription.setPublishingInterval(Double.valueOf(sampleInterval));

        // 设置订阅级别的数据变化监听器
        subscription.setSubscriptionListener(new OpcUaSubscription.SubscriptionListener() {
            @Override
            public void onDataReceived(OpcUaSubscription subscription, List<OpcUaMonitoredItem> items, List<DataValue> values) {
                // 处理该 nodeGroup 的数据
                handleDataChange(nodeGroup, items, values);
            }
        });

        // 在服务器上创建订阅
        subscription.create();

        // 根据节点类型添加监控项
        if (nodeGroup.getNodeType() == NodeGroupType.ARRAY) {
            // 只添加数组的第一个元素到订阅
            OpcUaNode node = nodeGroup.getNodeList().get(0);
            OpcUaMonitoredItem monitoredItem = OpcUaMonitoredItem.newDataItem(node.getNodeId());
            monitoredItem.setSamplingInterval(sampleInterval);

            // 添加监控项到该 nodeGroup 的订阅
            subscription.addMonitoredItem(monitoredItem);
            LogUtil.logInfo(true, serverName, "Added monitored item for node: {},{} in group: {}", node.getName(), node.getNodeId(), groupName);

        } else if (nodeGroup.getNodeType() == NodeGroupType.SCALAR) {
            // 为 SCALAR 类型的每个节点创建监控项
            for (OpcUaNode node : nodeGroup.getNodeList()) {
                OpcUaMonitoredItem monitoredItem = OpcUaMonitoredItem.newDataItem(node.getNodeId());
                monitoredItem.setSamplingInterval(sampleInterval);

                // 添加监控项到该 nodeGroup 的订阅
                subscription.addMonitoredItem(monitoredItem);
                LogUtil.logInfo(true, serverName, "Added monitored item for node: {},{}  in group: {}", node.getName(), node.getNodeId(), groupName);
            }
        }

        // 同步监控项到服务器
        try {
            subscription.synchronizeMonitoredItems();
            LogUtil.logInfo(true, serverName, "Successfully synchronized monitored items for nodeGroup: {}", groupName);
        } catch (MonitoredItemSynchronizationException e) {
            LogUtil.logError(true, serverName, "Failed to synchronize monitored items for nodeGroup: {}", groupName, e);
            e.getCreateResults().forEach(result ->
                    LogUtil.logError(true, serverName, "Failed to create item: nodeId={}, serviceResult={}, operationResult={}",
                            result.monitoredItem().getReadValueId().getNodeId(),
                            result.serviceResult(),
                            result.operationResult())
            );
        }

        // 将 subscription 保存到 map 中，方便后续管理
        subscriptionMap.put(groupName, subscription);
        LogUtil.logInfo(true, serverName, "Subscription created successfully for nodeGroup: {}", groupName);
    }

    /**
     * 处理 OPC UA 节点数据变化
     * 策略：
     * - SCALAR 类型：每个节点独立订阅，分别更新
     * - ARRAY 类型：只订阅第一个节点，收到数组后拆分赋值给所有节点
     *
     * @param nodeGroup 节点组
     * @param items     监控项列表
     * @param values    数据值列表
     */
    private void handleDataChange(OpcUaNodeGroup nodeGroup, List<OpcUaMonitoredItem> items, List<DataValue> values) {
        try {
            // 参数验证
            if (items == null || values == null || items.isEmpty() || values.isEmpty()) {
                LogUtil.logWarning(true, serverName, "Received empty data for nodeGroup: {}", nodeGroup.getName());
                return;
            }

            if (items.size() != values.size()) {
                LogUtil.logWarning(true, serverName, "Items and values size mismatch for nodeGroup: {}", nodeGroup.getName());
                return;
            }

            String groupName = nodeGroup.getName();
            boolean hasChanges = false;

            // 根据节点组类型采用不同的处理策略
            if (nodeGroup.getNodeType() == NodeGroupType.ARRAY) {
                // ARRAY 类型：处理数组数据，拆分后赋值给各个节点
                hasChanges = updateArrayData(nodeGroup, items, values);
            } else if (nodeGroup.getNodeType() == NodeGroupType.SCALAR) {
                // SCALAR 类型：每个节点独立更新
                hasChanges = updateScalarData(nodeGroup, items, values);
            }

            // 如果有节点发生变化，将数据入队
            if (hasChanges) {
                // 检查是否有节点值为 null
                for (OpcUaNode node : nodeGroup.getNodeList()) {
                    if (node.getNewValue() == null) {
                        LogUtil.logWarning(true, serverName, "Node {} in group {} has no value yet",
                                node.getName(), groupName);
                    }
                }
                if (nodeGroup.getModuleType() == ModuleType.CUSTOM) {
                    // 将数据入队
                    boolean success = globalDataQueue.enqueueCustomData(serverName, (CustomOpcUaNodeGroup) nodeGroup);

                    if (success) {
                        LogUtil.logDebugL1(true, serverName, "Data enqueued successfully for nodeGroup: {}_{}, nodes count: {}",
                                ModuleType.CUSTOM.toString(), groupName, nodeGroup.getNodeList().size());
                    } else {
                        LogUtil.logWarning(true, serverName, "Failed to enqueue data for nodeGroup: {}_{} - queue may be full", ModuleType.CUSTOM.toString(), groupName);
                    }
                }

                if (nodeGroup.getModuleType() == ModuleType.ALARM) {
                    // 将数据入队
                    boolean success = globalDataQueue.enqueueAlarmData(serverName, (AlarmOpcUaNodeGroup) nodeGroup);

                    if (success) {
                        LogUtil.logDebugL1(true, serverName, "Data enqueued successfully for nodeGroup: {}_{}, nodes count: {}",
                                ModuleType.ALARM.toString(), groupName, nodeGroup.getNodeList().size());
                    }
                }
            }

        } catch (Exception e) {
            LogUtil.logError(true, serverName, "Error handling data change for nodeGroup {}: {}",
                    nodeGroup.getName(), e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理 SCALAR 类型的节点数据
     * 每个节点独立订阅，分别更新
     *
     * @param nodeGroup 节点组
     * @param items     监控项列表
     * @param values    数据值列表
     * @return 是否有节点发生变化
     */
    private boolean updateScalarData(OpcUaNodeGroup nodeGroup, List<OpcUaMonitoredItem> items, List<DataValue> values) {
        boolean hasChanges = false;

        for (int i = 0; i < items.size(); i++) {
            OpcUaMonitoredItem item = items.get(i);
            DataValue dataValue = values.get(i);

            // 获取节点 ID
            NodeId nodeId = item.getReadValueId().getNodeId();

            // 在 nodeGroup 中找到对应的 OpcUaNode
            OpcUaNode matchingNode = findNodeByNodeId(nodeGroup, nodeId);

            if (matchingNode != null && dataValue.getValue() != null) {
                Object value = dataValue.getValue().getValue();

                if (value != null) {
                    // 更新节点值（会自动保存 oldValue）
                    matchingNode.updateValue(value);
                    hasChanges = true;

                    LogUtil.logDebugL1(true, serverName, "Node {}-{} updated: {} -> {}",
                            nodeGroup.getName(), matchingNode.getName(), matchingNode.getOldValue(), matchingNode.getNewValue());

                }
            }
        }

        return hasChanges;
    }

    /**
     * 处理 ARRAY 类型的节点数据
     * 只订阅了第一个节点，但收到的是整个数组，需要拆分后赋值给各个节点
     *
     * @param nodeGroup 节点组
     * @param items     监控项列表（只有一个元素）
     * @param values    数据值列表（只有一个元素，包含数组）
     * @return 是否有节点发生变化
     */
    private boolean updateArrayData(OpcUaNodeGroup nodeGroup, List<OpcUaMonitoredItem> items, List<DataValue> values) {
        // ARRAY 类型应该只有一个监控项（订阅的第一个节点）
        if (items.size() != 1 || values.size() != 1) {
            LogUtil.logWarning(true, serverName, "ARRAY nodeGroup should have only 1 monitored item, but got {}", items.size());
            return false;
        }

        DataValue dataValue = values.get(0);
        if (dataValue.getValue() == null) {
            LogUtil.logWarning(true, serverName, "Received null value for ARRAY nodeGroup: {}", nodeGroup.getName());
            return false;
        }

        Object arrayValue = dataValue.getValue().getValue();

        // 检查是否为数组类型
        if (arrayValue == null || !arrayValue.getClass().isArray()) {
            LogUtil.logWarning(true, serverName, "Expected array value for ARRAY nodeGroup {}, but got: {}",
                    nodeGroup.getName(), arrayValue != null ? arrayValue.getClass().getName() : "null");
            return false;
        }

        int arrayLength = Array.getLength(arrayValue);
        List<OpcUaNode> nodeList = nodeGroup.getNodeList();

        // 检查数组长度是否与节点数量匹配
        if (arrayLength < nodeList.size()) {
            LogUtil.logWarning(true, serverName, "Array length ({}) less than node count ({}) for nodeGroup: {}",
                    arrayLength, nodeList.size(), nodeGroup.getName());
        }

        boolean hasChanges = false;

        // 将数组拆分，分别赋值给每个节点
        for (int i = 0; i < nodeList.size(); i++) {
            OpcUaNode node = nodeList.get(i);

            // 从数组中获取对应索引的值
            Object elementValue = null;
            if (i < arrayLength) {
                elementValue = Array.get(arrayValue, i);
            }

            if (elementValue != null) {
                // 更新节点值
                node.updateValue(elementValue);
                hasChanges = true;

                LogUtil.logDebugL1(true, serverName, "Node {}-{} updated: {} -> {}",
                        nodeGroup.getName(), node.getName(), node.getOldValue(), node.getNewValue());

            }
        }

        LogUtil.logDebugL1(true, serverName, "ARRAY nodeGroup {} processed: {} elements from array of length {}",
                nodeGroup.getName(), nodeList.size(), arrayLength);

        return hasChanges;
    }

    /**
     * 根据 NodeId 在 nodeGroup 中查找对应的 OpcUaNode
     *
     * @param nodeGroup 节点组
     * @param nodeId    OPC UA NodeId
     * @return 匹配的 OpcUaNode，未找到返回 null
     */
    private OpcUaNode findNodeByNodeId(OpcUaNodeGroup nodeGroup, NodeId nodeId) {
        return nodeGroup.getNodeByNodeId(nodeId);
    }

}
