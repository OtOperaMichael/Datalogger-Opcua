<script setup lang="ts">
import {ref, computed} from 'vue';
import TabCustomModule from "@/components/template/tabs/TabCustomModule.vue";
import {message} from "ant-design-vue";

const activeKeyCustomModule = ref('1');
const outerActiveKey = ref('1');
import {useNewTemplateStore} from "@/stores/useNewTemplateStore.ts";
import request from "@/utils/request";
import {useTemplateListStore} from "@/stores/useTemplateListStore.ts";
import {storeToRefs} from "pinia";
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";
import {useSystemConfigStore} from "@/stores/useSystemConfigStore.ts";

const templateStore = useNewTemplateStore();
const templateListStore = useTemplateListStore();
const {selectedTemplateIndex} = storeToRefs(useTemplateListStore());
const loginUserStore = useLoginUserStore()
const systemConfigStore = useSystemConfigStore();


// 最大表格数量
const MAX_TABLE_COUNT = computed(() => systemConfigStore.maxTableCount || 10);

// 是否启用自定义数据采集
const isCustomEnabled = computed({
  get: () => templateStore.custom.enable,
  set: (value) => {
    templateStore.custom.enable = value;
  }
});

// 计算属性：生成内部 tabs 列表
const innerPanesCustomModule = computed(() => {
  return templateStore.custom.tableList.map((_, index) => ({
    key: String(index + 1),
    title: `Group ${index + 1}`,
    index: index,
    closable: templateStore.custom.tableList.length > 1
  }));
});

// 添加新 table
const addTableCustomModule = () => {
  if (templateStore.custom.tableList.length >= MAX_TABLE_COUNT.value) {
    message.warning(`Maximum number of tables ${MAX_TABLE_COUNT.value} reached'`);
    return;
  }

  const newKey = String(templateStore.custom.tableList.length + 1);
  activeKeyCustomModule.value = newKey;
  templateStore.addTable("custom");
};

// 删除 table
const removeTableCustomModule = (targetKey: string) => {
  if (templateStore.custom.tableList.length <= 1) {
    message.warning('At least one table must exist');
    return;
  }

  const targetIndex = parseInt(targetKey) - 1;

  let lastIndex = 0;
  innerPanesCustomModule.value.forEach((pane, i) => {
    if (pane.key === targetKey) {
      lastIndex = i - 1;
    }
  });

  templateStore.removeTable("custom",targetIndex);

  if (templateStore.custom.tableList.length && activeKeyCustomModule.value === targetKey) {
    if (lastIndex >= 0) {
      activeKeyCustomModule.value = String(lastIndex + 1);
    } else {
      activeKeyCustomModule.value = '1';
    }
  } else if (targetIndex < parseInt(activeKeyCustomModule.value) - 1) {
    activeKeyCustomModule.value = String(parseInt(activeKeyCustomModule.value) - 1);
  }
};

// 处理编辑事件
const onInnerEditCustomModule = (targetKey: string | MouseEvent, action: string) => {
  if (!loginUserStore.getIsLoggedIn) {
    message.warning('Please log in first to modify the template.');
    return; // 直接返回，不执行后续操作
  }

  if (action === 'add') {
    addTableCustomModule();
  } else {
    removeTableCustomModule(targetKey as string);
  }
};

async function saveTemplate() {
  //troubleshooting
  console.log('Template ifNew: ', templateStore.createNew)

  // 1. 校验
  const validation = templateStore.validateTemplate()
  if (!validation.valid) {
    console.error('Validation failed:', validation.field, validation.message)
    //  拼接字段名 + 错误信息（加冒号更清晰）
    message.error(`${validation.field}: ${validation.message || 'Validation failed'}`, 5)
    return //  必须 return，阻止后续保存！
  }

  try {
    // 2.如果是新增，先校验 templateName 是否重复！
    if (templateStore.createNew) {
      if (templateListStore.isTemplateNameDuplicate(templateStore.name)) {
        message.error('Template name already exists');
        return; // 立即终止，不提交
      }
    }

    // 3. 发送请求
    const {data} = await request.post("template/saveTemplate", templateStore.toJSON(), {params: {ifNew: templateStore.createNew}}) // 或template.$state

    // 4. 处理响应
    if (data?.code !== 200) {
      message.error(data?.msg || 'Failed to save template', 3)
    } else {
      message.success('Template saved successfully!', 3)
      const list = data.data.templateList
      templateListStore.loadFromData(list)
      if (templateStore.createNew) {
        selectedTemplateIndex.value = templateListStore.templateList.length - 1
      }
      //置位createNew, 此时再次保存需触发重名检查
      templateStore.createNew = true
    }
  } catch
    (error) {
    // 5. 处理网络错误、超时等
    console.error('Network error:', error)
    message.error('Network error, please try again');
  }
  //troubleshooting
  console.log('Template ifNew: ', templateStore.createNew)
}

</script>

<template>
  <div id="TemplateRightFrame">

    <div id="templateGeneral">
      <div class="inputBox">
        Template name:
        <a-input
          style="width: 200px;"
          placeholder="template name"
          :disabled="!loginUserStore.getIsLoggedIn"
          v-model:value="templateStore.name"
        />
      </div>
      <div class="inputBox">
        Port:
        <a-input
          style="width: 100px;"
          placeholder="4840"
          :disabled="!loginUserStore.getIsLoggedIn"
          v-model:value="templateStore.port"
        />
      </div>
      <div class="inputBox">
        Postfix:
        <a-input
          style="width: 150px;"
          placeholder="Optional suffix"
          :disabled="!loginUserStore.getIsLoggedIn"
          v-model:value="templateStore.postfix"
        />
      </div>
    </div>

    <div id="templateTabs">
      <a-card>
        <a-tabs v-model:activeKey="outerActiveKey" tab-position="left" type="card">
          <!--module 1: custom data collecting-->
          <a-tab-pane key="1">
            <template #tab>
              <span>
                <a-checkbox
                  :disabled="!loginUserStore.getIsLoggedIn"
                  v-model:checked="isCustomEnabled"
                  @click.stop                  style="margin-right: 8px;"
                />
                Custom
              </span>
            </template>
            <a-tabs
              v-model:activeKey="activeKeyCustomModule"
              type="editable-card"
              @edit="onInnerEditCustomModule"
            >
              <a-tab-pane
                v-for="pane in innerPanesCustomModule"
                :key="pane.key"
                :tab="pane.title"
                :closable="pane.closable"
              >
                <TabCustomModule :table-index="pane.index"/>
              </a-tab-pane>
            </a-tabs>
          </a-tab-pane>

          <!--module 2: alarm collecting-->
          <a-tab-pane key="2" tab="Alarm">

          </a-tab-pane>

          <!--module 3: communication logger-->
          <a-tab-pane key="3" tab="Comm">

          </a-tab-pane>


        </a-tabs>
      </a-card>
    </div>

    <div id="templateBottom">
      <a-button type="primary"
                v-show="loginUserStore.getIsLoggedIn"
                @click="saveTemplate()"
      >Save Template
      </a-button>
    </div>

  </div>
</template>

<style scoped>
#TemplateRightFrame {
  background-color: lightgrey;
  padding: 10px;
  border-radius: 5px;
  align-items: center;
  margin: 10px;
}

#templateGeneral {
  display: flex;
  align-items: center;
  justify-content: start;
}

#templateTabs {
  margin: 10px;
}

#templateBottom {
  margin-right: 50px;
  margin-bottom: 10px;
  display: flex;
  justify-content: right;
}

.inputBox {
  margin: 10px;
}
</style>



