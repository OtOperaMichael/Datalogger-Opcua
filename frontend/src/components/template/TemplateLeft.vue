<script setup lang="ts">
import {useNewTemplateStore} from "@/stores/useNewTemplateStore.ts";
import {useTemplateListStore} from "@/stores/useTemplateListStore.ts";
import {storeToRefs} from "pinia";
import request from "@/utils/request.ts";
import {message, Modal} from "ant-design-vue";
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";

const templateStore = useNewTemplateStore();

const {templateList, selectedTemplateIndex} = storeToRefs(useTemplateListStore());
const loginUserStore = useLoginUserStore()



// // 当前选中的模板索引
// const selectedTemplateIndex = ref(0);

function selectTemplate(index: number) {
  // 边界检查
  if (index < 0 || index >= templateList.value.length) return;

  selectedTemplateIndex.value = index;

  const templateData = templateList.value[index];
  if (!templateData) return;

  templateStore.loadTemplate(templateData);
  //赋值createNew
  templateStore.createNew = false;
  //troubleshooting
  console.log('Template ifNew: ', templateStore.createNew)
  console.log('Template ', templateStore.$state)

}

function newTemplate() {
  //初始化template
  templateStore.$reset()
  //高亮index赋值
  selectedTemplateIndex.value = templateList.value.length + 1;
  //赋值createNew
  templateStore.createNew = true;
}

// 删除server前确认
const showDeleteConfirm = (templateId: string, templateName: string) => {
  Modal.confirm({
    title: 'Confirm Deletion',
    content: `Are you sure you want to delete the template "${templateName}"?`,
    okText: 'Yes, Delete',
    okType: 'danger',
    cancelText: 'Cancel',
    onOk() {
      // 执行删除操作
      deleteTemplate(templateId);
    },
    onCancel() {
      console.log('Delete canceled');
    }
  });
};

//删除template
async function deleteTemplate(id: string){
  try {
    // 请求后端获取所有服务器信息
    let {data} = await request.get("template/deleteTemplate",{params:{id:id}})
    console.log("data:", data)
    console.log('templateList:', data.data.templateList)
    templateList.value = data.data.templateList

    //troubleshooting
    console.log('Template ifNew: ', templateStore.createNew)
    console.log('Template now: ', templateStore.toJSON())

    //置位createNew, 此时再次保存需触发重名检查
    templateStore.createNew = true
    //清空id, 再保存不携带id发送请求
    templateStore.id = ''

  } catch (err: any) {
    message.error('Network error, please try again');
  }
}

</script>

<template>
  <div id="TemplateLeftFrame">
    <div id="instanceContainer">
      <div
        v-for="(template, index) in templateList"
        :key="template.id || index"
        class="instance"
        :class="{ selected: selectedTemplateIndex === index }"
        @click="selectTemplate(index)">
        <div class="templateName">{{
            templateList[index]?.name || 'undefined template'
          }}
        </div>
        <div class="buttonContainer">
          <a-button
            v-show="(selectedTemplateIndex === index) && loginUserStore.getIsLoggedIn"
            class="templateEdit"
            type="primary" danger
            @click="showDeleteConfirm(template.id, template.name)"
          >Delete
          </a-button>
        </div>
      </div>
    </div>

    <div id="bottomContainer">
      <div
        v-show="loginUserStore.getIsLoggedIn"
        @click="newTemplate()"
        class="newInstance">
        +
      </div>
    </div>
  </div>

</template>

<style scoped>
#TemplateLeftFrame {
  background-color: lightgrey;
  padding: 10px;
  border-radius: 5px;
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

.newInstance {
  background-color: rgba(255, 255, 255, 0.5); /* 白色 + 70% 不透明度 */
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

/* 点击时变深色 */
.newInstance:active {
  background-color: gray;
}

.buttonContainer {

}

#instanceContainer {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  margin: 10px;
}

#bottomContainer {
  display: flex;
  justify-content: center;
  margin: 10px;
}

.templateEdit {
  width: 80px;
  margin: 2px;
}

.templateName {
  font-family: 'Segoe UI', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 1px;
  margin-bottom: 10px;
}
</style>

