<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>课程学习</h1>
        <p>课程下面是章节和知识点，知识点再关联资料和练习题。</p>
      </div>
      <el-input v-model="keyword" placeholder="搜索课程" style="width: 220px" clearable @keyup.enter="load" @clear="load" />
    </div>
    <div class="card-grid" v-loading="loading">
      <article class="course-card" :class="'tone-' + (index % 6)" v-for="(course, index) in courses" :key="course.id">
        <div class="course-cover"><span>{{ course.totalPoints }} 个知识点</span></div>
        <div class="course-body">
          <h3>{{ course.name }}</h3>
          <p>{{ course.description }}</p>
          <el-progress :percentage="course.progress" />
          <el-button type="primary" style="margin-top: 12px" @click="$router.push(`/courses/${course.id}`)">进入课程</el-button>
        </div>
      </article>
    </div>
    <el-empty v-if="!loading && !courses.length" description="没有匹配的课程" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'

const keyword = ref('')
const loading = ref(false)
const courses = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    courses.value = await http.get('/courses', { params: { keyword: keyword.value } })
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
