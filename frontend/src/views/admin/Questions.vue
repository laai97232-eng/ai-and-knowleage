<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>题库管理</h1>
        <p>单选题和判断题会自动判分，并写入错题与掌握情况。</p>
      </div>
      <el-button type="primary" @click="open()">新增题目</el-button>
    </div>
    <el-table :data="page.records" v-loading="loading">
      <el-table-column prop="courseName" label="课程" width="140" />
      <el-table-column prop="knowledgeName" label="知识点" width="120" />
      <el-table-column label="题型" width="80">
        <template #default="{ row }">{{ row.type === 'JUDGE' ? '判断' : '单选' }}</template>
      </el-table-column>
      <el-table-column prop="content" label="题干" />
      <el-table-column prop="answer" label="答案" width="80" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination style="margin-top: 16px" layout="prev, pager, next" :total="page.total" :page-size="10" @current-change="load" />
    <el-dialog v-model="dialog" title="题目" width="560px">
      <el-form label-position="top">
        <el-form-item label="知识点">
          <el-select v-model="form.knowledgePointId" filterable style="width: 100%">
            <el-option v-for="item in points" :key="item.id" :label="`${item.courseName} / ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-radio-group v-model="form.type">
            <el-radio value="SINGLE">单选</el-radio>
            <el-radio value="JUDGE">判断</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干"><el-input v-model="form.content" type="textarea" /></el-form-item>
        <el-form-item v-if="form.type === 'SINGLE'" label="选项">
          <el-input v-for="(_, i) in form.options" :key="i" v-model="form.options[i]" style="margin-bottom: 8px" />
        </el-form-item>
        <el-form-item label="答案">
          <el-select v-model="form.answer" style="width: 120px">
            <el-option v-for="letter in form.type === 'JUDGE' ? ['A', 'B'] : ['A', 'B', 'C', 'D']" :key="letter" :label="letter" :value="letter" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" /></el-form-item>
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
import http from '../../api/http'

const loading = ref(false)
const dialog = ref(false)
const points = ref<any[]>([])
const currentId = ref<number>()
const page = reactive({ total: 0, records: [] as any[] })
const form = reactive({
  knowledgePointId: undefined as number | undefined,
  type: 'SINGLE',
  content: '',
  options: ['A. ', 'B. ', 'C. ', 'D. '],
  answer: 'A',
  analysis: ''
})

async function load(pageNo = 1) {
  loading.value = true
  try {
    const data = await http.get<any>('/admin/questions', { params: { page: pageNo, size: 10 } })
    page.total = data.total
    page.records = data.records
  } finally {
    loading.value = false
  }
}

function open(row?: any) {
  currentId.value = row?.id
  form.knowledgePointId = row?.knowledgePointId
  form.type = row?.type || 'SINGLE'
  form.content = row?.content || ''
  form.options = row?.options?.length ? [...row.options] : ['A. ', 'B. ', 'C. ', 'D. ']
  form.answer = row?.answer || 'A'
  form.analysis = row?.analysis || ''
  dialog.value = true
}

async function save() {
  const payload = { ...form, options: form.type === 'JUDGE' ? ['A. 正确', 'B. 错误'] : form.options }
  if (currentId.value) await http.put(`/admin/questions/${currentId.value}`, payload)
  else await http.post('/admin/questions', payload)
  dialog.value = false
  load()
}

async function remove(id: number) {
  await http.delete(`/admin/questions/${id}`)
  load()
}

onMounted(async () => {
  points.value = await http.get('/admin/knowledge-points')
  load()
})
</script>
