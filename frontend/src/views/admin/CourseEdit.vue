<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>{{ course?.name }}</h1>
        <p>维护章节和知识点。</p>
      </div>
      <el-button @click="$router.push('/admin/courses')">返回</el-button>
    </div>
    <div class="panel" style="margin-bottom: 16px">
      <el-input v-model="chapterTitle" placeholder="新章节名称" style="width: 280px; margin-right: 8px" />
      <el-button type="primary" @click="addChapter">添加章节</el-button>
    </div>
    <div class="panel" v-for="chapter in course?.chapters || []" :key="chapter.id" style="margin-bottom: 12px">
      <div style="display: flex; justify-content: space-between">
        <h3>{{ chapter.title }}</h3>
        <el-button link type="danger" @click="removeChapter(chapter.id)">删除章节</el-button>
      </div>
      <el-table :data="chapter.points" size="small">
        <el-table-column prop="name" label="知识点" />
        <el-table-column prop="description" label="说明" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openPoint(chapter.id, row)">编辑</el-button>
            <el-button link type="danger" @click="removePoint(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button style="margin-top: 8px" @click="openPoint(chapter.id)">添加知识点</el-button>
    </div>
    <el-dialog v-model="dialog" title="知识点" width="480px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="savePoint">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import http from '../../api/http'

const route = useRoute()
const loading = ref(false)
const course = ref<any>(null)
const chapterTitle = ref('')
const dialog = ref(false)
const chapterId = ref<number>()
const pointId = ref<number>()
const form = reactive({ name: '', description: '' })

async function load() {
  loading.value = true
  try {
    course.value = await http.get(`/admin/courses/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function addChapter() {
  if (!chapterTitle.value.trim()) return
  await http.post(`/admin/courses/${route.params.id}/chapters`, { title: chapterTitle.value })
  chapterTitle.value = ''
  load()
}

async function removeChapter(id: number) {
  await ElMessageBox.confirm('请确认该章节下已经没有知识点', '删除章节')
  await http.delete(`/admin/chapters/${id}`)
  load()
}

function openPoint(cid: number, row?: any) {
  chapterId.value = cid
  pointId.value = row?.id
  form.name = row?.name || ''
  form.description = row?.description || ''
  dialog.value = true
}

async function savePoint() {
  if (pointId.value) await http.put(`/admin/knowledge/${pointId.value}`, form)
  else await http.post(`/admin/chapters/${chapterId.value}/knowledge`, form)
  dialog.value = false
  load()
}

async function removePoint(id: number) {
  await ElMessageBox.confirm('将同时删除该知识点的题目、资料和关系', '删除知识点')
  await http.delete(`/admin/knowledge/${id}`)
  load()
}

onMounted(load)
</script>
