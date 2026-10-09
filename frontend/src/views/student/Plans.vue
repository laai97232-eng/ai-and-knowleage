<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>学习计划</h1>
        <p>选择一门课程和天数，系统会按章节顺序把知识点分到每一天。</p>
      </div>
      <el-button type="primary" @click="dialog = true">生成计划</el-button>
    </div>
    <el-table :data="plans" v-loading="loading">
      <el-table-column prop="title" label="计划" />
      <el-table-column label="日期" width="220">
        <template #default="{ row }">{{ row.startDate }} 至 {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column label="进度" width="140">
        <template #default="{ row }">{{ row.done }}/{{ row.total }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ row.status === 'DONE' ? '已完成' : '进行中' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/plans/${row.id}`)">查看</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialog" title="生成学习计划" width="460px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="例如：30天完成 Java 基础学习" /></el-form-item>
        <el-form-item label="课程">
          <el-select v-model="form.courseId" style="width: 100%">
            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="天数"><el-input-number v-model="form.days" :min="1" :max="90" /></el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="create">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import http from '../../api/http'

const router = useRouter()
const loading = ref(false)
const dialog = ref(false)
const plans = ref<any[]>([])
const courses = ref<any[]>([])
const form = reactive({ title: '30天完成 Java 基础学习', courseId: undefined as number | undefined, days: 30, startDate: '' })

async function load() {
  loading.value = true
  try {
    plans.value = await http.get('/study-plans')
    courses.value = await http.get('/courses')
    if (!form.courseId && courses.value.length) form.courseId = courses.value[0].id
  } finally {
    loading.value = false
  }
}

async function create() {
  const detail = await http.post<any>('/study-plans/generate', form)
  dialog.value = false
  router.push(`/plans/${detail.id}`)
}

async function remove(id: number) {
  await http.delete(`/study-plans/${id}`)
  load()
}

onMounted(load)
</script>
