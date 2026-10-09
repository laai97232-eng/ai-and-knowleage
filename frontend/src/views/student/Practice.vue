<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>练习</h1>
        <p v-if="questions.length">第 {{ index + 1 }} / {{ questions.length }} 题</p>
      </div>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <el-empty v-if="!loading && !questions.length" description="这个知识点还没有题目" />
    <div class="panel" v-else-if="finished">
      <h2>本次答对 {{ correctCount }} / {{ questions.length }}</h2>
      <el-button type="primary" @click="$router.push('/wrong')">查看错题本</el-button>
    </div>
    <div class="panel" v-else-if="current">
      <el-tag>{{ current.type === 'JUDGE' ? '判断' : '单选' }}</el-tag>
      <h2>{{ current.content }}</h2>
      <el-radio-group v-model="choice" :disabled="!!result">
        <el-radio v-for="option in current.options" :key="option" :value="option.slice(0, 1)" style="display: flex; margin: 10px 0">
          {{ option }}
        </el-radio>
      </el-radio-group>
      <div v-if="result" style="margin-top: 12px">
        <el-alert :title="result.correct ? '回答正确' : '回答错误'" :type="result.correct ? 'success' : 'error'" :description="`正确答案 ${result.answer}。${result.analysis || ''}`" show-icon />
      </div>
      <div style="margin-top: 16px">
        <el-button v-if="!result" type="primary" :disabled="!choice" @click="submit">提交</el-button>
        <el-button v-else type="primary" @click="next">下一题</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http from '../../api/http'

const route = useRoute()
const loading = ref(false)
const questions = ref<any[]>([])
const index = ref(0)
const choice = ref('')
const result = ref<any>(null)
const correctCount = ref(0)
const finished = ref(false)
const current = computed(() => questions.value[index.value])

async function submit() {
  result.value = await http.post(`/questions/${current.value.id}/submit`, { answer: choice.value })
  if (result.value.correct) correctCount.value += 1
}

function next() {
  if (index.value >= questions.value.length - 1) {
    finished.value = true
    return
  }
  index.value += 1
  choice.value = ''
  result.value = null
}

onMounted(async () => {
  loading.value = true
  try {
    questions.value = await http.get(`/knowledge/${route.params.id}/questions`)
  } finally {
    loading.value = false
  }
})
</script>
