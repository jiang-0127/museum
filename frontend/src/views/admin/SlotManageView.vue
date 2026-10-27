<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import {
  adminExhibitions,
  adminMuseums,
  adminSlots,
  createSlot,
  deleteSlot,
  updateSlot,
} from '@/api/admin'
import type { Exhibition, Museum, Slot } from '@/types'

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

const emptyForm = () => ({
  museumId: undefined as number | undefined,
  exhibitionId: undefined as number | undefined,
  visitDate: dayjs().format('YYYY-MM-DD'),
  startTime: '09:00:00',
  endTime: '11:00:00',
  totalCapacity: 200,
  status: 1,
})

const form = reactive(emptyForm())

const rules: FormRules = {
  museumId: [{ required: true, message: '请选择博物馆', trigger: 'change' }],
  visitDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  totalCapacity: [{ required: true, message: '请输入总容量', trigger: 'blur' }],
}

const formExhibitions = computed(() =>
  exhibitions.value.filter((item) => !form.museumId || item.museumId === form.museumId),
)

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

function phaseHint(row: Slot) {
  const phase = slotPhase(row)
  if (phase === 'ended') return '时段已结束，系统已自动关闭'
  if (phase === 'ongoing') return '时段正在进行中，系统已自动关闭'
  return ''
}

function displayStatus(row: Slot) {
  return slotPhase(row) === 'upcoming' && row.status === 1 ? '开放' : '关闭'
}

const formStarted = computed(() => {
  if (!form.visitDate || !form.startTime) return false
  return !dayjs(`${form.visitDate} ${form.startTime}`).isAfter(dayjs())
})

async function load() {
  loading.value = true
  try {
    const data = await adminSlots({
      page: page.value,
      size,
      museumId: museumId.value,
      date: date.value || undefined,
      status: status.value,
    })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  if (museumId.value) form.museumId = museumId.value
  dialogVisible.value = true
}

function openEdit(row: Slot) {
  editingId.value = row.id
  Object.assign(form, {
    museumId: row.museumId,
    exhibitionId: row.exhibitionId,
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
  if (form.endTime <= form.startTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  saving.value = true
  try {
    const payload: Record<string, unknown> = { ...form, exhibitionId: form.exhibitionId || null }
    if (!dayjs(`${form.visitDate} ${form.startTime}`).isAfter(dayjs())) {
      payload.status = 0
    }
    if (editingId.value) {
      await updateSlot(editingId.value, payload)
      ElMessage.success('时段已更新')
    } else {
      await createSlot(payload)
      ElMessage.success('时段已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Slot) {
  try {
    await ElMessageBox.confirm(
      `确认删除 ${row.visitDate} ${row.startTime.slice(0, 5)} 的时段吗？已有预约的时段不能删除。`,
      '删除时段',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
    )
    await deleteSlot(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    // 取消
  }
}

async function toggleStatus(row: Slot) {
  if (slotPhase(row) !== 'upcoming') {
    ElMessage.info('已结束或正在进行的时段不能重新开放')
    return
  }
  try {
    await updateSlot(row.id, {
      museumId: row.museumId,
      exhibitionId: row.exhibitionId || null,
      visitDate: row.visitDate,
      startTime: row.startTime,
      endTime: row.endTime,
      totalCapacity: row.totalCapacity,
      status: row.status === 1 ? 0 : 1,
    })
    ElMessage.success(row.status === 1 ? '时段已关闭' : '时段已开放')
    load()
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
})
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="CAPACITY & SCHEDULE"
      title="时段与票额"
      desc="设置每日参观时段与总容量，并结合票种与预约记录展示可售票种和预约情况。已结束或正在进行的时段会自动关闭，取消预约会自动释放票额。"
    >
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增时段
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
        <el-select v-model="status" placeholder="全部状态" clearable @change="search">
          <el-option label="开放" :value="1" />
          <el-option label="关闭" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 个时段</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="museumName" label="博物馆" min-width="150" />
        <el-table-column label="参观日期" width="120">
          <template #default="{ row }">
            <span>{{ dayjs(row.visitDate).format('MM.DD') }}</span>
            <small class="weekday">{{ ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dayjs(row.visitDate).day()] }}</small>
          </template>
        </el-table-column>
        <el-table-column label="时段" width="130">
          <template #default="{ row }">
            {{ row.startTime.slice(0, 5) }} - {{ row.endTime.slice(0, 5) }}
          </template>
        </el-table-column>
        <el-table-column prop="exhibitionName" label="关联展览" width="140">
          <template #default="{ row }">{{ row.exhibitionName || '常设展' }}</template>
        </el-table-column>
        <el-table-column label="可售票种" width="110" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="row.ticketTypeNames?.length" placement="top" effect="light">
              <template #content>
                <div class="ticket-tip">
                  <div v-for="name in row.ticketTypeNames" :key="name">{{ name }}</div>
                </div>
              </template>
              <span class="stat-link">{{ row.ticketTypeCount }} 个</span>
            </el-tooltip>
            <span v-else class="stat-empty">—</span>
          </template>
        </el-table-column>
        <el-table-column label="预约记录" width="110" align="center">
          <template #default="{ row }">
            <span class="stat-text">{{ row.reservationOrderCount ?? 0 }} 单</span>
            <small class="stat-sub">{{ row.reservationVisitorCount ?? 0 }} 人</small>
          </template>
        </el-table-column>
        <el-table-column label="票额" width="170">
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
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tooltip :disabled="slotPhase(row) === 'upcoming'" :content="phaseHint(row)" placement="top">
              <el-tag :type="displayStatus(row) === '开放' ? 'success' : 'info'" effect="plain" round>
                {{ displayStatus(row) }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <div class="op-actions">
              <el-button text size="small" @click="openEdit(row)">编辑</el-button>
              <el-button
                text
                size="small"
                :disabled="slotPhase(row) !== 'upcoming'"
                @click="toggleStatus(row)"
              >
                {{ slotPhase(row) !== 'upcoming' ? '已关闭' : row.status === 1 ? '关闭' : '开放' }}
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑时段' : '新增时段'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-cols">
          <el-form-item label="博物馆" prop="museumId">
            <el-select
              v-model="form.museumId"
              placeholder="请选择博物馆"
              style="width: 100%"
              @change="form.exhibitionId = undefined"
            >
              <el-option v-for="museum in museums" :key="museum.id" :label="museum.name" :value="museum.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联展览（选填）">
            <el-select v-model="form.exhibitionId" placeholder="不关联则视为常设展" clearable style="width: 100%">
              <el-option v-for="item in formExhibitions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="参观日期" prop="visitDate">
            <el-date-picker
              v-model="form.visitDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="总容量" prop="totalCapacity">
            <el-input-number v-model="form.totalCapacity" :min="1" :max="10000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="开始时间" prop="startTime">
            <el-time-picker
              v-model="form.startTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="开始时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-time-picker
              v-model="form.endTime"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="结束时间"
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
        <p class="form-tip">
          <el-icon><InfoFilled /></el-icon>
          编辑已有预约的时段时，不能修改日期、时间或把容量降到已预约人数以下。已开始或已结束的时段会自动置为关闭。
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
  padding-right: 22px;
}

.cap-text {
  display: flex;
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
  margin-left: auto;
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

.form-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
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

@media (max-width: 600px) {
  .form-cols {
    grid-template-columns: 1fr;
  }
}
</style>
