import {defineStore} from 'pinia'

export const useLoginUserStore = defineStore('loginUserStore', {
  state: () => ({
    loginUser: "",
    isLoggedIn: false
  }),
  getters: {
    getCurrentUser: (state) => state.loginUser,
    getIsLoggedIn: (state) => state.isLoggedIn
  },
  actions: {
    setLoginUser(username: string) {
      this.loginUser = username
      this.isLoggedIn = true
    },
    logout() {
      this.loginUser = ""
      this.isLoggedIn = false
    }
  }
})
