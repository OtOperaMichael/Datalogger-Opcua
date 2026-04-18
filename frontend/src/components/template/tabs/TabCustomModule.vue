<script setup lang="ts">
import {computed, watch} from 'vue';
import {useNewTemplateStore} from "@/stores/useNewTemplateStore.ts";
import {NodeGroupType, DataType, type CustomTableInterface} from "@/types/template.ts";
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";
import {useSystemConfigStore} from "@/stores/useSystemConfigStore.ts";

const loginUserStore = useLoginUserStore()
const systemConfigStore = useSystemConfigStore();


const props = defineProps<{
  tableIndex: number
}>();

const MAX_NODE_COUNT = computed(() => systemConfigStore.maxTableFieldCount || 10);

const template = useNewTemplateStore();

const isDisabled = computed(() => !loginUserStore.getIsLoggedIn || !template.custom.enable);

const tableData = computed<CustomTableInterface>(() => {
  return template.custom.tableList[props.tableIndex] || {
    name: "",
    sampleInterval: 1000,
    nodeGroupType: NodeGroupType.SCALAR,
    nodeList: Array.from({length: 10}, () => ({
      name: "",
      nodeId: "",
      dataType: DataType.INT
    }))
  };
});

const nodeTypeOptions = [
  {label: 'SCALAR', value: NodeGroupType.SCALAR},
  {label: 'ARRAY', value: NodeGroupType.ARRAY},
]

const dataTypeOptions = [
  {label: 'BOOL', value: DataType.BOOL},
  {label: 'INT', value: DataType.INT},
  {label: 'DOUBLE', value: DataType.DOUBLE},
  {label: 'STRING', value: DataType.STRING},
]

function addNode() {
  template.addTagName('custom', props.tableIndex);
}

function removeNode(index: number) {
  template.removeTagName('custom', props.tableIndex, index);
}

function updateArrayNodeValues() {
  if (tableData.value.nodeGroupType !== NodeGroupType.ARRAY) {
    return;
  }

  const firstNode = tableData.value.nodeList[0];
  if (!firstNode) {
    return;
  }

  for (let i = 1; i < tableData.value.nodeList.length; i++) {
    const node = tableData.value.nodeList[i];
    if (node && firstNode.nodeId) {
      node.nodeId = `${firstNode.nodeId}[${i}]`;
      node.dataType = firstNode.dataType;
    }
  }
}

watch(
  () => [tableData.value.nodeList[0]?.nodeId, tableData.value.nodeList[0]?.dataType, tableData.value.nodeGroupType],
  () => {
    updateArrayNodeValues();
  },
  { deep: true }
);

function isFirstNodeEditable(nodeIndex: number): boolean {
  if (tableData.value.nodeGroupType !== NodeGroupType.ARRAY) {
    return true;
  }
  return nodeIndex === 0;
}
</script>

<template>
  <div id="tab-header">
    <div class="inputBox">
      Table name:
      <a-input
        style="width: 200px;"
        v-model:value="tableData.name"
        :disabled="isDisabled"
      />
    </div>
    <div class="inputBox">
      Sample interval (ms):
      <a-input-number
        v-model:value="tableData.sampleInterval"
        :disabled="isDisabled"
        :min="100"
        :max="1000"
      />
    </div>
    <div class="inputBox">
      Node type:
      <a-select
        style="width: 120px;"
        v-model:value="tableData.nodeGroupType"
        :options="nodeTypeOptions"
        :disabled="isDisabled"
      />
    </div>
  </div>

  <div id="tab-content">
    <div class="nodeInput" v-for="(node, index) in tableData.nodeList" :key="index">
      <div class="inputNo">{{ index + 1 }}:</div>

      <a-input
        placeholder="Node name"
        style="width: 200px;"
        v-model:value="node.name"
        :disabled="isDisabled"
      />

      <a-input
        placeholder="Node ID (e.g., ns=2;s=Tag1)"
        style="width: 400px; margin-left: 10px;"
        v-model:value="node.nodeId"
        :disabled="isDisabled || !isFirstNodeEditable(index)"
      />

      <a-select
        placeholder="Data type"
        style="width: 100px; margin-left: 10px;"
        v-model:value="node.dataType"
        :options="dataTypeOptions"
        :disabled="isDisabled || !isFirstNodeEditable(index)"
      />

      <a-button
        type="default"
        size="small"
        @click="removeNode(index)"
        :disabled="isDisabled || tableData.nodeList.length <= 1"
        style="margin-left: 10px; min-width: auto; padding: 0 8px;"
      >
        −
      </a-button>
    </div>

    <div class="add-node-btn">
      <a-button
        type="dashed"
        @click="addNode"
        :disabled="isDisabled || tableData.nodeList.length >= MAX_NODE_COUNT"
      >
        + Add Node (Max: {{MAX_NODE_COUNT}})
      </a-button>
    </div>
  </div>

</template>

<style scoped>
#tab-header {
  display: flex;
  align-items: center;
  justify-content: start;
}

#tab-content {
  margin: 10px;
}

.nodeInput {
  display: flex;
  place-items: center;
  margin: 5px;
}

.inputBox {
  margin: 10px;
}

.inputNo {
  margin-left: 10px;
  margin-right: 10px;
  width: 30px;
  text-align: right;

}

.add-node-btn {
  margin: 10px;
}
</style>
