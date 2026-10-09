<template>
  <div class="page" v-loading="loading">
    <div class="welcome">
      <div>
        <p class="eyebrow">今日学习</p>
        <h1>你好，{{ store.profile?.username }}</h1>
        <p>从一门课程开始。学习时长、做题正确率和错题会汇总在这里。</p>
      </div>
      <div>
        <el-button size="large" @click="$router.push('/insights')">学习分析</el-button>
        <div>
        <el-button size="large" @click="$router.push('/insights')">学习分析</el-button>
        <el-button type="primary" size="large" @click="$router.push('/courses')">去学习</el-button>
      </div>
      </div>
    </div>
    <div class="stat-grid">
      <div class="stat-card"><span>今日学习</span><strong>{{ stats?.todayMinutes || 0 }}</strong><span>分钟</span></div>
      <div class="stat-card"><span>本周学习</span><strong>{{ stats?.weekMinutes || 0 }}</strong><span>分钟</span></div>
      <div class="stat-card"><span>本月学习</span><strong>{{ stats?.monthMinutes || 0 }}</strong><span>分钟</span></div>
      <div class="stat-card"><span>错题</span><strong>{{ stats?.wrongCount || 0 }}</strong><span>道</span></div>
    </div>
    <div class="panel" style="margin-top: 16px">
      <h3>课程进度</h3>
      <el-empty v-if="!stats?.courses?.length" description="还没有学习记录，先进入一门课程" />
      <div v-for="item in stats?.courses || []" :key="item.courseId" style="margin: 12px 0">
        <div style="display: flex; justify-content: space-between">
          <span>{{ item.courseName }}</span>
          <span class="muted">已掌握 {{ item.mastered }}/{{ item.total }}</span>
        </div>
        <el-progress :percentage="item.progress" :stroke-width="10" />
      </div>
    </div>
    <div class="panel" style="margin-top: 16px">
      <h3>知识点掌握</h3>
      <el-table v-if="stats?.mastery?.length" :data="stats.mastery" size="small">
        <el-table-column prop="courseName" label="课程" width="160" />
        <el-table-column prop="name" label="知识点" />
        <el-table-column prop="answered" label="作答次数" width="100" />
        <el-table-column label="正确率" width="160">
          <template #default="{ row }">{{ row.rate }}%</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="做题之后会显示正确率" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const stats = ref<any>(null)

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await http.get('/profile/stats')
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
})
</script>
