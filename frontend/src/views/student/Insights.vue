<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>学习分析</h1>
        <p>{{ data?.courseName || '选择一门课程' }} · 薄弱点来自错题和低于 60% 的正确率，学习路径按前置关系排列。</p>
      </div>
      <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="load">
        <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
      </el-select>
    </div>
    <div class="stat-grid">
      <div class="stat-card"><span>已掌握</span><strong>{{ data?.mastered || 0 }}</strong><span>个知识点</span></div>
      <div class="stat-card"><span>学习中</span><strong>{{ data?.learning || 0 }}</strong><span>个知识点</span></div>
      <div class="stat-card"><span>薄弱点</span><strong>{{ data?.weak || 0 }}</strong><span>个需要复习</span></div>
      <div class="stat-card"><span>做题正确率</span><strong>{{ data?.accuracy || 0 }}%</strong><span>{{ data?.attempts || 0 }} 次作答</span></div>
    </div>
    <div class="insight-grid">
      <div class="panel">
        <h3>近 7 天学习时长</h3>
        <div ref="lineEl" class="insight-chart"></div>
      </div>
      <div class="panel">
        <h3>掌握分布</h3>
        <div ref="pieEl" class="insight-chart"></div>
      </div>
    </div>
    <div class="insight-grid">
      <div class="panel">
        <h3>建议先做</h3>
        <el-empty v-if="!data?.recommendations?.length" description="这门课还没有可推荐的下一步" />
        <div class="advice" v-for="item in data?.recommendations || []" :key="item.kind + item.id">
          <div>
            <b>{{ item.name }}</b>
            <div class="muted">{{ kindText(item.kind) }} · {{ item.reason }}</div>
          </div>
          <el-button type="primary" link @click="$router.push(item.kind === 'REVIEW' ? `/practice/${item.id}` : `/knowledge/${item.id}`)">
            {{ item.kind === 'REVIEW' ? '去练习' : '去学习' }}
          </el-button>
        </div>
      </div>
      <div class="panel">
        <h3>薄弱知识点</h3>
        <el-empty v-if="!data?.weakPoints?.length" description="目前没有明显薄弱点" />
        <div class="advice" v-for="item in data?.weakPoints || []" :key="item.id">
          <div>
            <b>{{ item.name }}</b>
            <div class="muted">{{ item.reason }}</div>
          </div>
          <el-button type="primary" link @click="$router.push(`/practice/${item.id}`)">练习</el-button>
        </div>
      </div>
    </div>
    <div class="panel" style="margin-top: 16px">
      <h3>学习路径</h3>
      <el-empty v-if="!data?.path?.length" description="这门课还没有知识点" />
      <div class="path-list">
        <div class="path-step" v-for="(step, index) in data?.path || []" :key="step.id">
          <span class="path-index">{{ index + 1 }}</span>
          <div>
            <b>{{ step.name }}</b>
            <div class="muted">{{ stateText(step.state) }} · {{ step.reason }}</div>
          </div>
          <el-tag :type="tagType(step.state)" effect="plain">{{ stateText(step.state) }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import http from '../../api/http'

const route = useRoute()
const courses = ref<any[]>([])
const courseId = ref<number>()
const data = ref<any>(null)
const loading = ref(false)
const lineEl = ref<HTMLElement>()
const pieEl = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

function kindText(kind: string) {
  if (kind === 'REVIEW') return '复习'
  if (kind === 'RELATED') return '相关知识'
  return '下一站'
}
function stateText(state: string) {
  if (state === 'DONE') return '已掌握'
  if (state === 'READY') return '可以开始'
  return '先补前置'
}
function tagType(state: string) {
  if (state === 'DONE') return 'success'
  if (state === 'READY') return 'warning'
  return 'info'
}

async function load() {
  if (!courseId.value) return
  loading.value = true
  try {
    data.value = await http.get('/insights', { params: { courseId: courseId.value } })
    await nextTick()
    draw()
  } finally {
    loading.value = false
  }
}

function draw() {
  if (lineEl.value) {
    if (!lineChart) lineChart = echarts.init(lineEl.value)
    const weekly = data.value?.weekly || []
    lineChart.setOption({
      grid: { left: 36, right: 12, top: 24, bottom: 28 },
      xAxis: { type: 'category', data: weekly.map((item: any) => item.date), axisLine: { lineStyle: { color: '#8aa099' } } },
      yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: 'rgba(20,49,43,0.08)' } } },
      series: [{
        type: 'line',
        smooth: true,
        data: weekly.map((item: any) => item.minutes),
        areaStyle: { color: 'rgba(15,118,110,0.16)' },
        lineStyle: { color: '#0f766e', width: 2 },
        itemStyle: { color: '#0f766e' }
      }]
    })
  }
  if (pieEl.value) {
    if (!pieChart) pieChart = echarts.init(pieEl.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['42%', '68%'],
        label: { color: '#14312b' },
        data: [
          { name: '已掌握', value: data.value?.mastered || 0, itemStyle: { color: '#0f766e' } },
          { name: '学习中', value: data.value?.learning || 0, itemStyle: { color: '#c4843a' } },
          { name: '薄弱', value: data.value?.weak || 0, itemStyle: { color: '#b4534b' } },
          { name: '未学习', value: data.value?.notStarted || 0, itemStyle: { color: '#8aa099' } }
        ]
      }]
    })
  }
}

function resize() {
  lineChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  courses.value = await http.get('/courses')
  const preset = Number(route.query.courseId)
  courseId.value = preset || courses.value[0]?.id
  window.addEventListener('resize', resize)
  await load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  lineChart?.dispose()
  pieChart?.dispose()
})
</script>
