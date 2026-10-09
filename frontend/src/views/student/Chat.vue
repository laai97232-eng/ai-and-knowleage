<template>
  <div class="page chat-page">
    <div class="page-head">
      <div>
        <h1>AI 问答</h1>
        <p>{{ statusText }}</p>
      </div>
      <el-button type="primary" @click="newChat">新对话</el-button>
    </div>
    <div class="chat-layout">
      <aside class="panel chat-sessions">
        <div class="chat-side-head">历史对话</div>
        <el-empty v-if="!sessions.length" description="还没有对话" :image-size="64" />
        <button
          v-for="item in sessions"
          :key="item.id"
          class="session-item"
          :class="{ active: item.id === sessionId }"
          @click="openSession(item.id)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.courseName || '全部课程' }}</span>
        </button>
      </aside>
      <section class="panel chat-main">
        <div class="chat-toolbar">
          <el-select v-model="courseId" clearable placeholder="不限课程" style="width: 220px" :disabled="!!sessionId">
            <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
          </el-select>
          <el-button v-if="sessionId" link type="danger" @click="removeSession">删除本对话</el-button>
        </div>
        <div class="chat-stream" ref="stream">
          <div v-if="!messages.length" class="chat-empty">
            <p>按课程资料回答问题，并标出来源。</p>
            <button v-for="hint in hints" :key="hint" class="hint" @click="useHint(hint)">{{ hint }}</button>
          </div>
          <div v-for="message in messages" :key="message.id" class="bubble" :class="message.role">
            <div class="bubble-text">{{ message.content }}</div>
            <div v-if="message.sources?.length" class="sources">
              <span v-for="source in message.sources" :key="source.title + source.excerpt">{{ source.title }}</span>
            </div>
          </div>
        </div>
        <form class="chat-composer" @submit.prevent="send">
          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            resize="none"
            placeholder="例如：什么是 Java 的多态？"
            @keydown.enter.exact.prevent="send"
          />
          <el-button type="primary" native-type="submit" :loading="sending" :disabled="!question.trim()">发送</el-button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import http from '../../api/http'

const route = useRoute()
const courses = ref<any[]>([])
const sessions = ref<any[]>([])
const messages = ref<any[]>([])
const courseId = ref<number | undefined>()
const sessionId = ref<number | undefined>()
const question = ref('')
const sending = ref(false)
const modelEnabled = ref(false)
const chunkCount = ref(0)
const stream = ref<HTMLElement>()
const hints = ['什么是 Java 的多态？', 'ArrayList 为什么按下标查询比较快？', '封装的含义是什么？']

const statusText = computed(() => {
  const mode = modelEnabled.value ? '已接入对话模型' : '当前按资料原文摘录回答'
  return `${mode} · 已索引 ${chunkCount.value} 个资料片段`
})

async function loadSessions() {
  sessions.value = await http.get('/chat/sessions')
}

async function openSession(id: number) {
  const detail = await http.get<any>(`/chat/sessions/${id}`)
  sessionId.value = detail.id
  courseId.value = detail.courseId || undefined
  messages.value = detail.messages || []
  await scrollBottom()
}

function newChat() {
  sessionId.value = undefined
  messages.value = []
  question.value = ''
}

async function send() {
  const text = question.value.trim()
  if (!text || sending.value) return
  sending.value = true
  const localId = Date.now()
  messages.value.push({ id: localId, role: 'user', content: text, sources: [] })
  question.value = ''
  await scrollBottom()
  try {
    const data = await http.post<any>('/chat/ask', {
      sessionId: sessionId.value,
      courseId: courseId.value,
      question: text
    })
    sessionId.value = data.sessionId
    messages.value.push({
      id: localId + 1,
      role: 'assistant',
      content: data.answer,
      sources: data.sources || []
    })
    await loadSessions()
    await scrollBottom()
  } catch {
    messages.value = messages.value.filter((item) => item.id !== localId)
    question.value = text
  } finally {
    sending.value = false
  }
}

function useHint(hint: string) {
  question.value = hint
  send()
}

async function removeSession() {
  if (!sessionId.value) return
  await ElMessageBox.confirm('删除后不能恢复', '删除对话')
  await http.delete(`/chat/sessions/${sessionId.value}`)
  newChat()
  await loadSessions()
}

async function scrollBottom() {
  await nextTick()
  if (stream.value) stream.value.scrollTop = stream.value.scrollHeight
}

onMounted(async () => {
  const status = await http.get<any>('/chat/status')
  modelEnabled.value = !!status.modelEnabled
  chunkCount.value = status.chunkCount || 0
  courses.value = await http.get('/courses')
  await loadSessions()
  const presetCourse = Number(route.query.courseId)
  if (presetCourse) courseId.value = presetCourse
  const preset = typeof route.query.q === 'string' ? route.query.q : ''
  if (preset) {
    question.value = preset
    await send()
  }
})
</script>
