import {defineStore} from 'pinia'
import type {ServerInterface} from "@/types/server.ts";
import request from "@/utils/request.ts";
import {message} from "ant-design-vue";

export const useServerListStore = defineStore("serverListStore", {
  state: () => ({
    serverList: [
      {
        id: "1undefined line",
        name: "undefined line",
        templateName: "undefined template",
        hostIP: "199.199.199.199",
        running: false
      },
      {
        id: "2fake line",
        name: "fake line",
        templateName: "fake template",
        hostIP: "199.199.199.198",
        running: false
      }
    ] //  类型断言确保类型安全
  }),

  actions: {
    //从后端数据加载
    loadFromData(data: ServerInterface[]) {
      this.serverList = data
    },
    // 新增：校验 instanceNo 是否重复（只校验，不修改状态）
    isInstanceNameDuplicate(name: string): boolean {
      if (!name) return false; // 或 true？根据业务逻辑，通常空值不算重复
      return this.serverList.some(server => server.name === name);
    },
    async getAllServers() {
      try {
        // 请求后端获取所有服务器信息
        let {data} = await request.get("server/getAllServers")
        console.log('app loaded, loading serverList from backend:', data.data.serverList)
        this.serverList = data.data.serverList
      } catch (err: any) {
        message.error('Network error, please try again');
      }
    }
  }
});

