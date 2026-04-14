import {createApp} from 'vue'
import {createPinia} from 'pinia'

import App from './App.vue'
import router from './router'

import Antd from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';

import "./assets/global.css"// ← 引入全局样式

import {useTemplateListStore} from "@/stores/useTemplateListStore";
import {useServerListStore} from "@/stores/useServerListStore";
import {useSystemConfigStore} from "@/stores/useSystemConfigStore.ts";




const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

//每次应用启动，从后端获取信息。避免在组件中重复获取数据
const templateList = useTemplateListStore()
const serverList = useServerListStore()
const systemConfig = useSystemConfigStore()
templateList.getAllTemplates()
serverList.getAllServers()
systemConfig.loadSystemConfig();

app.mount('#app')
