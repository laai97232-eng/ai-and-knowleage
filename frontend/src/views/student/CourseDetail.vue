<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>{{ course?.name }}</h1>
        <p>{{ course?.description }}</p>
      </div>
      <div>
        <el-button @click="$router.push(`/graph?courseId=${route.params.id}`)">知识图谱</el-button>
        <el-button @click="$router.push(`/insights?courseId=${route.params.id}`)">学习分析</el-button>
        <el-button @click="$router.push('/courses')">返回</el-button>
      </div>
    </div>
    <el-progress :percentage="course?.progress || 0" :stroke-width="12" style="margin-bottom: 16px" />
    <el-collapse v-model="open">
      <el-collapse-item v-for="chapter in course?.chapters || []" :key="chapter.id" :name="chapter.id" :title="chapter.title">
        <div class="point-row" v-for="point in chapter.points" :key="point.id">
          <div>
            <b>{{ point.name }}</b>
            <div class="muted">{{ statusText(point.status) }} · {{ point.questionCount }} 题 · {{ point.documentCount }} 份资料</div>
          </div>
          <div style="display: flex; gap: 8px; align-items: center">
            <el-progress :percentage="point.progress" style="width: 120px" />
            <el-button @click="$router.push(`/knowledge/${point.id}`)">查看</el-button>
            <el-button type="primary" @click="$router.push(`/practice/${point.id}`)">练习</el-button>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'

const route = useRoute()
const loading = ref(false)
const course = ref<any>(null)
const open = ref<number[]>([])

function statusText(status: string) {
  if (status === 'MASTERED') return '已掌握'
  if (status === 'LEARNING') return '学习中'
  return '未学习'
}

onMounted(async () => {
  loading.value = true
  try {
    course.value = await http.get(`/courses/${route.params.id}`)
    open.value = (course.value.chapters || []).map((item: any) => item.id)
  } finally {
    loading.value = false
  }
})
</script>
