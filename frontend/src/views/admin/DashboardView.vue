<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import ChartPanel from '@/components/ChartPanel.vue'
import {
  statisticsMuseumHeat,
  statisticsOverview,
  statisticsSlotSaturation,
  statisticsTicketDistributionDetail,
  statisticsTrend,
} from '@/api/admin'
import type { NameValue, Overview, SlotSaturation, TicketDistribution, TrendPoint } from '@/types'

const overview = ref<Overview | null>(null)
const trend = ref<TrendPoint[]>([])
const heat = ref<NameValue[]>([])
const saturation = ref<SlotSaturation[]>([])
const distribution = ref<TicketDistribution[]>([])
const trendDays = ref(7)
const loading = ref(true)

const kpis = computed(() => [
  {
    label: '今日预约',
    value: overview.value?.todayReservations ?? 0,
    unit: '单',
    icon: 'Tickets',
    color: '#b08a4f',
    hint: '今日新增预约订单',
  },
  {
    label: '今日参观人数',
    value: overview.value?.todayVisitors ?? 0,
    unit: '人',
    icon: 'User',
    color: '#3f7a5c',
    hint: '今日预约参观总人数',
  },
  {
    label: '今日核销',
    value: overview.value?.todayCheckins ?? 0,
    unit: '次',
    icon: 'CircleCheck',
    color: '#4b6b9a',
    hint: '已完成核销',
  },
  {
    label: '待支付订单',
    value: overview.value?.pendingPayments ?? 0,
    unit: '单',
    icon: 'Wallet',
    color: '#c28a3e',
    hint: '超时将自动释放票额',
  },
])

const checkinRate = computed(() => {
  const checked = overview.value?.todayCheckins ?? 0
  const visitors = overview.value?.todayVisitors ?? 0
  if (!visitors) return 0
  return Math.min(100, Math.round((checked / visitors) * 100))
})

const cancelRate = computed(() => {
  const canceled = overview.value?.todayCanceled ?? 0
  const total = overview.value?.todayReservations ?? 0
  if (!total) return 0
  return Math.min(100, Math.round((canceled / total) * 100))
})

const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', backgroundColor: '#172033', borderWidth: 0, textStyle: { color: '#f7f3eb', fontSize: 11 } },
  legend: {
    top: 0,
    right: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#8a8377', fontSize: 11 },
    data: ['预约单量', '参观人数', '核销量'],
  },
  grid: { left: 8, right: 16, top: 40, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: trend.value.map((item) => dayjs(item.date).format('MM/DD')),
    axisLine: { lineStyle: { color: '#e5ddcf' } },
    axisTick: { show: false },
    axisLabel: { color: '#9b9488', fontSize: 10 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f0ebe1' } },
    axisLabel: { color: '#9b9488', fontSize: 10 },
  },
  series: [
    {
      name: '预约单量',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: trend.value.map((item) => item.reservations),
      itemStyle: { color: '#b08a4f' },
      lineStyle: { width: 2.5 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(176,138,79,0.22)' },
            { offset: 1, color: 'rgba(176,138,79,0)' },
          ],
        },
      },
    },
    {
      name: '参观人数',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: trend.value.map((item) => item.visitors),
      itemStyle: { color: '#3f7a5c' },
      lineStyle: { width: 2 },
    },
    {
      name: '核销量',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: trend.value.map((item) => item.checkins),
      itemStyle: { color: '#4b6b9a' },
      lineStyle: { width: 2 },
    },
  ],
}))

const heatOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 8, right: 24, top: 10, bottom: 8, containLabel: true },
  xAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f0ebe1' } },
    axisLabel: { color: '#9b9488', fontSize: 10 },
  },
  yAxis: {
    type: 'category',
    data: heat.value.map((item) => item.name),
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#5f5a52', fontSize: 11 },
  },
  series: [
    {
      type: 'bar',
      data: heat.value.map((item) => item.value),
      barWidth: 14,
      itemStyle: {
        borderRadius: [0, 7, 7, 0],
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 1,
          y2: 0,
          colorStops: [
            { offset: 0, color: '#d5bb8b' },
            { offset: 1, color: '#b08a4f' },
          ],
        },
      },
      label: { show: true, position: 'right', color: '#8a8377', fontSize: 10 },
    },
  ],
}))

const ticketKindOrder = ['普通预约票', '单展票', '全馆联票']

const distributionData = computed(() => {
  const map = new Map(distribution.value.map((item) => [item.name, item]))
  return ticketKindOrder.map((name) => {
    const found = map.get(name)
    return {
      name,
      value: Number(found?.value) || 0,
      children: (found?.children ?? []).filter((child) => child.value > 0),
    }
  })
})

const pieOption = computed<EChartsOption>(() => {
  const data = distributionData.value
  const total = data.reduce((sum, item) => sum + item.value, 0)
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#172033',
      borderWidth: 0,
      padding: [10, 14],
      textStyle: { color: '#f7f3eb', fontSize: 11 },
      formatter: (params: unknown) => {
        const item = params as {
          name: string
          value: number
          percent?: number
          data?: { children?: NameValue[] }
        }
        const percent =
          item.percent != null
            ? item.percent.toFixed(1)
            : total
              ? ((item.value / total) * 100).toFixed(1)
              : '0.0'
        const head = `${item.name}<br/>数量：${item.value} 单<br/>比例：${percent}%`
        const children = item.data?.children ?? []
        if (!children.length) {
          return head
        }
        const detail = children
          .map((child) => `${child.name}：${child.value} 单`)
          .join('<br/>')
        return `${head}<br/><span style="display:inline-block;margin-top:4px;color:#a9b0bd">具体票种</span><br/>${detail}`
      },
    },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 9,
      itemHeight: 9,
      textStyle: { color: '#8a8377', fontSize: 10 },
      data: ticketKindOrder,
    },
    color: ['#b08a4f', '#3f7a5c', '#4b6b9a'],
    series: [
      {
        name: '票种分布',
        type: 'pie',
        radius: ['48%', '70%'],
        center: ['50%', '43%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fffdf9', borderWidth: 3 },
        label: { show: false },
        labelLine: { show: false },
        emphasis: {
          scale: true,
          scaleSize: 6,
          label: { show: false },
        },
        data,
      },
    ],
  }
})

async function load() {
  loading.value = true
  try {
    const [overviewData, trendData, heatData, saturationData, distributionData] = await Promise.all([
      statisticsOverview(),
      statisticsTrend(trendDays.value),
      statisticsMuseumHeat(30),
      statisticsSlotSaturation(),
      statisticsTicketDistributionDetail(),
    ])
    overview.value = overviewData
    trend.value = trendData
    heat.value = heatData
    saturation.value = saturationData
    distribution.value = distributionData
  } finally {
    loading.value = false
  }
}

async function changeTrend(days: number) {
  trendDays.value = days
  trend.value = await statisticsTrend(days)
}

function onTrendChange(value: string | number | boolean | undefined) {
  changeTrend(Number(value))
}

onMounted(load)
</script>

<template>
  <div class="dashboard">
    <PageHeader
      eyebrow="OPERATION OVERVIEW"
      title="数据看板"
      desc="今日运营数据、近七日趋势、博物馆热度与时段饱和度一览。"
    >
      <span class="date-chip">{{ dayjs().format('YYYY 年 MM 月 DD 日') }}</span>
      <el-button :loading="loading" plain @click="load">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </PageHeader>

    <div v-if="loading && !overview" class="kpi-grid">
      <div v-for="i in 4" :key="i" class="kpi-card">
        <el-skeleton :rows="2" animated />
      </div>
    </div>
    <div v-else class="kpi-grid">
      <div v-for="kpi in kpis" :key="kpi.label" class="kpi-card">
        <div class="kpi-icon" :style="{ color: kpi.color, background: `${kpi.color}14` }">
          <el-icon><component :is="kpi.icon" /></el-icon>
        </div>
        <div class="kpi-copy">
          <span>{{ kpi.label }}</span>
          <strong class="serif">
            {{ kpi.value }}
            <small>{{ kpi.unit }}</small>
          </strong>
          <em>{{ kpi.hint }}</em>
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="admin-card trend-card">
        <div class="table-head">
          <div>
            <h3>预约与核销趋势</h3>
            <small>近 {{ trendDays }} 天预约量、参观人数与核销量</small>
          </div>
          <el-radio-group v-model="trendDays" size="small" @change="onTrendChange">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
        <ChartPanel :option="trendOption" :height="330" />
      </div>

      <div class="admin-card status-card">
        <div class="table-head">
          <div>
            <h3>今日概览</h3>
            <small>核销率与取消率实时计算</small>
          </div>
        </div>
        <div class="progress-item">
          <div class="progress-head">
            <span>核销率</span>
            <strong class="serif">{{ checkinRate }}%</strong>
          </div>
          <div class="progress-track">
            <i :style="{ width: `${checkinRate}%`, background: 'linear-gradient(90deg, #6f9a7f, #3f7a5c)' }"></i>
          </div>
          <small>今日核销 {{ overview?.todayCheckins || 0 }} 次 / 参观 {{ overview?.todayVisitors || 0 }} 人</small>
        </div>
        <div class="progress-item">
          <div class="progress-head">
            <span>取消率</span>
            <strong class="serif">{{ cancelRate }}%</strong>
          </div>
          <div class="progress-track">
            <i :style="{ width: `${cancelRate}%`, background: 'linear-gradient(90deg, #d5bb8b, #b08a4f)' }"></i>
          </div>
          <small>今日取消 {{ overview?.todayCanceled || 0 }} 单</small>
        </div>
        <div class="quick-stats">
          <div>
            <span>累计预约</span>
            <strong class="serif">{{ overview?.totalReservations || 0 }}</strong>
          </div>
          <div>
            <span>注册用户</span>
            <strong class="serif">{{ overview?.totalUsers || 0 }}</strong>
          </div>
          <div>
            <span>博物馆</span>
            <strong class="serif">{{ overview?.totalMuseums || 0 }}</strong>
          </div>
        </div>
        <el-button class="checkin-link" plain @click="$router.push('/admin/checkin')">
          <el-icon><Tickets /></el-icon>
          进入票码核销
        </el-button>
      </div>
    </div>

    <div class="chart-grid lower">
      <div class="admin-card">
        <div class="table-head">
          <div>
            <h3>博物馆热度</h3>
            <small>近 30 天预约单量</small>
          </div>
        </div>
        <ChartPanel :option="heatOption" :height="280" />
      </div>
      <div class="admin-card">
        <div class="table-head">
          <div>
            <h3>票种分布</h3>
            <small>全部预约订单</small>
          </div>
        </div>
        <ChartPanel :option="pieOption" :height="280" />
      </div>
      <div class="admin-card saturation-card">
        <div class="table-head">
          <div>
            <h3>时段饱和度</h3>
            <small>未来 7 天热门时段</small>
          </div>
          <el-button text size="small" @click="$router.push('/admin/slots')">管理时段</el-button>
        </div>
        <div class="saturation-list">
          <div v-for="slot in saturation.slice(0, 5)" :key="slot.id" class="saturation-item">
            <div class="sat-head">
              <strong>{{ slot.museumName }}</strong>
              <span>{{ slot.visitDate }} {{ slot.startTime.slice(0, 5) }}</span>
              <em>{{ slot.saturation }}%</em>
            </div>
            <div class="progress-track thin">
              <i
                :style="{
                  width: `${Math.min(100, slot.saturation)}%`,
                  background:
                    slot.saturation >= 90
                      ? 'linear-gradient(90deg, #d29a94, #b0443f)'
                      : 'linear-gradient(90deg, #d5bb8b, #b08a4f)',
                }"
              ></i>
            </div>
          </div>
          <el-empty v-if="!saturation.length" description="暂无时段数据" :image-size="50" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  animation: floatUp 0.4s ease both;
}

.date-chip {
  padding: 8px 14px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fffdf9;
  color: var(--muted);
  font-size: 11px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 18px;
}

.kpi-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.kpi-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: 12px;
  font-size: 19px;
}

.kpi-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-copy > span {
  color: var(--muted);
  font-size: 11px;
}

.kpi-copy strong {
  color: var(--ink);
  font-size: 30px;
  font-weight: 500;
  line-height: 1.15;
}

.kpi-copy strong small {
  color: var(--muted);
  font-family: var(--font-sans);
  font-size: 10px;
  font-weight: 400;
}

.kpi-copy em {
  color: var(--muted);
  font-size: 9.5px;
  font-style: normal;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1.7fr 0.9fr;
  gap: 16px;
  margin-bottom: 16px;
}

.chart-grid.lower {
  grid-template-columns: 1fr 1fr 1.15fr;
}

.trend-card {
  min-width: 0;
}

.status-card {
  display: flex;
  flex-direction: column;
}

.progress-item {
  margin-bottom: 18px;
}

.progress-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8px;
}

.progress-head span {
  color: var(--text-soft);
  font-size: 11px;
}

.progress-head strong {
  color: var(--ink);
  font-size: 22px;
  font-weight: 500;
}

.progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #f0e9dd;
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s ease;
}

.progress-item > small {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  font-size: 9.5px;
}

.quick-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: auto;
  padding: 14px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

.quick-stats div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  border-right: 1px solid var(--line);
}

.quick-stats div:last-child {
  border-right: none;
}

.quick-stats span {
  color: var(--muted);
  font-size: 9px;
}

.quick-stats strong {
  color: var(--ink);
  font-size: 20px;
  font-weight: 500;
}

.checkin-link {
  width: 100%;
  margin-top: 14px;
}

.saturation-card {
  min-width: 0;
}

.saturation-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sat-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 10px;
}

.sat-head strong {
  max-width: 42%;
  overflow: hidden;
  color: var(--text);
  font-size: 11px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sat-head span {
  color: var(--muted);
  font-size: 9px;
}

.sat-head em {
  margin-left: auto;
  color: var(--gold-deep);
  font-size: 10px;
  font-style: normal;
}

.progress-track.thin {
  height: 5px;
}

@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-grid.lower {
    grid-template-columns: 1fr 1fr;
  }

  .saturation-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }

  .status-card {
    min-height: auto;
  }
}

@media (max-width: 560px) {
  .kpi-grid,
  .chart-grid.lower {
    grid-template-columns: 1fr;
  }
}
</style>
