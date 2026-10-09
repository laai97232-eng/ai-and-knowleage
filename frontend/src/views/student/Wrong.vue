<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>错题本</h1>
        <p>答错会记在这里。再次答对后会自动移出，也可以手动移除。</p>
      </div>
    </div>
    <el-table :data="rows">
      <el-table-column prop="knowledgeName" label="知识点" width="140" />
      <el-table-column prop="content" label="题目" />
      <el-table-column prop="lastAnswer" label="上次答案" width="100" />
      <el-table-column prop="wrongCount" label="错误次数" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/practice/${row.knowledgePointId}`)">再练一次</el-button>
          <el-button link type="danger" @click="remove(row.id)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="目前没有错题" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '../../api/http'

const loading = ref(false)
const rows = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    rows.value = await http.get('/wrong-questions')
  } finally {
    loading.value = false
  }
}

async function remove(id: number) {
  await http.delete(`/wrong-questions/${id}`)
  load()
}

onMounted(load)
</script>
