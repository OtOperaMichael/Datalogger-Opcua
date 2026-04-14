import {defineStore} from 'pinia'
import type {ServerInterface} from "@/types/server.ts";



export const useSelectedServerStore = defineStore("selectedServerStore", {
  state: () => ({
    id: "undefined id",
    name: "undefined line",
    templateName: "undefined template",
    hostIP: "199.199.199.199",
    running: true
  }),

  actions: {
    /**
     *用serverList中选中的赋值
     * @param data
     */
    loadServer(data: ServerInterface) {
      this.id = data.id;
      this.name = data.name;
      this.templateName = data.templateName;
      this.hostIP = data.hostIP;
      this.running = data.running;
    }
  }
});

