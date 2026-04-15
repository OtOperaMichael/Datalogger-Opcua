package com.lego.serverTask;

import com.lego.pojo.template.custom.Table;
import com.lego.pojo.Server;
import com.lego.pojo.template.Template;
import com.lego.serverTask.protocols.libplctag.CIPTagGroup;
import com.lego.serverTask.protocols.opcua.OpcUaNodeGroup;
import com.lego.util.DBUtil;
import com.lego.util.TemplateUtil;
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

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private  String serverUrl;
    private  Integer sampleInterval;
    private final Template template;

    private ArrayList<OpcUaNodeGroup> customModuleNodeGroupList = new ArrayList<>();

    private int minute;
    private int second;
    private long startTime;
    private long stopTime;
    private long costTime;
    private boolean isTagGroupChanged;

    // 引用全局队列
    private final GlobalDataQueue globalDataQueue;

    //构造器，初始化tagGroups
    public OpcuaDatalogger(Server server) {
        serverName = server.getName();
        templateName = server.getTemplateName();
        template = TemplateUtil.resolveTemplate(server);

        // 获取全局队列实例
        globalDataQueue = GlobalDataQueue.getInstance();

        //如果需要的template被配置了
        if (template != null) {

            //create database 为每一个server instance, e.g. P86B
            if (template.getCustom().isEnable() || template.getAlarm().isEnable() || template.getCommunication().isEnable()) {
                try {
                    DBUtil.createSchemaIfNotExists(serverName);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            String hostIP = server.getHostIP();
            String port = (template.getPort() != null) ? template.getPort() : "4840";
            String postfix = (template.getPostfix() != null) ? template.getPostfix() : "";
            serverUrl = "opc.tcp://" + hostIP + ":" + port + postfix;

            sampleInterval = template.getSampleInterval();

            //Module1: custom, 遍历template中的所有table
            if (template.getCustom().isEnable()) {

                for (int index = 0; index < template.getCustom().getTablelist().size(); index++) {

                    Table table = template.getCustom().getTablelist().get(index);
                    //从前端传来的tagGroup有可能为空，为空则跳过
                    if (!table.getName().isEmpty()) {
                        OpcUaNodeGroup nodeGroup = new OpcUaNodeGroup(serverName, table);
                        customModuleNodeGroupList.add(nodeGroup);
                        //create hyper table for each tagGroup
                        DBUtil.createHyperTable(serverName, nodeGroup);

                    } else {
                        break;
                    }
                }

            }

            //Module2: alarm

            //Module3: communication

        }
    }


//    private CIPTagGroup createCipTagGroup(String serverName, Table table) {
//
//        List<String> tagNameList = table.getTagNameList();
//
//        String tagAddr = "";
//        TagType tagType;
//        ArrayList<String> tagNames = new ArrayList<>();
//
//        for (int i = 0; i < tagNameList.size(); i++) {
//            //从前端输入的tagname有可能为空，为空则为无效
//            if ((tagNameList.get(i) != null) && !tagNameList.get(i).isEmpty()) {
//                tagNames.add(table.getTagNameList().get(i));
//            } else {
//                break;
//            }
//        }
//
//        StringBuilder str = new StringBuilder();
//        str.append("protocol=ab-eip&gateway=");
//        str.append(hostIP);
//        str.append("&path=");
//        str.append(template.getHostCpuSlot());
//        str.append("&plc=ControlLogix&elem_count=");
//        str.append(tagNames.size());
//        str.append("&name=");
//        str.append(table.getTagAddr());
//        tagAddr = str.toString();
//        tagType = table.getTagType();
//
//        return new CIPTagGroup(serverName, table.getName(), tagAddr, tagType, tagNames);
//    }

    /**
     * 启动 OPC UA 连接和订阅
     */
    public void start() throws Exception {

        // 获取服务端点描述
        List<EndpointDescription> endpoints = DiscoveryClient.getEndpoints(serverUrl).get();

        // 选择无安全策略的端点
        EndpointDescription endpoint = endpoints.stream()
                .filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getUri()))
                .findFirst()
                .orElseThrow(() -> new Exception("No endpoint with SecurityPolicy.None found"));

        // 创建客户端配置
        OpcUaClientConfig config = OpcUaClientConfig.builder()
                .setEndpoint(endpoint)
                .build();

        // 创建客户端
        OpcUaClient client = OpcUaClient.create(config);

        try {
            // 连接到 OPC UA 服务器
            client.connect();
            DBUtil.writeLogToDB(serverName , "Connected to OPC UA Server: " + serverUrl);

            // 创建订阅
            OpcUaSubscription subscription = new OpcUaSubscription(client);
            subscription.setPublishingInterval(Double.valueOf(sampleInterval));


            // Set a listener for data changes at the subscription level.
            subscription.setSubscriptionListener(
                    new OpcUaSubscription.SubscriptionListener() {
                        @Override
                        public void onDataReceived(
                                OpcUaSubscription subscription,
                                List<OpcUaMonitoredItem> items,
                                List<DataValue> values) {

                            for (int i = 0; i < items.size(); i++) {
                                logger.info(
                                        "subscription onDataReceived: nodeId={}, value={}",
                                        items.get(i).getReadValueId().getNodeId(),
                                        values.get(i).value());
                            }
                        }
                    });

            // Create the subscription on the server.
            subscription.create();

            // 为每个节点创建监控项并添加到订阅
            for (NodeId nodeId : NODE_IDS) {
                OpcUaMonitoredItem monitoredItem = OpcUaMonitoredItem.newDataItem(nodeId);
                monitoredItem.setSamplingInterval(SAMPLING_INTERVAL);

                // 添加监控项到订阅
                subscription.addMonitoredItem(monitoredItem);
            }

            // Synchronize the MonitoredItems with the server.
            // This will create, modify, and delete items as necessary.
            try {
                subscription.synchronizeMonitoredItems();
            } catch (MonitoredItemSynchronizationException e) {
                e.getCreateResults()
                        .forEach(
                                result ->
                                        logger.warn(
                                                "failed to create item: nodeId={}, serviceResult={}, operationResult={}",
                                                result.monitoredItem().getReadValueId().getNodeId(),
                                                result.serviceResult(),
                                                result.operationResult()));
            }


            // 保持运行状态，持续接收数据
            logger.info("Subscription is active. Waiting for data changes...");
            logger.info("Press Ctrl+C to stop.");

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                try {
                    subscription.delete();
                    client.disconnect();
                    logger.info("Disconnected from server");
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));

            // 让程序持续运行
            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            logger.error("Error in subscription", e);
            throw e;
        } finally {
            // 清理资源
            try {
                client.disconnect();
            } catch (Exception e) {
                logger.error("Error disconnecting", e);
            }
        }

        minute = LocalDateTime.now().getMinute();
        second = LocalDateTime.now().getSecond();

        for (CIPTagGroup cipTagGroup : customModuleNodeGroupList) {

            //log reading time, if time is too long
            //or every 20 minutes, as heart beat for each instance
            startTime = System.currentTimeMillis();
            isTagGroupChanged = cipTagGroup.isTagGroupChanged();
            stopTime = System.currentTimeMillis();
            costTime = stopTime - startTime;
            if ((costTime > 50) || (minute % 20 == 0 && second == 0)) {
                DBUtil.writeLogToDB(serverName, serverName + " " + " read tags taking time: " + (stopTime - startTime) + "ms");
            }

            if (isTagGroupChanged) {
                // 将数据写入全局队列，由消费者线程池统一处理， 使用InfluxDB3数据库
                globalDataQueue.enqueueCustomData(serverName, cipTagGroup);
            }

        }
    }

    /**
     * 销毁所有的 tag group
     */
    public void destroyAllTags() {
        System.out.println(serverName + ": Destroying all tag groups for server");
        DBUtil.writeLogToDB(serverName, "Destroying all tag groups for server: ");

        for (CIPTagGroup tagGroup : customModuleNodeGroupList) {
            try {
                tagGroup.destoryTagGroup();
            } catch (Exception e) {
                System.err.println("Error destroying tag group: " + e.getMessage());
                DBUtil.writeLogToDB(serverName, "Error destroying tag group: " + e.getMessage());
            }
        }
        customModuleNodeGroupList.clear();
        System.out.println(serverName + ": All tag groups destroyed for server");
        DBUtil.writeLogToDB(serverName, "All tag groups destroyed for server");
    }

}
