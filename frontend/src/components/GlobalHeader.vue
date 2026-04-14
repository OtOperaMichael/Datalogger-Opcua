<template>
  <a-row class="global-header">
    <a-col flex="300px">
      <div class="title-bar">
        <img class="logo" src="../assets/logo.jpg" alt="logo"/>
        <!-- 修改标题样式 -->
        <div class="title title-enhanced">Datalogger</div>
      </div>
    </a-col>
    <a-col flex="auto">
      <a-menu
        v-model:selectedKeys="current"
        mode="horizontal"
        :items="items"
        @click="doMenuClick"
      />
    </a-col>
    <a-col flex="300px">
      <div class="user-login-status">
        <a-space>
          <a-button @click="login">
            <UserOutlined v-if="loginUserStore.isLoggedIn" style="margin-right: 4px;"/>

            {{ loginUserStore.isLoggedIn ? 'Logout' : 'Login' }}
          </a-button>
          <a-button
            danger
            v-show="loginUserStore.getIsLoggedIn"
            @click="reInit()"
          >
            Reinitialize
          </a-button>
        </a-space>
      </div>
    </a-col>
  </a-row>
</template>


<script lang="ts" setup>
import {createVNode, h, reactive, ref} from 'vue';
import {type MenuProps, message, Modal} from 'ant-design-vue';
import {useRouter} from "vue-router";
import request from "@/utils/request.ts";
import {UserOutlined} from '@ant-design/icons-vue';
import {useLoginUserStore} from "@/stores/useLoginUserStore.ts";
import {useSystemConfigStore} from "@/stores/useSystemConfigStore.ts";

const router = useRouter();
const loginUserStore = useLoginUserStore();
const systemConfigStore = useSystemConfigStore();

// 管理员信息
const admin = reactive({username: 'admin', password: 'admin'})

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginFormRules = {
  username: [
    {required: true, message: 'Please input username', trigger: 'blur'}
  ],
  password: [
    {required: true, message: 'Please input password', trigger: 'blur'}
  ]
}

// 点击菜单后的路由跳转事件
const doMenuClick = ({key}: { key: string }) => {
  router.push({
    path: key,
  });
};

const current = ref<string[]>(['mail']);
router.afterEach((to, from, failure) => {
  current.value = [to.path]
})

const items = ref<MenuProps['items']>([
  {
    key: '/home',
    label: 'Home',
    title: 'Home',
  },
  {
    key: '/template',
    label: 'Template',
    title: 'Template',
  },
  {
    key: '/server',
    label: 'Server',
    title: 'Server',
  }
]);

async function reInit() {
  Modal.confirm({
    title: '确认重置？',
    content: '此操作将删除所有数据库配置（MySQL 和 InfluxDB），并返回初始设置页面。是否继续？',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const {data} = await request.post('common/resetConfig'); // 注意：建议用 POST
        if (data?.code === 200) {
          message.success('配置已重置，正在跳转...');
          // 延迟 1 秒跳转，确保 message 能被看到
          setTimeout(() => {
            router.push('/setup');
          }, 1000);
        } else {
          message.error('重置失败: ' + (data?.message || '未知错误'));
        }
      } catch (error: any) {
        message.error('请求失败: ' + (error.response?.data?.message || error.message));
      }
    },
  });
}

async function login() {
  // 如果已经登录，先退出
  if (loginUserStore.isLoggedIn) {
    Modal.confirm({
      title: '确认退出登录？',
      content: '确定要退出当前用户吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        loginUserStore.logout();
        message.success('已退出登录');
      }
    });
    return;
  }

  // 未登录，显示登录表单
  Modal.confirm({
    title: '用户登录',
    content: () => h('div', {style: {marginTop: '20px'}}, [
      h('div', {style: {marginBottom: '10px', display: 'flex', alignItems: 'center'}}, [
        h('span', {
          style: {
            width: '70px',
            display: 'inline-block',
            textAlign: 'center',
            marginRight: '10px'
          }
        }, '用户名：'),
        h('input', {
          type: 'text',
          value: loginForm.username,
          onInput: (e: Event) => {
            loginForm.username = (e.target as HTMLInputElement).value;
          },
          style: {
            padding: '4px 8px',
            border: '1px solid #d9d9d9',
            borderRadius: '4px',
            width: '200px'
          }
        })
      ]),
      h('div', {style: {marginBottom: '10px', display: 'flex', alignItems: 'center'}}, [
        h('span', {
          style: {
            width: '70px',
            display: 'inline-block',
            textAlign: 'center',
            marginRight: '10px'
          }
        }, '密   码：'),
        h('input', {
          type: 'password',
          value: loginForm.password,
          onInput: (e: Event) => {
            loginForm.password = (e.target as HTMLInputElement).value;
          },
          style: {
            padding: '4px 8px',
            border: '1px solid #d9d9d9',
            borderRadius: '4px',
            width: '200px'
          }
        })
      ])
    ]),
    okText: '登录',
    cancelText: '取消',
    onOk: async () => {
      // 验证用户名和密码
      if (loginForm.username === admin.username && loginForm.password === systemConfigStore.adminPassword) {
        loginUserStore.setLoginUser(loginForm.username);
        message.success('登录成功！');
        // 清空表单
        loginForm.username = '';
        loginForm.password = '';
      } else {
        message.error('用户名或密码不正确');
        // 清空密码框
        loginForm.password = '';
      }
    },
    onCancel: () => {
      // 取消时清空表单
      loginForm.username = '';
      loginForm.password = '';
    }
  });

}

</script>

<style scoped>
.title-bar {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 20px;
}

.logo {
  width: 40px;
  height: 40px;
  margin-right: 16px;
  border-radius: 4px;
}

/* 增强版标题样式 */
.title-enhanced {
  font-family: 'Segoe UI', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  font-weight: 700;
  font-size: 24px;
  color: #1890ff; /* Ant Design 主色 */
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #1890ff, #40a9ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  position: relative;
}


</style>
