<script setup lang="ts">
import {storeToRefs} from "pinia";
import {useServerListStore} from "@/stores/useServerListStore.ts";
import {useTemplateListStore} from "@/stores/useTemplateListStore.ts";
import {useInstanceStore} from "@/stores/useInstanceStore.ts";
import {computed, onMounted, reactive, ref} from "vue";
import request from "@/utils/request.ts";
import {message, Modal} from "ant-design-vue";
import type {ServerInterface} from "@/types/server.ts";
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";
import {useSystemConfigStore} from "@/stores/useSystemConfigStore.ts";

const serverListStore = useServerListStore()
const {serverList} = storeToRefs(serverListStore)
const {templateList} = storeToRefs(useTemplateListStore())
const instanceStore = useInstanceStore()
const loginUserStore = useLoginUserStore()
const systemConfigStore = useSystemConfigStore();

// 当前选中的server索引
const selectedServerIndex = ref(0);

// 服务器数量上限
const MAX_SERVER_COUNT = computed(() => systemConfigStore.maxServerCount || 10);

// 加载状态映射，用于显示每个服务器的 loading 状态
const loadingMap = ref<Record<string, boolean>>({});

async function showLog(index: number) {
  // 边界检查  index == 1000 表示查看app的log
  if ((index < 0 || index >= serverList.value.length) && (index !== 1000)) return;
  selectedServerIndex.value = index;

  let lineId = ""

  if (index === 1000){
    lineId = "app"
  } else {
    //获取server id
    const server = serverList.value.find((_, idx) => idx === index);
    if (!server?.name) return;
    lineId = server.name
  }

  try {
    //请求后端 server log
    let {data} = await request.get("instance/queryLogs", {params: {lineId: lineId}})
    if (data.code === 200) {
      const list = data.data.logList;
      instanceStore.loadLogs(list)
    } else {
      // 业务失败（如数据库错误、校验失败等）
      console.warn('unable to get server logs:', data);
    }
  } catch
    (err: any) {
    // 网络错误 / HTTP 非 2xx 错误（如 500, 404）
    console.error('Network or HTTP error:', err);
    message.error('Network error, please try again');
  }

}

//请求后端启动server
async function startServer(id: string) {
  loadingMap.value[id] = true;
  try {
    // 请求后端获取所有服务器信息，启动服务器需要较长时间，设置 30 秒超时
    let {data} = await request.get("server/startServer", {
      params: {id: id},
      timeout: 60000 // 30 秒超时
    })

    if (data.code === 200) {
      // 成功：刷新列表 & 关闭弹窗
      const list = data.data.serverList;
      serverListStore.loadFromData(list)
      message.success('Server start successfully');
    } else {
      // 业务失败（如数据库错误、校验失败等）
      console.warn('Backend business error:', data);
      message.error(data.message || 'Failed to start server');
    }
  } catch (err: any) {
    // 网络错误 / HTTP 非 2xx 错误（如 500, 404）
    console.error('Network or HTTP error:', err);
    message.error('Network error, please try again');
  } finally {
    loadingMap.value[id] = false;
  }

}

//请求后端停止server
async function stopServer(id: string) {
  try {
    // 请求后端获取所有服务器信息
    let {data} = await request.get("server/stopServer", {params: {id: id}})

    if (data.code === 200) {
      // 成功：刷新列表 & 关闭弹窗
      const list = data.data.serverList;
      serverListStore.loadFromData(list)
      message.success('Server stop successfully');
    } else {
      // 业务失败（如数据库错误、校验失败等）
      console.warn('Backend business error:', data);
      message.error(data.message || 'Failed to stop server');
    }
  } catch (err: any) {
    // 网络错误 / HTTP 非 2xx 错误（如 500, 404）
    console.error('Network or HTTP error:', err);
    message.error('Network error, please try again');
  }
}

// 删除server前确认
const showDeleteConfirm = (serverId: string, serverName: string) => {
  Modal.confirm({
    title: 'Confirm Deletion',
    content: `Are you sure you want to delete the server "${serverName}"? Data will be deleted permanently!`,
    okText: 'Yes, Delete',
    okType: 'danger',
    cancelText: 'Cancel',
    onOk() {
      // 执行删除操作
      deleteServer(serverId);
    },
    onCancel() {
      console.log('Delete canceled');
    }
  });
};

//请求后端删除server
async function deleteServer(id: string) {
  try {
    // 请求后端获取所有服务器信息
    let {data} = await request.get("server/deleteServer", {params: {id: id}})

    if (data.code === 200) {
      // 成功：刷新列表 & 关闭弹窗
      const list = data.data.serverList;
      serverListStore.loadFromData(list)
      visible.value = false;
      message.success('Server delete successfully');
    } else {
      // 业务失败（如数据库错误、校验失败等）
      console.warn('Backend business error:', data);
      message.error(data.message || 'Failed to delete server');
    }
  } catch (err: any) {
    // 网络错误 / HTTP 非 2xx 错误（如 500, 404）
    console.error('Network or HTTP error:', err);
    message.error('Network error, please try again');
  }
}

function newServer() {
  // 检查服务器数量是否超过上限
  if (serverList.value.length >= MAX_SERVER_COUNT.value) {
    message.warning(`Maximum number of servers (${MAX_SERVER_COUNT.value}) reached. Cannot create more servers.`);
    return;
  }

  visible.value = true
}

const visible = ref(false)

// 动态生成 select 选项：label = templateName, value = templateName
const templateOptions = computed(() => {
  return templateList.value.map(item => ({
    label: item.name,
    value: item.name // id 是唯一标识符
  }))
})

//新建instance 弹窗
const formState = reactive({name: '', template: '', hostIP: ''})
const rules = {
  name: [
    {required: true, message: 'Please input instance name', trigger: 'blur'},
    {
      pattern: /^[a-z][a-z0-9_-]*$/,
      message: 'InstanceID must start with a letter and can only contain lower case letters, digits, underscores (_), or hyphens (-), example:P86B_1',
      trigger: 'blur'
    }
  ],
  template: [
    {
      required: true,
      message: 'Please select a template',
      trigger: 'change' // 推荐用 change，因为 select 是选择触发
    }
  ],
  hostIP: [
    {required: true, message: 'Please input host IP', trigger: 'blur'},
    {
      pattern: /^(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}|([a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?\.)*[a-zA-Z0-9]([a-zA-Z0-9\-]*[a-zA-Z0-9])?\.[a-zA-Z]{2,}(:\d{1,5})?)$/,
      message: 'Must be IPv4 (e.g., 192.168.1.100) or domain with optional port (e.g., xxx.sdn.abc.com[:8080])',
      trigger: 'blur'
    }
  ]
}
const formRef = ref()

const handleOk = async () => {
  try {
    // 1. 校验表单
    await formRef.value.validateFields();

    // 如果走到这里，说明校验成功！
    console.log('Form validation passed');

  } catch (validationError: any) {
    // 表单校验失败（Ant Design Vue 会自动显示错误提示）
    console.warn('Form validation failed:', validationError);
    return; // 不继续提交
  }

  try {
    // 2.先校验 instanceName 是否重复！
    if (serverListStore.isInstanceNameDuplicate(formState.name)) {
      message.error('Instance name already exists');
      return; // 立即终止，不关闭弹窗，不提交
    }

    // 3. 名字不重复，才进行数据转换和提交
    const newServer: ServerInterface = {
      id: '', // 后端生成 = 数字 + "_" + serverName
      name: formState.name, // 注意字段映射
      templateName: formState.template,
      hostIP: formState.hostIP,
      running: false
    };
    console.log("new server:", newServer)

    // 4. 提交到后端
    const {data} = await request.post("server/saveServer", newServer);
    console.log("data:", data)

    // 5. 显式检查业务状态码
    if (data.code === 200) {
      // 成功：刷新列表 & 关闭弹窗
      const list = data.data.serverList;
      serverListStore.loadFromData(list)
      visible.value = false;
      selectedServerIndex.value = serverList.value.length - 1;
      message.success('Server added successfully');
    } else {
      // 业务失败（如数据库错误、校验失败等）
      console.warn('Backend business error:', data);
      message.error(data.message || 'Failed to add server');
    }

  } catch (err: any) {
    // 网络错误 / HTTP 非 2xx 错误（如 500, 404）
    console.error('Network or HTTP error:', err);
    message.error('Network error, please try again');
  }
}

</script>

<template>
  <div id="ServerLeftFrame" @click="showLog(1000)">
    <div
      v-for="(server, index) in serverList"
      :key="server.id"
      class="server"
      :class="{ selected: selectedServerIndex === index }"
      @click.stop="showLog(index)">
      <div class="lineInfo-name" :style="{ color: server.running ? '#389e0d' : 'inherit' }">{{ server.name }}</div>
      <div class="lineInfo">Template: {{ server.templateName }}</div>
      <div class="lineInfo">IP: {{ server.hostIP }}</div>
      <div class="btnContainer">
        <a-button
          v-show="(selectedServerIndex === index) && loginUserStore.getIsLoggedIn"
          :type="server.running ? 'success' : 'default'"
          class="btn"
          :loading="loadingMap[server.id]"
          @click.stop="startServer(server.id)"
        >Start
        </a-button>
        <a-button
          v-show="(selectedServerIndex === index) && loginUserStore.getIsLoggedIn"
          :type="!server.running ? 'success' : 'default'"
          class="btn"
          @click.stop="stopServer(server.id)"
        >Stop
        </a-button>
        <a-button
          v-show="(selectedServerIndex === index) && loginUserStore.getIsLoggedIn"
          class="btn"
          type="primary" danger
          @click.stop="showDeleteConfirm(server.id, server.name)"
        >Delete
        </a-button>
      </div>
    </div>

    <div
      class="newServer"
      v-show="loginUserStore.getIsLoggedIn"
      @click.stop="newServer()" >
      +
    </div>

    <a-modal
      v-model:open="visible"
      title="Create a new instance"
      :centered="true"
      @ok="handleOk"
      @cancel="visible = false"
      width="500px"
    >
      <a-form :model="formState" :rules="rules" ref="formRef" layout="vertical">
        <a-form-item name="name" label="Instance name:">
          <a-input v-model:value="formState.name"/>
        </a-form-item>
        <a-form-item name="template" label="Template:">
          <a-select
            v-model:value="formState.template"
            :options="templateOptions"
            placeholder="Please select a template"
            allow-clear
          />
        </a-form-item>
        <a-form-item name="hostIP" label="HostIP:">
          <a-input v-model:value="formState.hostIP"/>
        </a-form-item>
      </a-form>
    </a-modal>


  </div>

</template>

<style scoped>
#ServerLeftFrame {
  background-color: lightgrey;
  padding: 10px;
  border-radius: 5px;
  align-items: center;
  margin: 10px;
  display: flex;
  flex-wrap: wrap;
}

.server {
  background-color: white;
  padding: 10px;
  border-radius: 5px;
  height: 160px;
  width: 300px;
  margin: 10px;
  display: flex;
  flex-direction: column;
  justify-content: center; /* 垂直居中 */
  align-items: center; /* 水平居中 */
}

.server.selected {
  background-color: #88bfda; /* Ant Design 的 info 背景色 */
  padding: 10px;
  border-radius: 5px;

}

.newServer {
  background-color: rgba(255, 255, 255, 0.5); /* 白色 + 70% 不透明度 */
  padding: 10px;
  border-radius: 5px;
  height: 160px;
  width: 240px;
  margin: 10px;
  display: flex;
  flex-direction: column;
  justify-content: center; /* 垂直居中 */
  align-items: center; /* 水平居中 */
}

/* 点击时变深色 */
.newServer:active {
  background-color: gray;
}

.lineInfo {
  margin: 5px;
}

.lineInfo-name {
  font-family: 'Segoe UI', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.btnContainer {
  margin: 10px;

}

.btn {
  width: 80px;
  margin: 2px;
}
</style>
