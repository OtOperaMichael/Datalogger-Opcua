package com.lego.serverTask;

import com.lego.pojo.template.ModuleType;
import com.lego.pojo.template.NodeGroupType;
import com.lego.pojo.Server;
import com.lego.pojo.template.Table;
import com.lego.pojo.template.Template;
import com.lego.pojo.template.alarm.AlarmNode;
import com.lego.pojo.template.communication.CommNode;
import com.lego.pojo.template.custom.CustomNode;
import com.lego.serverTask.protocols.opcua.*;
import com.lego.util.DBUtil;
import com.lego.util.LogUtil;
import com.lego.util.TemplateUtil;
import lombok.Getter;
import org.eclipse.milo.opcua.sdk.client.*;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ArrayList<CommOpcUaNodeGroup> commModuleNodeGroupList = new ArrayList<>();

    // 引用全局队列
    private final GlobalDataQueue globalDataQueue;

    // 单一订阅，管理所有监控项
    private OpcUaSubscription mainSubscription;

    // 保存 nodeGroup 名称到监控项列表的映射，用于数据处理时区分
    private Map<String, List<OpcUaMonitoredItem>> nodeGroupMonitoredItemsMap = new HashMap<>();

    // 保存 client 引用,用于关闭时断开连接
    private OpcUaClient client;

    // 标记任务是否正在运行
    @Getter
    private volatile boolean isRunning = false;

    // 自动重连相关
    private ScheduledExecutorService reconnectExecutor;
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long RECONNECT_DELAY_SECONDS = 5;

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

                Table<CustomNode> table = template.getCustom().getTableList().get(index);
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

                Table<AlarmNode> table = template.getAlarm().getTableList().get(index);
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
        if (communicationModuleIsEnabled) {

            for (int index = 0; index < template.getCommunication().getTableList().size(); index++) {

                Table<CommNode> table = template.getCommunication().getTableList().get(index);
                //从前端传来的tagGroup有可能为空，为空则跳过
                if (!table.getName().isEmpty()) {
                    CommOpcUaNodeGroup nodeGroup = new CommOpcUaNodeGroup(serverName, ModuleType.COMMUNICATION, table);
                    commModuleNodeGroupList.add(nodeGroup);

                } else {
                    break;
                }

            }

            //create table, hyper, one single table for comm module, called "comm_history"
            DBUtil.createHyperTable(serverName, commModuleNodeGroupList.get(0));

        }


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
            connectAndSubscribe();

            isRunning = true;
            LogUtil.logInfo(true, serverName, "Server {} started successfully", serverName);

        } catch (Exception e) {
            isRunning = false;
            LogUtil.logError(true, serverName, "Failed to start server: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 执行连接和订阅的核心逻辑
     */
    private void connectAndSubscribe() throws Exception {
        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(serverUrl).get();
        EndpointDescription endpoint = endpoints.stream()
                .filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getUri()))
                .findFirst()
                .orElse(null);

        if (endpoint == null) {
            LogUtil.logError(true, serverName, "No endpoint with SecurityPolicy.None found for server: {}", serverUrl);
            throw new Exception("No suitable endpoint found");
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

        OpcUaClientConfig config = OpcUaClientConfig.builder()
                .setEndpoint(fixedEndpoint)
                .setSessionTimeout(UInteger.valueOf(60000))
                .setRequestTimeout(UInteger.valueOf(10000))
                .setMaxPendingPublishRequests(UInteger.valueOf(1))
                .setKeepAliveInterval(UInteger.valueOf(30000))
                .setKeepAliveFailuresAllowed(UInteger.valueOf(5))
                .build();
        client = OpcUaClient.create(config);

        client.addSessionActivityListener(new SessionActivityListener() {
            @Override
            public void onSessionActive(UaSession session) {
                LogUtil.logDebugL1(true, serverName, "Opcua session activated");
            }

            @Override
            public void onSessionInactive(UaSession session) {
                LogUtil.logDebugL1(true, serverName, "Opcua session deactivated");
                handleConnectionLost();
            }
        });


        client.addFaultListener(faultEvent -> {
            LogUtil.logError(true, serverName, "OPC UA fault received: {}", faultEvent.toString());
        });

        client.connect();
        LogUtil.logInfo(true, serverName, "Connected to OPC UA Server: {}", serverUrl);

        createSubscriptions();

        reconnectAttempts.set(0);
    }

    /**
     * 创建所有模块的订阅 - 使用单一 Subscription 管理所有监控项
     * Publish Interval = 所有 group 的 sampling interval 的最小值
     */
    private void createSubscriptions() throws Exception {
        if (customModuleNodeGroupList.isEmpty() && alarmModuleNodeGroupList.isEmpty() && commModuleNodeGroupList.isEmpty()) {
            LogUtil.logWarning(true, serverName, "No node groups to subscribe");
            return;
        }

        // 找到最小的采样间隔作为订阅的发布间隔
        Integer minSampleInterval = Integer.MAX_VALUE;

        for (CustomOpcUaNodeGroup ng : customModuleNodeGroupList) {
            minSampleInterval = Math.min(minSampleInterval, ng.getSampleInterval());
        }
        for (AlarmOpcUaNodeGroup ng : alarmModuleNodeGroupList) {
            minSampleInterval = Math.min(minSampleInterval, ng.getSampleInterval());
        }
        for (CommOpcUaNodeGroup ng : commModuleNodeGroupList) {
            minSampleInterval = Math.min(minSampleInterval, ng.getSampleInterval());
        }

        LogUtil.logInfo(true, serverName, "Creating single subscription with publishing interval: {} ms (min of all sampling intervals)",
                minSampleInterval);

        // 创建唯一的订阅
        mainSubscription = new OpcUaSubscription(client);
        mainSubscription.setPublishingInterval(Double.valueOf(minSampleInterval));
        mainSubscription.setLifetimeCount(UInteger.valueOf(1000));
        mainSubscription.setMaxKeepAliveCount(UInteger.valueOf(10));
        mainSubscription.setMaxNotificationsPerPublish(UInteger.valueOf(500));
        mainSubscription.setPriority(UByte.valueOf((short) 1));

        // 设置订阅级别的数据变化监听器
        mainSubscription.setSubscriptionListener(new OpcUaSubscription.SubscriptionListener() {
            @Override
            public void onDataReceived(OpcUaSubscription subscription, List<OpcUaMonitoredItem> items, List<DataValue> values) {
                handleDataChangeFromSingleSubscription(items, values);
            }
        });

        // 在服务器上创建订阅
        mainSubscription.create();

        // 为所有 nodeGroup 添加监控项，每个监控项使用自己的采样间隔
        int totalItems = 0;

        if (customModuleIsEnabled) {
            for (CustomOpcUaNodeGroup nodeGroup : customModuleNodeGroupList) {
                totalItems += addMonitoredItemsForNodeGroup(nodeGroup);
            }
        }

        if (alarmModuleIsEnabled) {
            for (AlarmOpcUaNodeGroup nodeGroup : alarmModuleNodeGroupList) {
                totalItems += addMonitoredItemsForNodeGroup(nodeGroup);
            }
        }

        if (communicationModuleIsEnabled) {
            for (CommOpcUaNodeGroup nodeGroup : commModuleNodeGroupList) {
                totalItems += addMonitoredItemsForNodeGroup(nodeGroup);
            }
        }

        // 同步所有监控项到服务器
        try {
            mainSubscription.synchronizeMonitoredItems();
            LogUtil.logInfo(true, serverName, "Successfully synchronized {} monitored items across {} node groups",
                    totalItems, nodeGroupMonitoredItemsMap.size());
        } catch (MonitoredItemSynchronizationException e) {
            LogUtil.logError(true, serverName, "Failed to synchronize monitored items", e);
            e.getCreateResults().forEach(result ->
                    LogUtil.logError(true, serverName, "Failed to create item: nodeId={}, serviceResult={}, operationResult={}",
                            result.monitoredItem().getReadValueId().getNodeId(),
                            result.serviceResult(),
                            result.operationResult())
            );
        }

        LogUtil.logInfo(true, serverName, "Single subscription created successfully with {} node groups and {} total monitored items",
                nodeGroupMonitoredItemsMap.size(), totalItems);
    }

    /**
     * 为单个 nodeGroup 添加监控项到主订阅
     * 每个 nodeGroup 使用自己配置的采样间隔
     *
     * @return 添加的监控项数量
     */
    private int addMonitoredItemsForNodeGroup(OpcUaNodeGroup nodeGroup) throws Exception {
        String groupName = nodeGroup.getModuleType().toString().toLowerCase() + "_" + nodeGroup.getName();
        Integer sampleInterval = nodeGroup.getSampleInterval();
        List<OpcUaMonitoredItem> groupItems = new ArrayList<>();

        LogUtil.logInfo(true, serverName, "Adding monitored items for nodeGroup: {} with sampling interval: {} ms",
                groupName, sampleInterval);

        if (nodeGroup.getNodeType() == NodeGroupType.ARRAY) {
            // ARRAY 类型：只添加数组的第一个元素到订阅
            OpcUaNode node = nodeGroup.getNodeList().get(0);
            OpcUaMonitoredItem monitoredItem = OpcUaMonitoredItem.newDataItem(node.getNodeId());
            monitoredItem.setSamplingInterval(sampleInterval);
            monitoredItem.setQueueSize(UInteger.valueOf(1));
            mainSubscription.addMonitoredItem(monitoredItem);
            groupItems.add(monitoredItem);
            LogUtil.logInfo(true, serverName, "Added array monitored item for node: {},{} in group: {} (sampling: {} ms)",
                    node.getName(), node.getNodeId(), groupName, sampleInterval);

        } else if (nodeGroup.getNodeType() == NodeGroupType.SCALAR) {
            // SCALAR 类型：为每个节点创建监控项
            for (OpcUaNode node : nodeGroup.getNodeList()) {
                OpcUaMonitoredItem monitoredItem = OpcUaMonitoredItem.newDataItem(node.getNodeId());
                monitoredItem.setSamplingInterval(sampleInterval);
                monitoredItem.setQueueSize(UInteger.valueOf(1));
                mainSubscription.addMonitoredItem(monitoredItem);
                groupItems.add(monitoredItem);
                LogUtil.logDebugL1(true, serverName, "Added scalar monitored item for node: {},{} in group: {} (sampling: {} ms)",
                        node.getName(), node.getNodeId(), groupName, sampleInterval);
            }
        }

        // 保存该 nodeGroup 的监控项列表
        nodeGroupMonitoredItemsMap.put(groupName, groupItems);

        return groupItems.size();
    }

    /**
     * 处理连接丢失，触发自动重连
     */
    private void handleConnectionLost() {
        if (!isRunning || isReconnecting.get()) {
            return;
        }

        LogUtil.logWarning(true, serverName, "Connection lost, scheduling reconnection...");

        if (reconnectExecutor == null || reconnectExecutor.isShutdown()) {
            reconnectExecutor = new ScheduledThreadPoolExecutor(1, r -> {
                Thread thread = new Thread(r, "Reconnect-Thread-" + serverName);
                thread.setDaemon(true);
                return thread;
            });
        }

        reconnectExecutor.schedule(this::attemptReconnect, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 尝试重新连接
     */
    private void attemptReconnect() {
        if (!isRunning) {
            LogUtil.logInfo(true, serverName, "Server is not running, canceling reconnection");
            return;
        }

        if (isReconnecting.get()) {
            LogUtil.logWarning(true, serverName, "Reconnection already in progress");
            return;
        }

        int attempts = reconnectAttempts.incrementAndGet();
        if (attempts > MAX_RECONNECT_ATTEMPTS) {
            LogUtil.logError(true, serverName, "Max reconnection attempts ({}) reached, giving up", MAX_RECONNECT_ATTEMPTS);
            isRunning = false;
            return;
        }

        if (!isReconnecting.compareAndSet(false, true)) {
            LogUtil.logWarning(true, serverName, "Another reconnection attempt is in progress");
            return;
        }

        try {
            LogUtil.logInfo(true, serverName, "Attempting reconnection (attempt {}/{})", attempts, MAX_RECONNECT_ATTEMPTS);

            cleanupConnection();

            connectAndSubscribe();

            isReconnecting.set(false);
            reconnectAttempts.set(0);
            LogUtil.logInfo(true, serverName, "Reconnection successful");

        } catch (Exception e) {
            isReconnecting.set(false);
            LogUtil.logError(true, serverName, "Reconnection attempt {} failed: {}", attempts, e.getMessage());

            if (isRunning && attempts < MAX_RECONNECT_ATTEMPTS) {
                long delay = RECONNECT_DELAY_SECONDS * attempts;
                LogUtil.logInfo(true, serverName, "Scheduling next reconnection attempt in {} seconds", delay);
                reconnectExecutor.schedule(this::attemptReconnect, delay, TimeUnit.SECONDS);
            } else if (attempts >= MAX_RECONNECT_ATTEMPTS) {
                // 新增：明确记录已达到最大重试次数并停止
                LogUtil.logError(true, serverName, "Max reconnection attempts ({}) reached, giving up. Server task will stop.", MAX_RECONNECT_ATTEMPTS);
                isRunning = false;
            }
        }
    }

    /**
     * 清理当前连接资源（不断开连接状态标记）
     */
    private void cleanupConnection() {
        if (mainSubscription != null) {
            try {
                mainSubscription.delete();
                LogUtil.logInfo(true, serverName, "Deleted main subscription");
            } catch (Exception e) {
                LogUtil.logWarning(true, serverName, "Error deleting main subscription: {}", e.getMessage());
            }
            mainSubscription = null;
        }

        if (!nodeGroupMonitoredItemsMap.isEmpty()) {
            LogUtil.logInfo(true, serverName, "Clearing {} node group monitored items mappings",
                    nodeGroupMonitoredItemsMap.size());
            nodeGroupMonitoredItemsMap.clear();
        }

        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception e) {
                LogUtil.logWarning(true, serverName, "Error disconnecting client: {}", e.getMessage());
            }
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

        if (reconnectExecutor != null && !reconnectExecutor.isShutdown()) {
            reconnectExecutor.shutdownNow();
            LogUtil.logInfo(true, serverName, "Reconnect executor shut down");
        }

        isReconnecting.set(false);

        cleanupConnection();

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
        if (communicationModuleIsEnabled) {
            if (!commModuleNodeGroupList.isEmpty()) {
                int size = commModuleNodeGroupList.size();
                commModuleNodeGroupList.clear();
                LogUtil.logInfo(true, serverName, "Cleared {} node groups from local cache of communication module", size);
            }
        }

        LogUtil.logInfo(true, serverName, "Shutdown process completed for server: {}", serverName);
    }

    /**
     * 从单一订阅处理数据变化
     * 自动识别变化的 items 属于哪个 nodeGroup，并更新对应的值
     *
     * @param items  监控项列表
     * @param values 数据值列表
     */
    private void handleDataChangeFromSingleSubscription(List<OpcUaMonitoredItem> items, List<DataValue> values) {
        try {
            if (items == null || values == null || items.isEmpty() || values.isEmpty()) {
                LogUtil.logWarning(true, serverName, "Received empty data from subscription");
                return;
            }

            if (items.size() != values.size()) {
                LogUtil.logWarning(true, serverName, "Items and values size mismatch: items={}, values={}",
                        items.size(), values.size());
                return;
            }

            // 收集有变化的 nodeGroup（去重）
            Set<OpcUaNodeGroup> changedNodeGroups = new HashSet<>();

            // 遍历所有收到的数据变化
            for (int i = 0; i < items.size(); i++) {
                OpcUaMonitoredItem item = items.get(i);
                DataValue dataValue = values.get(i);

                if (dataValue.getValue() == null) {
                    continue;
                }

                // 获取节点 ID
                NodeId nodeId = item.getReadValueId().getNodeId();

                // 根据 nodeId 找到对应的 nodeGroup
                OpcUaNodeGroup targetNodeGroup = findNodeGroupByNodeId(nodeId);

                if (targetNodeGroup == null) {
                    LogUtil.logWarning(true, serverName, "Cannot find nodeGroup for nodeId: {}", nodeId);
                    continue;
                }

                // 处理数据更新
                boolean hasChange = false;
                if (targetNodeGroup.getNodeType() == NodeGroupType.ARRAY) {
                    hasChange = updateArrayDataForSingleNode(targetNodeGroup, dataValue);
                } else if (targetNodeGroup.getNodeType() == NodeGroupType.SCALAR) {
                    hasChange = updateScalarNode(targetNodeGroup, nodeId, dataValue);
                }

                // 标记该 nodeGroup 有变化
                if (hasChange) {
                    changedNodeGroups.add(targetNodeGroup);
                }
            }

            // 对所有有变化的 nodeGroup 进行入队处理
            for (OpcUaNodeGroup nodeGroup : changedNodeGroups) {
                enqueueNodeGroupData(nodeGroup);
            }

        } catch (Exception e) {
            LogUtil.logError(true, serverName, "Error handling data change from subscription: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 根据 NodeId 查找对应的 nodeGroup
     * 通过遍历所有 nodeGroup 的监控项来匹配
     */
    private OpcUaNodeGroup findNodeGroupByNodeId(NodeId nodeId) {
        // 遍历所有 nodeGroup 的监控项映射
        for (Map.Entry<String, List<OpcUaMonitoredItem>> entry : nodeGroupMonitoredItemsMap.entrySet()) {
            String groupName = entry.getKey();
            List<OpcUaMonitoredItem> groupItems = entry.getValue();

            // 检查该 group 中是否有匹配的监控项
            for (OpcUaMonitoredItem item : groupItems) {
                if (item.getReadValueId().getNodeId().equals(nodeId)) {
                    // 找到匹配的 group，返回对应的 nodeGroup 对象
                    return findNodeGroupByName(groupName);
                }
            }
        }

        return null;
    }

    /**
     * 根据 nodeGroup 名称查找对应的对象
     */
    private OpcUaNodeGroup findNodeGroupByName(String groupName) {
        // 从 groupName 中提取模块类型和表名
        // 格式: moduletype_tablename
        String[] parts = groupName.split("_", 2);
        if (parts.length != 2) {
            return null;
        }

        String moduleType = parts[0];
        String tableName = parts[1];

        if ("custom".equals(moduleType)) {
            for (CustomOpcUaNodeGroup ng : customModuleNodeGroupList) {
                if (ng.getName().equals(tableName)) {
                    return ng;
                }
            }
        } else if ("alarm".equals(moduleType)) {
            for (AlarmOpcUaNodeGroup ng : alarmModuleNodeGroupList) {
                if (ng.getName().equals(tableName)) {
                    return ng;
                }
            }
        } else if ("communication".equals(moduleType)) {
            for (CommOpcUaNodeGroup ng : commModuleNodeGroupList) {
                if (ng.getName().equals(tableName)) {
                    return ng;
                }
            }
        }

        return null;
    }

    /**
     * 更新 SCALAR 类型的单个节点
     */
    private boolean updateScalarNode(OpcUaNodeGroup nodeGroup, NodeId nodeId, DataValue dataValue) {
        Object value = dataValue.getValue().getValue();
        if (value == null) {
            return false;
        }

        OpcUaNode matchingNode = findNodeByNodeId(nodeGroup, nodeId);
        if (matchingNode != null) {
            matchingNode.updateValue(value);
            return true;
        }

        return false;
    }

    /**
     * 更新 ARRAY 类型的数据（从单个节点接收数组）
     */
    private boolean updateArrayDataForSingleNode(OpcUaNodeGroup nodeGroup, DataValue dataValue) {
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
                node.updateValue(elementValue);
                hasChanges = true;
            }
        }

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

    /**
     * 将 nodeGroup 的数据入队
     */
    private void enqueueNodeGroupData(OpcUaNodeGroup nodeGroup) {
        String groupName = nodeGroup.getName();

        // 检查是否有节点值为 null
        for (OpcUaNode node : nodeGroup.getNodeList()) {
            if (node.getNewValue() == null) {
                LogUtil.logWarning(true, serverName, "Node {} in group {} has no value yet",
                        node.getName(), groupName);
            }
        }

        if (nodeGroup.getModuleType() == ModuleType.CUSTOM) {
            boolean success = globalDataQueue.enqueueCustomData(serverName, (CustomOpcUaNodeGroup) nodeGroup);
            if (success) {
                LogUtil.logDebugL1(true, serverName, "Data enqueued successfully for nodeGroup: {}_{}, nodes count: {}",
                        ModuleType.CUSTOM.toString(), groupName, nodeGroup.getNodeList().size());
            }
        } else if (nodeGroup.getModuleType() == ModuleType.ALARM) {
            boolean success = globalDataQueue.enqueueAlarmData(serverName, (AlarmOpcUaNodeGroup) nodeGroup);
            if (success) {
                LogUtil.logDebugL1(true, serverName, "Data enqueued successfully for nodeGroup: {}_{}, nodes count: {}",
                        ModuleType.ALARM.toString(), groupName, nodeGroup.getNodeList().size());
            }
        } else if (nodeGroup.getModuleType() == ModuleType.COMMUNICATION) {
            boolean success = globalDataQueue.enqueueCommunicationData(serverName, (CommOpcUaNodeGroup) nodeGroup);
            if (success) {
                LogUtil.logDebugL1(true, serverName, "Data enqueued successfully for nodeGroup: {}_{}, nodes count: {}",
                        ModuleType.COMMUNICATION.toString(), groupName, nodeGroup.getNodeList().size());
            }
        }
    }
}
