<template>
  <div class="layout">
    <aside class="sider">
      <div class="brand">
        <div class="brand-mark">管</div>
        <div>
          <b>管理后台</b>
          <span>课程 · 题库 · 资料</span>
        </div>
      </div>
      <el-menu router :default-active="active">
        <el-menu-item index="/admin"><el-icon><DataAnalysis /></el-icon><span>数据概览</span></el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/admin/courses"><el-icon><Collection /></el-icon><span>课程管理</span></el-menu-item>
        <el-menu-item index="/admin/relations"><el-icon><Share /></el-icon><span>知识关系</span></el-menu-item>
        <el-menu-item index="/admin/documents"><el-icon><Document /></el-icon><span>资料管理</span></el-menu-item>
        <el-menu-item index="/admin/questions"><el-icon><EditPen /></el-icon><span>题库管理</span></el-menu-item>
      </el-menu>
    </aside>
    <section class="main">
      <nav class="mobile-nav">
        <router-link to="/admin">概览</router-link>
        <router-link to="/admin/users">用户</router-link>
        <router-link to="/admin/courses">课程</router-link>
        <router-link to="/admin/relations">关系</router-link>
        <router-link to="/admin/documents">资料</router-link>
        <router-link to="/admin/questions">题库</router-link>
      </nav>
      <header class="topbar">
        <div class="user-chip">
          <span class="avatar-dot">{{ store.profile?.username?.slice(0, 1) }}</span>
          <span>{{ store.profile?.username }}</span>
        </div>
        <el-tag effect="plain">管理员</el-tag>
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
