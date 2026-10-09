<template>
  <div class="page" v-loading="loading">
    <div class="welcome">
      <div>
        <p class="eyebrow">管理后台</p>
        <h1>数据概览</h1>
        <p>用户、课程、知识点和学习数据。</p>
      </div>
    </div>
    <div class="stat-grid">
      <div class="stat-card"><span>用户</span><strong>{{ stats?.userCount || 0 }}</strong></div>
      <div class="stat-card"><span>课程</span><strong>{{ stats?.courseCount || 0 }}</strong></div>
      <div class="stat-card"><span>知识点</span><strong>{{ stats?.knowledgeCount || 0 }}</strong></div>
      <div class="stat-card"><span>题目</span><strong>{{ stats?.questionCount || 0 }}</strong></div>
      <div class="stat-card"><span>资料</span><strong>{{ stats?.documentCount || 0 }}</strong></div>
      <div class="stat-card"><span>学习分钟</span><strong>{{ stats?.studyMinutes || 0 }}</strong></div>
      <div class="stat-card"><span>答题次数</span><strong>{{ stats?.answerCount || 0 }}</strong></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'

const loading = ref(false)
const stats = ref<any>(null)
onMounted(async () => {
  loading.value = true
  try {
    stats.value = await http.get('/admin/stats')
  } finally {
    loading.value = false
  }
})
</script>
