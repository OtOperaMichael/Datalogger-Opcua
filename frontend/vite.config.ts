import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
// 只在开发环境导入
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // 只在开发环境使用 devtools 插件
    process.env.NODE_ENV === 'development' ? vueDevTools() : null,
  ].filter(Boolean), // 过滤掉 null，避免在生产环境中产生空数组项
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },

  server: {
    port: 3000, // 前端服务端口
    proxy: {
      // 1. 这里的 '/api' 是请求时的前缀，例如 axios.get('/api/user')
      //    只有以这个开头的请求才会被代理
      '/api': {
        target: 'http://localhost:8080', // 2. 你的后端真实地址
        changeOrigin: true,              // 3. 必须：修改请求头的 origin 为目标地址
        rewrite: (path) => path.replace(/^\/api/, '') // 4. 可选：去掉 /api 前缀
      }
    }
  }


})
