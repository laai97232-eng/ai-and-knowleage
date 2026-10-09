<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>知识关系</h1>
        <p>前置关系表示起点是终点的前置知识，用来排列学习路径。相关关系表示两边有联系，会画在知识图谱里。</p>
      </div>
    </div>
    <div class="panel" style="margin-bottom: 16px">
      <el-select v-model="form.sourceId" filterable placeholder="起点" style="width: 220px; margin-right: 8px">
        <el-option v-for="item in points" :key="item.id" :label="`${item.courseName} / ${item.name}`" :value="item.id" />
      </el-select>
      <el-select v-model="form.relationType" style="width: 140px; margin-right: 8px">
        <el-option label="前置知识" value="PREREQUISITE" />
        <el-option label="相关知识" value="RELATED" />
      </el-select>
      <el-select v-model="form.targetId" filterable placeholder="终点" style="width: 220px; margin-right: 8px">
        <el-option v-for="item in points" :key="item.id" :label="`${item.courseName} / ${item.name}`" :value="item.id" />
      </el-select>
      <el-button type="primary" @click="create">添加</el-button>
    </div>
    <el-table :data="rows" v-loading="loading">
      <el-table-column prop="sourceName" label="起点" />
      <el-table-column label="关系" width="140">
        <template #default="{ row }">{{ row.relationType === 'PREREQUISITE' ? '前置知识' : '相关知识' }}</template>
      </el-table-column>
      <el-table-column prop="targetName" label="终点" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import http from '../../api/http'

const loading = ref(false)
const rows = ref<any[]>([])
const points = ref<any[]>([])
const form = reactive({ sourceId: undefined as number | undefined, targetId: undefined as number | undefined, relationType: 'PREREQUISITE' })

async function load() {
  loading.value = true
  try {
    rows.value = await http.get('/admin/relations')
    points.value = await http.get('/admin/knowledge-points')
  } finally {
    loading.value = false
  }
}

async function create() {
  await http.post('/admin/relations', form)
  load()
}

async function remove(id: number) {
  await http.delete(`/admin/relations/${id}`)
  load()
}

onMounted(load)
</script>
