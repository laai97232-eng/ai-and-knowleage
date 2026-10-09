<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>{{ plan?.title }}</h1>
        <p>{{ plan?.description }} · {{ plan?.startDate }} 至 {{ plan?.endDate }}</p>
      </div>
      <el-button @click="$router.push('/plans')">返回</el-button>
    </div>
    <div class="panel" v-for="day in days" :key="day" style="margin-bottom: 12px">
      <h3>第 {{ day }} 天</h3>
      <div class="point-row" v-for="item in itemsOf(day)" :key="item.id">
        <div>
          <b>{{ item.content }}</b>
          <div class="muted">{{ item.courseName }} · {{ item.status === 'DONE' ? '已完成' : '未完成' }}</div>
          <div v-if="item.note" class="muted">笔记：{{ item.note }}</div>
        </div>
        <div>
          <el-button v-if="item.knowledgePointId" @click="$router.push(`/knowledge/${item.knowledgePointId}`)">去学习</el-button>
          <el-button type="primary" @click="open(item)">打卡</el-button>
        </div>
      </div>
    </div>
    <el-dialog v-model="dialog" title="完成打卡" width="420px">
      <el-form label-position="top">
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="DONE">完成</el-radio>
            <el-radio value="TODO">未完成</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="学习时长（分钟，可不填）"><el-input-number v-model="form.durationMinutes" :min="0" :max="600" /></el-form-item>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'

const route = useRoute()
const loading = ref(false)
const plan = ref<any>(null)
const dialog = ref(false)
const currentId = ref<number>()
const form = reactive({ status: 'DONE', note: '', durationMinutes: 30 })
const days = computed(() => [...new Set((plan.value?.items || []).map((item: any) => item.dayIndex))])

function itemsOf(day: number) {
  return (plan.value?.items || []).filter((item: any) => item.dayIndex === day)
}

async function load() {
  loading.value = true
  try {
    plan.value = await http.get(`/study-plans/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

function open(item: any) {
  currentId.value = item.id
  form.status = item.status === 'DONE' ? 'DONE' : 'DONE'
  form.note = item.note || ''
  dialog.value = true
}

async function save() {
  await http.put(`/study-plan-items/${currentId.value}`, {
    status: form.status,
    note: form.note,
    durationMinutes: form.durationMinutes > 0 ? form.durationMinutes : null
  })
  ElMessage.success('已更新')
  dialog.value = false
  load()
}

onMounted(load)
</script>
