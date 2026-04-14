import {createRouter, createWebHistory} from 'vue-router'
import HomePage from "@/pages/HomePage.vue";
import ServerPage from "@/pages/ServerPage.vue";
import TemplatePage from "@/pages/TemplatePage.vue";
import SetupPage from "@/pages/SetupPage.vue";
import request from "@/utils/request.ts";



// 👇 全局状态：只检查一次
let configChecked = false;
let configReady = false;

// 异步检查配置（只执行一次）
async function checkConfigOnce() {
  if (configChecked) return configReady; // 已检查过，直接返回结果

  try {
    let {data} = await request.get('common/getConfigStatus');
    configReady = data.code === 200;
  } catch (error) {
    console.error('Failed to check config status:', error);
    configReady = false;
  } finally {
    configChecked = true;
  }
  return configReady;
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/setup',
      name: 'setup',
      component: SetupPage,
      meta: {isSetup: true} // ← 补上这行
    },
    {
      path: '/',
      redirect: '/home',
    },
    {
      path: '/home',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/server',
      name: 'server',
      component: ServerPage,
    },
    {
      path: '/template',
      name: 'template',
      component: TemplatePage,
    }
  ],
})

// 全局前置守卫：只在必要时触发检查
router.beforeEach(async (to, from, next) => {
  // 如果目标是 /setup，直接放行
  if (to.path === '/setup') {
    return next();
  }

  // 检查配置状态（只会在第一次触发）
  const ready = await checkConfigOnce();

  if (ready) {
    next(); // 配置就绪，允许访问

  } else {
    next('/setup'); // 未就绪，跳转到 setup
  }
})

export default router
