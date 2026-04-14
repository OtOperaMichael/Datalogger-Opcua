import {defineStore} from 'pinia'
import request from "@/utils/request.ts";
import {type SystemConfigInterface} from "@/types/systemConfig.ts";

export const useSystemConfigStore = defineStore('systemConfigStore', {
  state: () => ({

    adminPassword: "",
    maxServerCount: 0,
    maxTableCount: 0,
    maxTableFieldCount: 0

  }),
  getters: {},
  actions: {

    loadConfig: function (systemConfig: SystemConfigInterface) {
      this.adminPassword = systemConfig.adminPassword
      this.maxServerCount = systemConfig.maxServerCount
      this.maxTableCount = systemConfig.maxTableCount
      this.maxTableFieldCount = systemConfig.maxTableFieldCount
    },

    // 异步加载系统配置（只执行一次）
    async  loadSystemConfig() {
      try {
        let {data} = await request.get('common/getSystemConfig');
        if (data.code === 200) {
          this.loadConfig(data.data.systemConfig);
        }
      } catch (error) {
        console.error('Failed to load system config:', error);
      }
    }

  }
})
