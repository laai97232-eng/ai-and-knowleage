import { defineStore } from 'pinia'
import http from '../api/http'

export interface Profile {
  id: number
  username: string
  email?: string
  avatar?: string
  role: 'STUDENT' | 'ADMIN'
  status: number
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    profile: null as Profile | null
  }),
  actions: {
    async login(username: string, password: string) {
      const data = await http.post<{ token: string; user: Profile }>('/auth/login', { username, password })
      this.token = data.token
      this.profile = data.user
      localStorage.setItem('token', data.token)
    },
    async fetchMe() {
      this.profile = await http.get<Profile>('/profile')
    },
    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem('token')
    }
  }
})
