NEW_FILE_CODE
<script setup lang="ts">
import {computed} from 'vue';
import {useNewTemplateStore} from "@/stores/useNewTemplateStore.ts";
import {type TableInterface, TagType} from "@/types/template.ts";
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";
import {useSystemConfigStore} from "@/stores/useSystemConfigStore.ts";
const loginUserStore = useLoginUserStore()
const systemConfigStore = useSystemConfigStore();


const props = defineProps<{
  tableIndex: number
}>();

// 最大字段数量
const MAX_TABLE_FIELD_COUNT = computed(() => systemConfigStore.maxTableFieldCount || 10);

const template = useNewTemplateStore();

// 计算属性：获取当前 table 的数据
const tableData = computed<TableInterface>(() => {
  return template.tableList[props.tableIndex] || {
    name: "",
    tagAddr: "",
    tagType: TagType.BOOL,
    tagNameList: Array(16).fill("")
  };
});

const tagOptions = [
  {label: 'BOOL', value: TagType.BOOL},
  {label: 'Int', value: TagType.Int},
  {label: 'Dint', value: TagType.Dint},
  {label: 'Real', value: TagType.Real},
]

function addTagName() {
  template.addTagName(props.tableIndex);
}

function removeTagName(index: number) {
  template.removeTagName(props.tableIndex, index);
}
</script>

<template>
  <div id="tab-header">
    <div class="inputBox">
      Table name:
      <a-input
        style="width: 200px;"
        v-model:value="tableData.name"
        :disabled="!loginUserStore.getIsLoggedIn"
      />
    </div>
    <div class="inputBox">
      Tag addr:
      <a-input
        style="width: 200px;"
        v-model:value="tableData.tagAddr"
        :disabled="!loginUserStore.getIsLoggedIn"
      />
    </div>
    <div class="inputBox">
      Tag type:
      <a-select
        style="width: 100px;"
        v-model:value="tableData.tagType"
        :options="tagOptions"
        :disabled="!loginUserStore.getIsLoggedIn"
      />
    </div>
  </div>

  <div id="tab-content">
    <div class="addrInput" v-for="(tag, index) in tableData.tagNameList" :key="index">
      <div class="inputNo">{{ index + 1 }}:</div>
      <a-input
        style="width: 300px;"
        v-model:value="tableData.tagNameList[index]"
        :disabled="!loginUserStore.getIsLoggedIn"
      />
      <a-button
        type="default"
        size="small"
        @click="removeTagName(index)"
        :disabled="!loginUserStore.getIsLoggedIn || tableData.tagNameList.length <= 1"
        style="margin-left: 10px; min-width: auto; padding: 0 8px;"
      >
        −
      </a-button>
    </div>

    <div class="add-tag-btn">
      <a-button
        type="dashed"
        @click="addTagName"
        :disabled="!loginUserStore.getIsLoggedIn || tableData.tagNameList.length >= MAX_TABLE_FIELD_COUNT"
      >
        + Add Tag (Max: {{MAX_TABLE_FIELD_COUNT}})
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

.addrInput {
  display: flex;
  place-items: center;
}

.inputBox {
  margin: 10px;
}

.inputNo {
  margin-left: 10px;
  margin-right: 10px;
  width: 20px;
}

.addrInput {
  margin: 5px;
}

.add-tag-btn {
  margin: 10px;
}
</style>
