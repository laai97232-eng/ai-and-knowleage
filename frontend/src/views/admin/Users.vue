<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p>查询、修改角色、禁用或删除用户。</p>
      </div>
      <el-input v-model="keyword" placeholder="搜索用户名" style="width: 200px" clearable @keyup.enter="load(1)" />
    </div>
    <el-table :data="page.records" v-loading="loading">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">{{ row.role === 'ADMIN' ? '管理员' : '学生' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ row.status === 1 ? '正常' : '禁用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="edit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 16px" layout="prev, pager, next" :total="page.total" :page-size="10" @current-change="load" />
    <el-dialog v-model="dialog" title="编辑用户" width="420px">
      <el-form label-position="top">
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="学生" value="STUDENT" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">禁用</el-radio>
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
const keyword = ref('')
const dialog = ref(false)
const currentId = ref<number>()
const page = reactive({ total: 0, records: [] as any[] })
const form = reactive({ email: '', role: 'STUDENT', status: 1 })

async function load(pageNo = 1) {
  loading.value = true
  try {
    const data = await http.get<any>('/admin/users', { params: { keyword: keyword.value, page: pageNo, size: 10 } })
    page.total = data.total
    page.records = data.records
  } finally {
    loading.value = false
  }
}

function edit(row: any) {
  currentId.value = row.id
  form.email = row.email || ''
  form.role = row.role
  form.status = row.status
  dialog.value = true
}

async function save() {
  await http.put(`/admin/users/${currentId.value}`, form)
  dialog.value = false
  load()
}

async function remove(id: number) {
  await ElMessageBox.confirm('删除后该用户的学习数据也会清除', '确认删除')
  await http.delete(`/admin/users/${id}`)
  load()
}

onMounted(() => load(1))
</script>
