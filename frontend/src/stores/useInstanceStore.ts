import {defineStore} from 'pinia'

export const useInstanceStore = defineStore('instanceStore', {
  state: () => ({
    logList: Array(1).fill("")
  }),
  getters: {
    getLogs: (state) => state.logList
  },
  actions: {
    loadLogs: function (logs: string[]) {
      this.logList = logs
    }
  }
})
