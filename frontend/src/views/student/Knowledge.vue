<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>{{ detail?.name }}</h1>
        <p>{{ detail?.courseName }} / {{ detail?.chapterTitle }} · {{ statusText(detail?.status) }} · 做题正确率 {{ detail?.mastery || 0 }}%</p>
      </div>
      <div>
        <el-button @click="$router.back()">返回</el-button>
        <el-button type="primary" @click="$router.push(`/practice/${route.params.id}`)">开始练习</el-button>
        <el-button @click="ask">问资料</el-button>
        <el-button @click="dialog = true">记录学习</el-button>
      </div>
    </div>
    <div class="panel">
      <p>{{ detail?.description || '暂无说明' }}</p>
      <p><b>前置知识：</b>{{ names(detail?.prerequisites) }}</p>
      <p><b>后续知识：</b>{{ names(detail?.nextPoints) }}</p>
      <p><b>相关知识：</b>{{ names(detail?.related) }}</p>
    </div>
    <div class="panel" style="margin-top: 16px">
      <h3>学习资料</h3>
      <el-empty v-if="!detail?.documents?.length" description="这个知识点还没有资料" />
      <div class="point-row" v-for="doc in detail?.documents || []" :key="doc.id">
        <div>{{ doc.title }} <span class="muted">{{ doc.category }} · {{ doc.fileType }}</span></div>
        <div>
          <el-button v-if="isText(doc.fileType)" link type="primary" @click="openDoc(doc)">查看</el-button>
          <el-button link type="primary" @click="downloadDocument(doc.id, doc.fileName)">下载</el-button>
        </div>
      </div>
    </div>
    <el-dialog v-model="dialog" title="记录本次学习" width="420px">
      <el-form label-position="top">
        <el-form-item label="学习内容"><el-input v-model="form.content" /></el-form-item>
        <el-form-item label="时长（分钟）"><el-input-number v-model="form.durationMinutes" :min="1" :max="600" /></el-form-item>
        <el-form-item label="笔记"><el-input v-model="form.note" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveRecord">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="textVisible" :title="textTitle" width="640px">
      <pre style="white-space: pre-wrap; line-height: 1.7">{{ text }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http, { downloadDocument } from '../../api/http'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref<any>(null)
const dialog = ref(false)
const textVisible = ref(false)
const textTitle = ref('')
const text = ref('')
const form = reactive({ content: '', durationMinutes: 30, note: '' })

function statusText(status?: string) {
  if (status === 'MASTERED') return '已掌握'
  if (status === 'LEARNING') return '学习中'
  return '未学习'
}
function names(list?: { name: string }[]) {
  return list?.length ? list.map((item) => item.name).join('、') : '无'
}
function isText(type: string) {
  return ['txt', 'md', 'markdown'].includes(type)
}

async function load() {
  loading.value = true
  try {
    detail.value = await http.get(`/knowledge/${route.params.id}`)
    form.content = detail.value.name
  } finally {
    loading.value = false
  }
}

function ask() {
  router.push({
    path: '/chat',
    query: {
      courseId: detail.value?.courseId,
      q: `请根据资料讲解：${detail.value?.name || ''}`
    }
  })
}

async function openDoc(doc: any) {
  textTitle.value = doc.title
  text.value = await http.get(`/documents/${doc.id}/text`)
  textVisible.value = true
}

async function saveRecord() {
  await http.post('/study-records', {
    knowledgePointId: Number(route.params.id),
    content: form.content,
    durationMinutes: form.durationMinutes,
    note: form.note
  })
  ElMessage.success('已记下这次学习')
  dialog.value = false
  load()
}

onMounted(load)
</script>
