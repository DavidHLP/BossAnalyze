import { defineStore } from 'pinia'
export const useTokenStore = defineStore('token', {
  state: () => ({
    token: null as string | null,
  }),
  actions: {
    setToken(token: string) {
      localStorage.setItem('token', token)
      localStorage.setItem('token_timestamp', Date.now().toString())
      this.token = token
    },
    getToken() {
      return this.token
    },
    removeToken() {
      localStorage.removeItem('token')
      localStorage.removeItem('token_timestamp')
      this.token = null
    },
  },
})
