<script setup lang="ts">
import {storeToRefs} from "pinia";
import {useServerListStore} from "@/stores/useServerListStore.ts";
import {ref} from "vue";
import {useSelectedServerStore} from "@/stores/useSelectedServerStore.ts";

const serverStore = useSelectedServerStore();
const {serverList} = storeToRefs(useServerListStore())

// 当前选中的server索引
const selectedServerIndex = ref(0);

function selectServer(index: number) {
  // 边界检查
  if (index < 0 || index >= serverList.value.length) return;
  selectedServerIndex.value = index;
  //赋值selected server
  const templateData = serverList.value[index];
  if (!templateData) return;

  serverStore.loadServer(templateData);
}

</script>

<template>
  <div id="HomeLeftFrame">
    <div id="instanceContainer">

      <div
        v-for="(server, index) in serverList"
        :key="index"
        class="instance"
        :class="{ selected: selectedServerIndex === index }"
        @click="selectServer(index)">
        <div class="lineInfo-name">{{ server.name }}</div>
        <div class="lineInfo">{{ server.templateName }}</div>
      </div>

    </div>
  </div>
</template>

<style scoped>
#HomeLeftFrame {
  background-color: lightgrey;
  padding: 10px;
  border-radius: 5px;
  align-items: center;
  margin: 10px;

}

#instanceContainer {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin: 10px;
}


.instance {
  background-color: white;
  padding: 10px;
  border-radius: 5px;
  height: 100px;
  width: 200px;
  margin: 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-evenly; /* 垂直居中 */
  align-items: center; /* 水平居中 */
}

.instance.selected {
  background-color: #88bfda; /* Ant Design 的 info 背景色 */
  padding: 10px;
  border-radius: 5px;
}

.lineInfo-name {
  font-family: 'Segoe UI', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 1px;

}
</style>
