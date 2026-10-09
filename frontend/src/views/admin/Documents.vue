<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>资料管理</h1>
        <p>支持 PDF、Word、Markdown、TXT、PPT。文本资料学生可以直接打开。</p>
      </div>
      <div>
        <el-button @click="rebuild" :loading="rebuilding">重建资料索引</el-button>
        <el-button type="primary" @click="dialog = true">上传资料</el-button>
      </div>
    </div>
    <el-table :data="page.records" v-loading="loading">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="courseName" label="课程" width="140" />
      <el-table-column prop="knowledgeName" label="知识点" width="120" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="fileType" label="类型" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">{{ row.status === 1 ? '通过' : '待审' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="toggle(row)">{{ row.status === 1 ? '撤回' : '通过' }}</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 16px" layout="prev, pager, next" :total="page.total" :page-size="10" @current-change="load" />
    <el-dialog v-model="dialog" title="上传资料" width="480px">
      <el-form label-position="top">
        <el-form-item label="课程">
          <el-select v-model="form.courseId" style="width: 100%" @change="form.knowledgePointId = undefined">
            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-select v-model="form.knowledgePointId" clearable style="width: 100%">
            <el-option v-for="item in filteredPoints" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option label="讲义" value="讲义" />
            <el-option label="课件" value="课件" />
            <el-option label="笔记" value="笔记" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件"><input type="file" @change="onFile" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="upload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'

const loading = ref(false)
const rebuilding = ref(false)
const dialog = ref(false)
const courses = ref<any[]>([])
const points = ref<any[]>([])
const file = ref<File>()
const page = reactive({ total: 0, records: [] as any[] })
const form = reactive({ courseId: undefined as number | undefined, knowledgePointId: undefined as number | undefined, title: '', category: '讲义' })
const filteredPoints = computed(() => points.value.filter((item) => item.courseId === form.courseId))

async function load(pageNo = 1) {
  loading.value = true
  try {
    const data = await http.get<any>('/admin/documents', { params: { page: pageNo, size: 10 } })
    page.total = data.total
    page.records = data.records
  } finally {
    loading.value = false
  }
}

function onFile(event: Event) {
  file.value = (event.target as HTMLInputElement).files?.[0]
}

async function upload() {
  const data = new FormData()
  data.append('courseId', String(form.courseId || ''))
  if (form.knowledgePointId) data.append('knowledgePointId', String(form.knowledgePointId))
  data.append('title', form.title)
  data.append('category', form.category)
  if (file.value) data.append('file', file.value)
  await http.post('/admin/documents', data)
  dialog.value = false
  load()
}

async function toggle(row: any) {
  await http.put(`/admin/documents/${row.id}`, { status: row.status === 1 ? 0 : 1 })
  load()
}

async function remove(id: number) {
  await http.delete(`/admin/documents/${id}`)
  load()
}

async function rebuild() {
  rebuilding.value = true
  try {
    const count = await http.post<number>('/admin/index/rebuild')
    ElMessage.success(`索引已更新，共 ${count} 个片段`)
  } finally {
    rebuilding.value = false
  }
}

onMounted(async () => {
  courses.value = await http.get('/admin/courses')
  points.value = await http.get('/admin/knowledge-points')
  load()
})
</script>
