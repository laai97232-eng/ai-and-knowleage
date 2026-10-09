<template>
  <div class="layout">
    <aside class="sider">
      <div class="brand">
        <div class="brand-mark">学</div>
        <div>
          <b>智能学习助手</b>
          <span>图谱 · 路径 · AI 问答</span>
        </div>
      </div>
      <el-menu router :default-active="active">
        <el-menu-item index="/"><el-icon><House /></el-icon><span>学习首页</span></el-menu-item>
        <el-menu-item index="/courses"><el-icon><Reading /></el-icon><span>课程学习</span></el-menu-item>
        <el-menu-item index="/chat"><el-icon><ChatDotRound /></el-icon><span>AI 问答</span></el-menu-item>
        <el-menu-item index="/graph"><el-icon><Share /></el-icon><span>知识图谱</span></el-menu-item>
        <el-menu-item index="/insights"><el-icon><DataAnalysis /></el-icon><span>学习分析</span></el-menu-item>
        <el-menu-item index="/plans"><el-icon><Calendar /></el-icon><span>学习计划</span></el-menu-item>
        <el-menu-item index="/records"><el-icon><Notebook /></el-icon><span>学习记录</span></el-menu-item>
        <el-menu-item index="/wrong"><el-icon><Warning /></el-icon><span>错题本</span></el-menu-item>
        <el-menu-item index="/profile"><el-icon><User /></el-icon><span>个人中心</span></el-menu-item>
      </el-menu>
    </aside>
    <section class="main">
      <nav class="mobile-nav">
        <router-link to="/">首页</router-link>
        <router-link to="/courses">课程</router-link>
        <router-link to="/chat">问答</router-link>
        <router-link to="/graph">图谱</router-link>
        <router-link to="/insights">分析</router-link>
        <router-link to="/plans">计划</router-link>
        <router-link to="/records">记录</router-link>
        <router-link to="/wrong">错题</router-link>
        <router-link to="/profile">我的</router-link>
      </nav>
      <header class="topbar">
        <div class="user-chip">
          <span class="avatar-dot">{{ store.profile?.username?.slice(0, 1) }}</span>
          <span>{{ store.profile?.username }}</span>
        </div>
        <el-tag type="success" effect="plain">学生</el-tag>
        <el-button link type="primary" @click="logout">退出</el-button>
      </header>
      <div class="content">
        <router-view />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const active = computed(() => (route.meta.active as string) || route.path)

function logout() {
  store.logout()
  router.push('/login')
}
</script>
