<template>
  <div class="auth-page">
    <section class="auth-side">
      <p class="eyebrow" style="color: #f6e2c4">JOIN</p>
      <h1>创建学生账号</h1>
      <p>注册后可以选课、做题、记录学习时长，并查看自己的错题和进度。</p>
    </section>
    <section class="auth-form">
      <div class="auth-card">
        <h2>注册</h2>
        <el-form :model="form" label-position="top">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="3 到 20 位" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
          </el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="submit">注册</el-button>
        </el-form>
        <p class="muted">已有账号？<router-link to="/login">返回登录</router-link></p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', email: '', password: '' })

async function submit() {
  loading.value = true
  try {
    await http.post('/auth/register', form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>
