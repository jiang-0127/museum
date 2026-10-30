<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import {
  adminExhibitions,
  adminMuseums,
  adminSlots,
  createSlotBatch,
  deleteSlot,
  updateSlot,
} from '@/api/admin'
import { getBasicQuota } from '@/api/public'
import type { Exhibition, Museum, Slot } from '@/types'
import { isClosedOn } from '@/utils/schedule'

const rows = ref<Slot[]>([])
const museums = ref<Museum[]>([])
const exhibitions = ref<Exhibition[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const museumId = ref<number | undefined>(undefined)
const date = ref(dayjs().format('YYYY-MM-DD'))
const status = ref<number | undefined>(undefined)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const quotaSummary = ref<{
  daily: number
  used: number
  count: number
  basic: number
  booked: number
  remaining: number
  orderCount: number
  visitorCount: number
  closed: boolean
} | null>(null)

const today = dayjs().format('YYYY-MM-DD')
const emptyForm = () => ({
  museumId: undefined as number | undefined,
  dateRange: [today, today] as [string, string],
  visitDate: today,
  startTime: '09:00:00',
  endTime: '11:00:00',
  totalCapacity: 200,
  status: 1,
})

const form = reactive(emptyForm())

const rules: FormRules = {
  museumId: [{ required: true, message: '请选择博物馆', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  totalCapacity: [{ required: true, message: '请输入总容量', trigger: 'blur' }],
}

const formStarted = computed(() => {
  const d = editingId.value ? form.visitDate : form.dateRange[0]
  if (!d || !form.startTime) return false
  return !dayjs(`${d} ${form.startTime}`).isAfter(dayjs())
})

const museumDateBounds = computed(() => {
  if (!form.museumId) return null
  const list = exhibitions.value.filter(
    (item) => item.museumId === form.museumId && item.status === 1,
  )
  if (!list.length) return null
  const starts = list.map((item) => dayjs(item.startDate))
  const ends = list.map((item) => dayjs(item.endDate))
  return {
    start: starts.reduce((a, b) => (a.isBefore(b) ? a : b)),
    end: ends.reduce((a, b) => (a.isAfter(b) ? a : b)),
  }
})

const selectedMuseum = computed(
  () => museums.value.find((item) => item.id === form.museumId) || null,
)

const selectedMuseumDailyCapacity = computed(() => selectedMuseum.value?.dailyCapacity ?? 0)

const museumOpenTime = computed(() => selectedMuseum.value?.openTime?.slice(0, 5) || '')

const museumCloseTime = computed(() => selectedMuseum.value?.closeTime?.slice(0, 5) || '')

const selectedDateClosed = computed(() => {
  if (!museumId.value || !date.value) return false
  const museum = museums.value.find((item) => item.id === museumId.value)
  return isClosedOn(museum?.closedDays, date.value)
})

function isRowClosed(row: Slot) {
  const museum = museums.value.find((item) => item.id === row.museumId)
  return isClosedOn(museum?.closedDays, row.visitDate)
}

function hasOpenExhibition(museumId: number | undefined, visitDate: string | undefined) {
  if (!museumId || !visitDate) return false
  return exhibitions.value.some(
    (item) =>
      item.museumId === museumId &&
      item.status === 1 &&
      item.startDate <= visitDate &&
      item.endDate >= visitDate,
  )
}

function rowStatus(row: Slot) {
  if (isRowClosed(row)) return '关闭'
  return slotPhase(row) === 'upcoming' && row.status === 1 ? '开放' : '关闭'
}

function isEditable(row: Slot) {
  return !isRowClosed(row) && slotPhase(row) === 'upcoming'
}

function statusHint(row: Slot) {
  if (isRowClosed(row)) return '博物馆每周闭馆日，暂停预约且不可修改'
  const phase = slotPhase(row)
  if (phase === 'ended') return '入馆批次已结束，不可修改'
  if (phase === 'ongoing') return '入馆批次正在进行中，不可修改'
  if (row.status !== 1 && !hasOpenExhibition(row.museumId, row.visitDate)) {
    return '该日期没有开放中的展览，不能开放此分时批次'
  }
  return row.status === 1 ? '当前开放预约' : '已关闭，暂不可预约'
}

function parseHM(value: string): [number, number] | null {
  if (!value) return null
  const [h, m] = value.split(':').map(Number)
  if (Number.isNaN(h) || Number.isNaN(m)) return null
  return [h, m]
}

function isPastDate(d: Date) {
  return dayjs(d).isBefore(dayjs(), 'day')
}

function disabledDate(d: Date) {
  if (isPastDate(d)) return true
  const museum = selectedMuseum.value
  if (museum && isClosedOn(museum.closedDays, dayjs(d).format('YYYY-MM-DD'))) return true
  const bounds = museumDateBounds.value
  if (bounds) {
    const target = dayjs(d)
    if (target.isBefore(bounds.start, 'day') || target.isAfter(bounds.end, 'day')) return true
  }
  return false
}

function selectedStartDate() {
  return editingId.value ? form.visitDate : form.dateRange?.[0]
}

function disabledStartHours() {
  const open = parseHM(museumOpenTime.value)
  const close = parseHM(museumCloseTime.value)
  const d = selectedStartDate()
  const todayHour = d && dayjs(d).isSame(dayjs(), 'day') ? dayjs().hour() : -1
  const disabled = new Set<number>()
  for (let hour = 0; hour < 24; hour += 1) {
    if (open && hour < open[0]) disabled.add(hour)
    if (close && hour > close[0]) disabled.add(hour)
    if (close && hour === close[0] && close[1] === 0) disabled.add(hour)
    if (todayHour >= 0 && hour < todayHour) disabled.add(hour)
  }
  return Array.from(disabled)
}

function disabledStartMinutes(hour: number) {
  const open = parseHM(museumOpenTime.value)
  const close = parseHM(museumCloseTime.value)
  const d = selectedStartDate()
  const disabled = new Set<number>()
  for (let minute = 0; minute < 60; minute += 1) {
    if (open && hour === open[0] && minute < open[1]) disabled.add(minute)
    if (close && hour === close[0] && minute >= close[1]) disabled.add(minute)
    if (d && dayjs(d).isSame(dayjs(), 'day') && hour === dayjs().hour() && minute <= dayjs().minute()) {
      disabled.add(minute)
    }
  }
  return Array.from(disabled)
}

function disabledEndHours() {
  const open = parseHM(museumOpenTime.value)
  const close = parseHM(museumCloseTime.value)
  const start = parseHM(form.startTime)
  const disabled = new Set<number>()
  for (let hour = 0; hour < 24; hour += 1) {
    if (open && hour < open[0]) disabled.add(hour)
    if (close && hour > close[0]) disabled.add(hour)
    if (start && hour < start[0]) disabled.add(hour)
  }
  return Array.from(disabled)
}

function disabledEndMinutes(hour: number) {
  const open = parseHM(museumOpenTime.value)
  const close = parseHM(museumCloseTime.value)
  const start = parseHM(form.startTime)
  const disabled = new Set<number>()
  for (let minute = 0; minute < 60; minute += 1) {
    if (open && hour === open[0] && minute < open[1]) disabled.add(minute)
    if (close && hour === close[0] && minute > close[1]) disabled.add(minute)
    if (start && hour === start[0] && minute <= start[1]) disabled.add(minute)
  }
  return Array.from(disabled)
}

function saturation(row: Slot) {
  if (!row.totalCapacity) return 0
  return Math.min(100, Math.round((row.bookedCount / row.totalCapacity) * 100))
}

type SlotPhase = 'upcoming' | 'ongoing' | 'ended'

function slotPhase(row: Slot): SlotPhase {
  const start = dayjs(`${row.visitDate} ${row.startTime}`)
  const end = dayjs(`${row.visitDate} ${row.endTime}`)
  const now = dayjs()
  if (!now.isBefore(end)) return 'ended'
  if (!now.isBefore(start)) return 'ongoing'
  return 'upcoming'
}

async function load() {
  loading.value = true
  try {
    const data = await adminSlots({
      page: page.value,
      size,
      museumId: museumId.value,
      date: date.value || undefined,
      status: selectedDateClosed.value ? undefined : status.value,
    })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadQuotaSummary() {
  if (!museumId.value || !date.value) {
    quotaSummary.value = null
    return
  }
  try {
    const data = await getBasicQuota(museumId.value, date.value)
    quotaSummary.value = {
      daily: data.dailyCapacity,
      used: data.slotCapacitySum,
      count: data.slotCount,
      basic: data.basicQuota,
      booked: data.basicBooked,
      remaining: data.basicRemaining,
      orderCount: data.basicOrderCount,
      visitorCount: data.basicBooked,
      closed: data.closed,
    }
  } catch {
    quotaSummary.value = null
  }
}

function basicSaturation() {
  if (!quotaSummary.value || !quotaSummary.value.basic) return 0
  return Math.min(100, Math.round((quotaSummary.value.booked / quotaSummary.value.basic) * 100))
}

function search() {
  page.value = 1
  load()
  loadQuotaSummary()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  if (museumId.value) form.museumId = museumId.value
  dialogVisible.value = true
}

function openEdit(row: Slot) {
  if (!isEditable(row)) {
    ElMessage.warning(
      isRowClosed(row)
        ? '博物馆闭馆日不能修改分时批次'
        : '已开始或已结束的分时批次不能修改',
    )
    return
  }
  editingId.value = row.id
  Object.assign(form, {
    museumId: row.museumId,
    dateRange: [row.visitDate, row.visitDate],
    visitDate: row.visitDate,
    startTime: row.startTime,
    endTime: row.endTime,
    totalCapacity: row.totalCapacity,
    status: row.status,
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const targetDate = editingId.value ? form.visitDate : form.dateRange?.[0]
  if (
    form.status === 1 &&
    !hasOpenExhibition(form.museumId, targetDate)
  ) {
    ElMessage.warning('该日期没有开放中的展览，分时批次不能开放')
    return
  }
  if (selectedMuseum.value && targetDate && isClosedOn(selectedMuseum.value.closedDays, targetDate)) {
    ElMessage.warning('该日期为博物馆每周闭馆日，不能新增或修改分时批次')
    return
  }
  if (form.endTime <= form.startTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  if (
    (museumOpenTime.value && form.startTime.slice(0, 5) < museumOpenTime.value) ||
    (museumCloseTime.value && form.endTime.slice(0, 5) > museumCloseTime.value)
  ) {
    ElMessage.warning(`分时批次需在博物馆开放时间 ${museumOpenTime.value} - ${museumCloseTime.value} 内`)
    return
  }
  if (!editingId.value) {
    if (!form.dateRange?.[0] || !form.dateRange?.[1]) {
      ElMessage.warning('请选择开始日期和结束日期')
      return
    }
    if (dayjs(form.dateRange[1]).isBefore(dayjs(form.dateRange[0]))) {
      ElMessage.warning('结束日期不能早于开始日期')
      return
    }
    if (isPastDate(new Date(form.dateRange[0]))) {
      ElMessage.warning('不能选择已过去的日期')
      return
    }
    const bounds = museumDateBounds.value
    if (bounds) {
      if (dayjs(form.dateRange[0]).isBefore(bounds.start, 'day') || dayjs(form.dateRange[1]).isAfter(bounds.end, 'day')) {
        ElMessage.warning(
          `开设日期需在 ${bounds.start.format('YYYY-MM-DD')} 至 ${bounds.end.format('YYYY-MM-DD')} 的展览周期内`,
        )
        return
      }
    }
    if (!dayjs(`${form.dateRange[0]} ${form.startTime}`).isAfter(dayjs())) {
      ElMessage.warning('开始时间必须晚于当前时间')
      return
    }
  }
  saving.value = true
  try {
    if (editingId.value) {
      const payload: Record<string, unknown> = {
        museumId: form.museumId,
        exhibitionId: null,
        visitDate: form.visitDate,
        startTime: form.startTime,
        endTime: form.endTime,
        totalCapacity: form.totalCapacity,
        status: form.status,
      }
      if (!dayjs(`${form.visitDate} ${form.startTime}`).isAfter(dayjs())) {
        payload.status = 0
      }
      await updateSlot(editingId.value, payload)
      ElMessage.success('分时批次已更新')
    } else {
      const created = await createSlotBatch({
        museumId: form.museumId,
        startDate: form.dateRange[0],
        endDate: form.dateRange[1],
        startTime: form.startTime,
        endTime: form.endTime,
        totalCapacity: form.totalCapacity,
        status: 1,
      })
      ElMessage.success(created > 0 ? `已新增 ${created} 个分时批次` : '所选日期范围内没有新增入馆批次')
    }
    dialogVisible.value = false
    load()
    loadQuotaSummary()
  } finally {
    saving.value = false
  }
}

async function remove(row: Slot) {
  try {
    await ElMessageBox.confirm(
      `确认删除 ${row.visitDate} ${row.startTime.slice(0, 5)} 的分时批次吗？已有有效预约的批次不能删除。`,
      '删除入馆批次',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
    )
    await deleteSlot(row.id)
    ElMessage.success('已删除')
    load()
    loadQuotaSummary()
  } catch {
    // 取消
  }
}

async function toggleStatus(row: Slot) {
  if (!isEditable(row)) {
    ElMessage.warning(
      isRowClosed(row)
        ? '博物馆闭馆日不能修改分时批次状态'
        : '已开始或已结束的分时批次不能修改状态',
    )
    return
  }
  if (row.status !== 1 && !hasOpenExhibition(row.museumId, row.visitDate)) {
    ElMessage.warning('该日期没有开放中的展览，分时批次不能开放')
    return
  }
  try {
    await updateSlot(row.id, {
      museumId: row.museumId,
      exhibitionId: null,
      visitDate: row.visitDate,
      startTime: row.startTime,
      endTime: row.endTime,
      totalCapacity: row.totalCapacity,
      status: row.status === 1 ? 0 : 1,
    })
    ElMessage.success(row.status === 1 ? '分时批次已关闭' : '分时批次已开放')
    load()
    loadQuotaSummary()
  } catch {
    // 错误提示
  }
}

function changePage(value: number) {
  page.value = value
  load()
}

onMounted(async () => {
  const [museumData, exhibitionData] = await Promise.all([
    adminMuseums({ page: 1, size: 100 }),
    adminExhibitions({ page: 1, size: 100 }),
  ])
  museums.value = museumData.records
  exhibitions.value = exhibitionData.records
  load()
  loadQuotaSummary()
})
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="CAPACITY & SCHEDULE"
      title="入馆批次与票额"
      desc="普通票免选入馆批次，额度 = 日容量 − 当天分时批次容量之和；分时批次时间需在博物馆开放时间内。"
    >
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增分时批次
      </el-button>
    </PageHeader>

    <div class="admin-toolbar">
      <div class="toolbar-filters">
        <el-select v-model="museumId" placeholder="全部博物馆" clearable @change="search">
          <el-option v-for="museum in museums" :key="museum.id" :label="museum.name" :value="museum.id" />
        </el-select>
        <el-date-picker
          v-model="date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          clearable
          @change="search"
        />
        <el-select v-model="status" placeholder="全部状态" clearable :disabled="selectedDateClosed" @change="search">
          <el-option label="开放" :value="1" />
          <el-option label="关闭" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 个分时批次</span>
    </div>

    <div v-if="selectedDateClosed" class="closed-banner">
      <el-icon><InfoFilled /></el-icon>
      该日期为博物馆每周闭馆日，暂停预约，批次状态不可修改
    </div>

    <div class="quota-overview">
      <div class="quota-card quota-basic">
        <div class="quota-card-head">
          <span class="quota-label">普通票</span>
          <div class="quota-tags">
            <el-tag v-if="quotaSummary?.closed" size="small" type="info" effect="plain" round>闭馆日</el-tag>
            <el-tag size="small" effect="plain" round>免选入馆批次</el-tag>
          </div>
        </div>
        <template v-if="quotaSummary">
          <strong class="quota-value">{{ quotaSummary.closed ? 0 : quotaSummary.basic }}<small>张</small></strong>
          <div v-if="quotaSummary.closed" class="quota-formula closed-formula">
            博物馆每周闭馆日，当天暂停预约
          </div>
          <div v-else class="quota-formula">
            日容量 {{ quotaSummary.daily }} − 分时批次 {{ quotaSummary.used }} = 普通票额度
            {{ quotaSummary.basic }}
          </div>
          <div class="quota-metrics">
            <div class="quota-metric">
              <span>预约记录</span>
              <strong>{{ quotaSummary.orderCount }} 单</strong>
              <small>{{ quotaSummary.visitorCount }} 人</small>
            </div>
            <div class="quota-metric">
              <span>票额</span>
              <template v-if="quotaSummary.closed">
                <strong>暂停预约</strong>
                <small>闭馆日无票</small>
              </template>
              <template v-else>
                <strong>{{ quotaSummary.booked }} / {{ quotaSummary.basic }}</strong>
                <small>余 {{ quotaSummary.remaining }}</small>
              </template>
            </div>
          </div>
          <div v-if="!quotaSummary.closed" class="progress-track thin quota-progress">
            <i
              :style="{
                width: `${basicSaturation()}%`,
                background:
                  basicSaturation() >= 90
                    ? 'linear-gradient(90deg, #d29a94, #b0443f)'
                    : 'linear-gradient(90deg, #d5bb8b, #b08a4f)',
              }"
            ></i>
          </div>
        </template>
        <div v-else class="quota-empty">选择博物馆和日期后显示普通票额度</div>
        <p class="quota-desc">
          {{ quotaSummary?.closed ? '闭馆日不开放预约，恢复开馆后可继续预约。' : '开放时间内任意时间入馆，不占用具体入馆批次。' }}
        </p>
      </div>

      <div class="quota-card quota-timed">
        <div class="quota-card-head">
          <span class="quota-label">分时入馆批次</span>
          <div class="quota-tags">
            <el-tag v-if="quotaSummary?.closed" size="small" type="info" effect="plain" round>闭馆日</el-tag>
            <el-tag size="small" type="warning" effect="plain" round>有特定入馆时间</el-tag>
          </div>
        </div>
        <template v-if="quotaSummary">
          <strong class="quota-value">{{ quotaSummary.closed ? 0 : quotaSummary.used }}<small>张</small></strong>
          <div v-if="quotaSummary.closed" class="quota-formula closed-formula">
            博物馆每周闭馆日，分时批次暂停预约
          </div>
          <div v-else class="quota-formula">
            当天分时批次 {{ quotaSummary.count }} 个，容量已从普通票额度中划出
          </div>
        </template>
        <div v-else class="quota-empty">选择博物馆和日期后统计当天分时批次</div>
        <p class="quota-desc">
          {{ quotaSummary?.closed ? '批次仍可查看，闭馆日无法预约或修改状态。' : '按具体入馆时间核验，占用对应批次票额。' }}
        </p>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="museumName" label="博物馆" min-width="120" />
        <el-table-column label="参观日期" width="100">
          <template #default="{ row }">
            <div class="date-cell">
              <span>{{ dayjs(row.visitDate).format('MM.DD') }}</span>
              <small class="weekday">{{ ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dayjs(row.visitDate).day()] }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="入馆时间" width="115">
          <template #default="{ row }">
            {{ row.startTime.slice(0, 5) }} - {{ row.endTime.slice(0, 5) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tooltip :content="statusHint(row)" placement="top">
              <el-tag :type="rowStatus(row) === '开放' ? 'success' : 'info'" effect="plain" round>
                {{ rowStatus(row) }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="预约记录" width="95" align="center">
          <template #default="{ row }">
            <span class="stat-text">{{ row.reservationOrderCount ?? 0 }} 单</span>
            <small class="stat-sub">{{ row.reservationVisitorCount ?? 0 }} 人</small>
          </template>
        </el-table-column>
        <el-table-column label="票额" min-width="130">
          <template #default="{ row }">
            <div class="capacity-cell">
              <div class="cap-text">
                <strong>{{ row.bookedCount }}</strong> / {{ row.totalCapacity }}
                <span>余 {{ row.remaining }}</span>
              </div>
              <div class="progress-track thin">
                <i
                  :style="{
                    width: `${saturation(row)}%`,
                    background:
                      saturation(row) >= 90
                        ? 'linear-gradient(90deg, #d29a94, #b0443f)'
                        : 'linear-gradient(90deg, #d5bb8b, #b08a4f)',
                  }"
                ></i>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="op-actions">
              <el-button text size="small" :disabled="!isEditable(row)" @click="openEdit(row)">编辑</el-button>
              <el-button
                text
                size="small"
                :disabled="!isEditable(row) || (row.status !== 1 && !hasOpenExhibition(row.museumId, row.visitDate))"
                @click="toggleStatus(row)"
              >
                {{ isRowClosed(row) ? '闭馆' : slotPhase(row) !== 'upcoming' ? '已关闭' : row.status === 1 ? '关闭' : '开放' }}
              </el-button>
              <el-button text size="small" type="danger" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          background
          @current-change="changePage"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分时批次' : '新增分时批次'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-cols">
          <el-form-item label="博物馆" prop="museumId">
            <el-select v-model="form.museumId" placeholder="请选择博物馆" style="width: 100%">
              <el-option
                v-for="museum in museums"
                :key="museum.id"
                :label="museum.status === 1 ? museum.name : `${museum.name}（已停用）`"
                :value="museum.id"
                :disabled="museum.status !== 1"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-if="editingId" label="参观日期">
            <el-date-picker
              v-model="form.visitDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
              disabled
            />
          </el-form-item>
          <el-form-item v-else label="参观日期范围" class="span-2">
            <el-date-picker
              v-model="form.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :disabled-date="disabledDate"
              style="width: 100%"
            />
            <span v-if="museumDateBounds" class="range-hint">
              可开设日期：{{ museumDateBounds.start.format('YYYY-MM-DD') }} 至
              {{ museumDateBounds.end.format('YYYY-MM-DD') }}（该馆展览周期）
            </span>
          </el-form-item>
          <el-form-item label="总容量" prop="totalCapacity">
            <el-input-number v-model="form.totalCapacity" :min="1" :max="10000" style="width: 100%" />
            <span v-if="selectedMuseumDailyCapacity" class="range-hint">
              该馆日容量 {{ selectedMuseumDailyCapacity }}；普通票额度 = 日容量 − 当天分时批次容量之和
            </span>
          </el-form-item>
          <el-form-item label="开始时间" prop="startTime">
            <el-time-picker
              v-model="form.startTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="开始时间"
              :disabled-hours="disabledStartHours"
              :disabled-minutes="disabledStartMinutes"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-time-picker
              v-model="form.endTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="结束时间"
              :disabled-hours="disabledEndHours"
              :disabled-minutes="disabledEndMinutes"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="开放" :value="1" :disabled="formStarted" />
              <el-option label="关闭" :value="0" />
            </el-select>
          </el-form-item>
        </div>
        <p v-if="selectedMuseum" class="range-hint">
          博物馆开放时间 {{ museumOpenTime }} - {{ museumCloseTime }}，分时批次需在该时间段内。
        </p>
        <p class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          新增时可按开始、结束日期一次生成多天分时批次（已存在的日期会自动跳过）；编辑已有预约的分时批次时，不能修改日期、时间或把容量降到已预约人数以下。已开始或已结束的分时批次会自动置为关闭。停用的博物馆不能新增或启用批次；当天分时批次容量之和不能超过日容量。
        </p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.op-actions {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  white-space: nowrap;
}

.op-actions :deep(.el-button) {
  padding: 0;
}

.op-actions :deep(.el-button + .el-button) {
  margin-left: 8px;
}

.total-note {
  color: var(--muted);
  font-size: 11px;
}

.weekday {
  margin-left: 6px;
  color: var(--muted);
  font-size: 9px;
}

.date-cell {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.stat-link {
  color: var(--gold-deep);
  font-size: 11px;
  border-bottom: 1px dashed rgba(176, 138, 79, 0.5);
  cursor: help;
}

.stat-empty {
  color: var(--muted);
  font-size: 11px;
}

.stat-text {
  display: block;
  color: var(--ink);
  font-size: 12px;
}

.stat-sub {
  display: block;
  margin-top: 2px;
  color: var(--muted);
  font-size: 9.5px;
}

.ticket-tip {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 240px;
  overflow-y: auto;
  font-size: 11.5px;
  line-height: 1.5;
}

.capacity-cell {
  padding-right: 8px;
}

.cap-text {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 6px;
  color: var(--muted);
  font-size: 10px;
}

.cap-text strong {
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.cap-text span {
  color: var(--gold-deep);
  font-size: 9.5px;
}

.progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: #f0e9dd;
}

.progress-track.thin {
  height: 5px;
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.quota-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.quota-card {
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.quota-basic {
  border-color: #e8dcc4;
  background: linear-gradient(135deg, #fdf8ee, #fffdf9);
}

.quota-timed {
  background: linear-gradient(135deg, #f7f9fc, #fffdf9);
}

.quota-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.quota-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.closed-formula {
  color: var(--gold-deep);
}

.closed-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  margin-bottom: 14px;
  padding: 10px 14px;
  border: 1px solid rgba(176, 138, 79, 0.22);
  border-radius: 10px;
  background: rgba(176, 138, 79, 0.08);
  color: var(--gold-deep);
  font-size: 12px;
}

.quota-label {
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.quota-value {
  display: block;
  margin-top: 12px;
  color: var(--gold-deep);
  font-family: var(--font-serif);
  font-size: 32px;
  font-weight: 500;
  line-height: 1;
}

.quota-value small {
  margin-left: 4px;
  color: var(--muted);
  font-family: var(--font-sans);
  font-size: 11px;
  font-weight: 400;
}

.quota-formula {
  margin-top: 10px;
  color: var(--text-soft);
  font-size: 11px;
  line-height: 1.6;
}

.quota-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 14px;
}

.quota-metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.quota-metric span {
  color: var(--muted);
  font-size: 10px;
}

.quota-metric strong {
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.quota-metric small {
  color: var(--gold-deep);
  font-size: 9.5px;
}

.quota-progress {
  margin-top: 10px;
}

.quota-desc {
  margin-top: 8px;
  color: var(--muted);
  font-size: 10px;
  line-height: 1.6;
}

.quota-empty {
  margin-top: 12px;
  color: var(--muted);
  font-size: 11px;
}

.form-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.span-2 {
  grid-column: 1 / -1;
}

.range-hint {
  display: block;
  margin-top: 6px;
  color: var(--gold-deep);
  font-size: 10.5px;
  line-height: 1.6;
}

.form-tip {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  background: #f8f1e4;
  color: #8b754f;
  font-size: 10.5px;
  line-height: 1.7;
}

@media (max-width: 720px) {
  .quota-overview {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .form-cols {
    grid-template-columns: 1fr;
  }
}
</style>
