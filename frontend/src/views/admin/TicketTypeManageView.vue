<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  adminExhibitions,
  adminMuseums,
  adminTicketTypes,
  createTicketType,
  deleteTicketType,
  updateTicketType,
} from '@/api/admin'
import { audienceLabels } from '@/utils/ticket'
import type { Exhibition, Museum, TicketType } from '@/types'

const rows = ref<TicketType[]>([])
const museums = ref<Museum[]>([])
const exhibitions = ref<Exhibition[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const museumId = ref<number | undefined>(undefined)
const status = ref<number | undefined>(undefined)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const emptyForm = () => ({
  museumId: undefined as number | undefined,
  name: '',
  price: 0,
  ticketKind: 'BASIC' as 'BASIC' | 'SINGLE' | 'COMBO',
  exhibitionId: undefined as number | undefined,
  audienceType: 'GENERAL' as 'GENERAL' | 'ADULT' | 'STUDENT' | 'FAMILY',
  tagline: '',
  audience: '',
  description: '',
  benefits: '',
  rules: '',
  status: 1,
  sort: 0,
})

const form = reactive(emptyForm())

const rules: FormRules = {
  museumId: [{ required: true, message: '请选择博物馆', trigger: 'change' }],
  name: [{ required: true, message: '请输入票种名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const data = await adminTicketTypes({
      page: page.value,
      size,
      museumId: museumId.value,
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

function museumName(id: number) {
  return museums.value.find((museum) => museum.id === id)?.name || '—'
}

const kindLabels: Record<string, string> = {
  BASIC: '基础入馆',
  SINGLE: '单展票',
  COMBO: '全馆联票',
}

function kindLabel(value?: string) {
  return kindLabels[value || 'BASIC'] || '基础入馆'
}

function audienceLabel(value?: string) {
  return audienceLabels[(value || 'GENERAL') as keyof typeof audienceLabels] || '通用'
}

async function loadExhibitions() {
  if (!form.museumId) {
    exhibitions.value = []
    return
  }
  const data = await adminExhibitions({ museumId: form.museumId, page: 1, size: 100 })
  exhibitions.value = data.records
}

async function onMuseumChange() {
  form.exhibitionId = undefined
  await loadExhibitions()
}

watch(
  () => form.ticketKind,
  (kind) => {
    if (kind !== 'SINGLE') form.exhibitionId = undefined
    if (kind === 'BASIC') form.audienceType = 'GENERAL'
  },
)

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  if (museumId.value) form.museumId = museumId.value
  dialogVisible.value = true
  loadExhibitions()
}

function openEdit(row: TicketType) {
  editingId.value = row.id
  Object.assign(form, {
    museumId: row.museumId,
    name: row.name,
    price: Number(row.price),
    ticketKind: row.ticketKind || 'BASIC',
    exhibitionId: row.exhibitionId || undefined,
    audienceType: row.audienceType || 'GENERAL',
    tagline: row.tagline || '',
    audience: row.audience || '',
    description: row.description || '',
    benefits: row.benefits || '',
    rules: row.rules || '',
    status: row.status,
    sort: row.sort,
  })
  dialogVisible.value = true
  loadExhibitions()
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.ticketKind === 'SINGLE' && !form.exhibitionId) {
    ElMessage.warning('单展票请选择对应展览')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateTicketType(editingId.value, { ...form })
      ElMessage.success('票种已更新')
    } else {
      await createTicketType({ ...form })
      ElMessage.success('票种已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: TicketType) {
  try {
    await ElMessageBox.confirm(
      `确认删除票种「${row.name}」吗？存在有效预约时不能删除，普通预约票只能停用。`,
      '删除票种',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await deleteTicketType(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    // 取消或后端拒绝
  }
}

async function toggleStatus(row: TicketType) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await updateTicketType(row.id, {
      museumId: row.museumId,
      name: row.name,
      price: Number(row.price),
      ticketKind: row.ticketKind || 'BASIC',
      exhibitionId: row.exhibitionId || undefined,
      audienceType: row.audienceType || 'GENERAL',
      tagline: row.tagline || '',
      audience: row.audience || '',
      description: row.description || '',
      benefits: row.benefits || '',
      rules: row.rules || '',
      status: nextStatus,
      sort: row.sort,
    })
    ElMessage.success(nextStatus === 1 ? '已启用该票种' : '已停用该票种')
    load()
  } catch {
    // 错误已提示
  }
}

function changePage(value: number) {
  page.value = value
  load()
}

onMounted(async () => {
  const data = await adminMuseums({ page: 1, size: 100 })
  museums.value = data.records
  load()
})
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="TICKET TYPES"
      title="票种管理"
      desc="配置基础入馆票、单展票与全馆联票。停用后记录保留并显示为停用；删除后不再展示。博物馆停用期间不能新增票种，展览停用期间不能新增或启用单展票。"
    >
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增票种
      </el-button>
    </PageHeader>

    <div class="admin-toolbar">
      <div class="toolbar-filters">
        <el-select v-model="museumId" placeholder="全部博物馆" clearable @change="search">
          <el-option v-for="museum in museums" :key="museum.id" :label="museum.name" :value="museum.id" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable @change="search">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 个票种</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="票种名称" min-width="200">
          <template #default="{ row }">
            <div class="ticket-name-cell">
              <strong>{{ row.name }}</strong>
              <span v-if="row.tagline" class="tagline">{{ row.tagline }}</span>
              <small v-if="row.audience">{{ row.audience }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属博物馆" width="160">
          <template #default="{ row }">{{ museumName(row.museumId) }}</template>
        </el-table-column>
        <el-table-column label="票种类型" width="110">
          <template #default="{ row }">
            <el-tag effect="plain" round>{{ kindLabel(row.ticketKind) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="适用人群" width="100">
          <template #default="{ row }">{{ audienceLabel(row.audienceType) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="130">
          <template #default="{ row }">
            <strong class="price" :class="{ free: Number(row.price) === 0 }">
              {{ Number(row.price) === 0 ? '免费' : `¥${Number(row.price).toFixed(2)}` }}
            </strong>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" round>
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button text size="small" type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑票种' : '新增票种'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-cols">
          <el-form-item label="所属博物馆" prop="museumId">
            <el-select v-model="form.museumId" placeholder="请选择" style="width: 100%" @change="onMuseumChange">
              <el-option
                v-for="museum in museums"
                :key="museum.id"
                :label="museum.status === 1 ? museum.name : `${museum.name}（已停用）`"
                :value="museum.id"
                :disabled="museum.status !== 1"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item label="票种类型">
            <el-select v-model="form.ticketKind" style="width: 100%">
              <el-option label="普通预约票（基础入馆）" value="BASIC" />
              <el-option label="单展票（基础入馆 + 单展）" value="SINGLE" />
              <el-option label="全馆联票（基础入馆 + 全馆展览）" value="COMBO" />
            </el-select>
          </el-form-item>
          <el-form-item label="适用人群">
            <el-select v-model="form.audienceType" :disabled="form.ticketKind === 'BASIC'" style="width: 100%">
              <el-option label="通用" value="GENERAL" />
              <el-option label="成人票" value="ADULT" />
              <el-option label="学生票" value="STUDENT" />
              <el-option label="亲子票" value="FAMILY" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.ticketKind === 'SINGLE'" label="对应展览">
            <el-select v-model="form.exhibitionId" placeholder="请选择展览" style="width: 100%">
              <el-option
                v-for="exhibition in exhibitions"
                :key="exhibition.id"
                :label="exhibition.status === 1 ? exhibition.name : `${exhibition.name}（已停用）`"
                :value="exhibition.id"
                :disabled="exhibition.status !== 1"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="票种名称" prop="name">
            <el-input v-model="form.name" placeholder="如：单展票 · 千里江山特展" />
          </el-form-item>
          <el-form-item label="价格（元）" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" style="width: 100%" />
          </el-form-item>
        </div>
        <div class="form-cols">
          <el-form-item label="票种标签">
            <el-input v-model="form.tagline" placeholder="如：基础入馆、特展畅游" maxlength="40" />
          </el-form-item>
          <el-form-item label="适用人群">
            <el-input v-model="form.audience" placeholder="如：全日制在校学生" maxlength="150" />
          </el-form-item>
        </div>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="一句话说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="包含权益">
          <el-input
            v-model="form.benefits"
            type="textarea"
            :rows="4"
            maxlength="800"
            show-word-limit
            placeholder="每行一项，例如：常设展与基本陈列"
          />
        </el-form-item>
        <el-form-item label="使用规则">
          <el-input
            v-model="form.rules"
            type="textarea"
            :rows="4"
            maxlength="800"
            show-word-limit
            placeholder="每行一项，例如：入馆须携带本人有效身份证件"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.total-note {
  color: var(--muted);
  font-size: 11px;
}

.ticket-name-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.ticket-name-cell strong {
  color: var(--text);
  font-size: 13px;
}

.ticket-name-cell .tagline {
  width: fit-content;
  padding: 1px 7px;
  border: 1px solid #e8dcc4;
  border-radius: 999px;
  background: #fbf5e8;
  color: var(--gold-deep);
  font-size: 9px;
}

.ticket-name-cell small {
  color: var(--muted);
  font-size: 10px;
}

.price {
  color: var(--gold-deep);
  font-size: 13px;
  font-weight: 600;
}

.price.free {
  color: var(--success);
  font-size: 12px;
}

.form-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

@media (max-width: 600px) {
  .form-cols {
    grid-template-columns: 1fr;
  }
}
</style>
