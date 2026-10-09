<template>
  <div class="auth-page">
    <section class="auth-side">
      <p class="eyebrow" style="color: #f6e2c4">PHASE 01</p>
      <h1>智能学习助手</h1>
      <p>先把学习平台本身做完整：课程、知识点、资料、题库、错题和学习记录都在这里。</p>
      <div class="auth-pills">
        <span>课程学习</span>
        <span>AI 问答</span>
        <span>题库练习</span>
        <span>错题本</span>
        <span>学习计划</span>
      </div>
    </section>
    <section class="auth-form">
      <div class="auth-card">
        <h2>登录</h2>
        <el-form :model="form" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password />
          </el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="submit">登录</el-button>
        </el-form>
        <div style="margin-top: 14px; display: flex; gap: 8px">
          <el-button @click="fill('student', '123456')">学生演示</el-button>
          <el-button @click="fill('admin', '123456')">管理员演示</el-button>
        </div>
        <p class="muted">还没有账号？<router-link to="/register">注册学生账号</router-link></p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'student', password: '123456' })

function fill(username: string, password: string) {
  form.username = username
  form.password = password
}

async function submit() {
  loading.value = true
  try {
    await store.login(form.username, form.password)
    router.push(store.profile?.role === 'ADMIN' ? '/admin' : '/')
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>
