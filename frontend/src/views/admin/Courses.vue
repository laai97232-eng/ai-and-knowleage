<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>课程管理</h1>
        <p>创建课程后，进入课程维护章节和知识点。</p>
      </div>
      <el-button type="primary" @click="open()">新建课程</el-button>
    </div>
    <el-table :data="courses" v-loading="loading">
      <el-table-column prop="name" label="课程" />
      <el-table-column prop="totalPoints" label="知识点" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ row.status === 1 ? '上架' : '下架' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/admin/courses/${row.id}`)">章节</el-button>
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialog" title="课程" width="480px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { ElMessageBox } from 'element-plus'
import http from '../../api/http'

const loading = ref(false)
const dialog = ref(false)
const courses = ref<any[]>([])
const currentId = ref<number>()
const form = reactive({ name: '', description: '', status: 1 })

async function load() {
  loading.value = true
  try {
    courses.value = await http.get('/admin/courses')
  } finally {
    loading.value = false
  }
}

function open(row?: any) {
  currentId.value = row?.id
  form.name = row?.name || ''
  form.description = row?.description || ''
  form.status = row?.status ?? 1
  dialog.value = true
}

async function save() {
  if (currentId.value) await http.put(`/admin/courses/${currentId.value}`, form)
  else await http.post('/admin/courses', form)
  dialog.value = false
  load()
}

async function remove(id: number) {
  await ElMessageBox.confirm('将同时删除章节、知识点、题目和资料', '确认删除')
  await http.delete(`/admin/courses/${id}`)
  load()
}

onMounted(load)
</script>
