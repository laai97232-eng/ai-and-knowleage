<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <div>
        <h1>知识图谱</h1>
        <p>圆点是知识点，实线是前置关系，虚线是相关关系。颜色表示你目前的掌握情况。</p>
      </div>
      <el-select v-model="courseId" placeholder="选择课程" style="width: 220px" @change="load">
        <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
      </el-select>
    </div>
    <div class="panel graph-panel">
      <div class="graph-legend">
        <span><i class="dot mastered"></i>已掌握</span>
        <span><i class="dot learning"></i>学习中</span>
        <span><i class="dot weak"></i>薄弱</span>
        <span><i class="dot idle"></i>未学习</span>
      </div>
      <div ref="chartEl" class="graph-canvas"></div>
      <el-empty v-if="!loading && !nodeCount" description="这门课还没有知识点" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import http from '../../api/http'

const route = useRoute()
const router = useRouter()
const courses = ref<any[]>([])
const courseId = ref<number>()
const loading = ref(false)
const nodeCount = ref(0)
const chartEl = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const colors = ['#0f766e', '#c4843a', '#b4534b', '#8aa099']

async function load() {
  if (!courseId.value) return
  loading.value = true
  try {
    const data = await http.get<any>('/insights/graph', { params: { courseId: courseId.value } })
    nodeCount.value = data.nodes?.length || 0
    await nextTick()
    draw(data)
  } finally {
    loading.value = false
  }
}

function draw(data: any) {
  if (!chartEl.value) return
  if (!chart) chart = echarts.init(chartEl.value)
  const nodes = (data.nodes || []).map((node: any) => ({
    id: String(node.id),
    name: node.name,
    category: node.category,
    symbolSize: node.category === 2 ? 58 : 48,
    pointId: node.id,
    chapter: node.chapter,
    mastery: node.mastery,
    itemStyle: { color: colors[node.category] || colors[3] }
  }))
  const links = (data.edges || []).map((edge: any) => ({
    source: String(edge.source),
    target: String(edge.target),
    lineStyle: {
      type: edge.type === 'RELATED' ? 'dashed' : 'solid',
      color: edge.type === 'RELATED' ? '#c4843a' : '#0f766e',
      width: 1.4
    }
  }))
  chart.setOption({
    tooltip: {
      formatter: (params: any) => {
        if (params.dataType !== 'node') return ''
        return `${params.data.name}<br/>${params.data.chapter || ''}<br/>掌握度 ${params.data.mastery || 0}%`
      }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      label: { show: true, color: '#14312b', fontSize: 13 },
      force: { repulsion: 360, edgeLength: 130, gravity: 0.08 },
      data: nodes,
      links
    }]
  })
  chart.off('click')
  chart.on('click', (params: any) => {
    if (params.dataType === 'node' && params.data.pointId) {
      router.push(`/knowledge/${params.data.pointId}`)
    }
  })
}

function resize() {
  chart?.resize()
}

watch(() => route.query.courseId, (value) => {
  const id = Number(value)
  if (id && id !== courseId.value) {
    courseId.value = id
    load()
  }
})

onMounted(async () => {
  courses.value = await http.get('/courses')
  const preset = Number(route.query.courseId)
  courseId.value = preset || courses.value[0]?.id
  window.addEventListener('resize', resize)
  await load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
  chart = null
})
</script>
