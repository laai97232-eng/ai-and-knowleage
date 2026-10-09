<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>学习记录</h1>
        <p>每次学习可以记下内容、时长和笔记。</p>
      </div>
      <el-button type="primary" @click="dialog = true">新增记录</el-button>
    </div>
    <el-table :data="page.records" v-loading="loading">
      <el-table-column prop="studyDate" label="日期" width="120" />
      <el-table-column prop="courseName" label="课程" width="160" />
      <el-table-column prop="knowledgeName" label="知识点" width="140" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="durationMinutes" label="分钟" width="80" />
      <el-table-column prop="note" label="笔记" />
    </el-table>
    <el-pagination
      style="margin-top: 16px"
      layout="prev, pager, next"
      :total="page.total"
      :page-size="page.size"
      @current-change="change"
    />
    <el-dialog v-model="dialog" title="新增学习记录" width="460px">
      <el-form label-position="top">
        <el-form-item label="知识点">
          <el-select v-model="form.knowledgePointId" filterable style="width: 100%">
            <el-option v-for="item in points" :key="item.id" :label="`${item.courseName} / ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" /></el-form-item>
        <el-form-item label="时长"><el-input-number v-model="form.durationMinutes" :min="1" :max="600" /></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="form.studyDate" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="笔记"><el-input v-model="form.note" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'

const loading = ref(false)
const dialog = ref(false)
const points = ref<any[]>([])
const page = reactive({ total: 0, page: 1, size: 10, records: [] as any[] })
const form = reactive({ knowledgePointId: undefined as number | undefined, content: '', durationMinutes: 30, note: '', studyDate: '' })

async function load(pageNo = 1) {
  loading.value = true
  try {
    const data = await http.get<any>('/study-records', { params: { page: pageNo, size: 10 } })
    Object.assign(page, data)
  } finally {
    loading.value = false
  }
}

function change(pageNo: number) {
  load(pageNo)
}

async function save() {
  await http.post('/study-records', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

onMounted(async () => {
  const courses = await http.get<any[]>('/courses')
  const all: any[] = []
  for (const course of courses) {
    const detail = await http.get<any>(`/courses/${course.id}`)
    for (const chapter of detail.chapters || []) {
      for (const point of chapter.points || []) {
        all.push({ id: point.id, name: point.name, courseName: course.name })
      }
    }
  }
  points.value = all
  load()
})
</script>
