<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>个人中心</h1>
        <p>{{ profile?.username }} · {{ profile?.role === 'ADMIN' ? '管理员' : '学生' }}</p>
      </div>
    </div>
    <div class="panel" style="max-width: 520px">
      <el-form label-position="top">
        <el-form-item label="邮箱">
          <el-input v-model="email" />
        </el-form-item>
        <el-button type="primary" @click="saveEmail">保存资料</el-button>
      </el-form>
      <el-divider />
      <el-form label-position="top">
        <el-form-item label="头像">
          <input type="file" accept="image/*" @change="uploadAvatar" />
        </el-form-item>
        <img v-if="avatarUrl" :src="avatarUrl" alt="头像" style="width: 72px; height: 72px; border-radius: 50%; object-fit: cover" />
      </el-form>
      <el-divider />
      <el-form label-position="top">
        <el-form-item label="原密码"><el-input v-model="password.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="password.newPassword" type="password" show-password /></el-form-item>
        <el-button @click="savePassword">修改密码</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import type { Profile } from '../../stores/user'

const profile = ref<Profile | null>(null)
const email = ref('')
const avatarUrl = ref('')
const password = reactive({ oldPassword: '', newPassword: '' })

async function loadAvatar() {
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get('/api/profile/avatar', {
      responseType: 'blob',
      headers: { Authorization: `Bearer ${token}` }
    })
    avatarUrl.value = URL.createObjectURL(response.data)
  } catch {
    avatarUrl.value = ''
  }
}

async function load() {
  profile.value = await http.get('/profile')
  email.value = profile.value?.email || ''
  loadAvatar()
}

async function saveEmail() {
  await http.put('/profile', { email: email.value })
  ElMessage.success('已保存')
}

async function savePassword() {
  await http.put('/profile/password', password)
  ElMessage.success('密码已更新')
  password.oldPassword = ''
  password.newPassword = ''
}

async function uploadAvatar(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const data = new FormData()
  data.append('file', file)
  await http.post('/profile/avatar', data)
  ElMessage.success('头像已更新')
  loadAvatar()
}

onMounted(load)
</script>
