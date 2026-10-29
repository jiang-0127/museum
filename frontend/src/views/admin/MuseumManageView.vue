<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import {
  adminMuseums,
  createMuseum,
  deleteMuseum,
  updateMuseum,
} from '@/api/admin'
import type { Museum } from '@/types'
import { formatClosedDays, parseClosedDays } from '@/utils/schedule'

const rows = ref<Museum[]>([])
const total = ref(0)
const page = ref(1)
const size = 5
const keyword = ref('')
const status = ref<number | undefined>(undefined)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const editingWasEnabled = ref(false)
const formRef = ref<FormInstance>()

const emptyForm = () => ({
  name: '',
  address: '',
  description: '',
  openTime: '09:00',
  closeTime: '17:00',
  closedDays: [] as number[],
  dailyCapacity: 1200,
  phone: '',
  coverUrl: '',
  status: 1,
  sort: 0,
})

const form = reactive(emptyForm())

const weekDays = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
]

const rules: FormRules = {
  name: [{ required: true, message: '请输入博物馆名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  openTime: [{ required: true, message: '请输入开放时间', trigger: 'blur' }],
  closeTime: [{ required: true, message: '请输入闭馆时间', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const data = await adminMuseums({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
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
  editingWasEnabled.value = false
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: Museum) {
  editingId.value = row.id
  editingWasEnabled.value = row.status === 1
  Object.assign(form, {
    name: row.name,
    address: row.address,
    description: row.description || '',
    openTime: row.openTime || '09:00',
    closeTime: row.closeTime || '17:00',
    closedDays: parseClosedDays(row.closedDays),
    dailyCapacity: row.dailyCapacity ?? 1200,
    phone: row.phone || '',
    coverUrl: row.coverUrl || '',
    status: row.status,
    sort: row.sort,
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.openTime || !form.closeTime || form.openTime >= form.closeTime) {
    ElMessage.warning('闭馆时间必须晚于开放时间')
    return
  }
  if (form.closedDays.length >= 7) {
    ElMessage.warning('每周至少保留一天开放')
    return
  }
  saving.value = true
  const payload = { ...form, closedDays: form.closedDays.join(',') }
  try {
    if (editingId.value) {
      await updateMuseum(editingId.value, payload)
      if (editingWasEnabled.value && form.status === 0) {
        ElMessage.success('已更新，并级联停用该馆展览、票种与未来批次')
      } else {
        ElMessage.success('博物馆信息已更新')
      }
    } else {
      await createMuseum(payload)
      ElMessage.success('博物馆已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Museum) {
  try {
    await ElMessageBox.confirm(
      `确认删除「${row.name}」吗？存在有效预约时不能删除。`,
      '删除博物馆',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await deleteMuseum(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    // 取消或后端拒绝
  }
}

async function toggleStatus(row: Museum) {
  const nextStatus = row.status === 1 ? 0 : 1
  if (nextStatus === 0) {
    try {
      await ElMessageBox.confirm(
        `停用「${row.name}」将同时停用其全部展览、票种与未来入馆批次，确认停用吗？`,
        '停用博物馆',
        {
          type: 'warning',
          confirmButtonText: '确认停用',
          cancelButtonText: '取消',
        },
      )
    } catch {
      return
    }
  }
  try {
    await updateMuseum(row.id, { ...row, status: nextStatus })
    ElMessage.success(
      nextStatus === 0
        ? '已停用，并级联停用其展览、票种与未来批次'
        : '已启用该博物馆（展览、票种与批次需按需单独启用）',
    )
    load()
  } catch {
    // 错误已提示
  }
}

function changePage(value: number) {
  page.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="MUSEUM MANAGEMENT"
      title="博物馆管理"
      desc="维护博物馆基本信息、开放时间与展示状态。停用后记录保留并显示为停用；删除后不再展示。停用或删除都会级联处理该馆展览、票种与入馆批次。"
    >
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增博物馆
      </el-button>
    </PageHeader>

    <div class="admin-toolbar">
      <div class="toolbar-filters">
        <el-input v-model="keyword" placeholder="搜索名称或地址" clearable @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="status" placeholder="全部状态" clearable @change="search">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 座博物馆</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="博物馆" min-width="200">
          <template #default="{ row }">
            <div class="museum-cell">
              <div class="thumb">
                <img v-if="row.coverUrl" :src="row.coverUrl" alt="" />
                <span v-else class="serif">{{ row.name.slice(0, 1) }}</span>
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <small>{{ row.address }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="开放时间" width="130">
          <template #default="{ row }">{{ row.openTime }} - {{ row.closeTime }}</template>
        </el-table-column>
        <el-table-column label="每周闭馆日" width="110">
          <template #default="{ row }">{{ formatClosedDays(row.closedDays) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="状态" width="100" align="center">
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

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑博物馆' : '新增博物馆'"
      width="620px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-cols">
          <el-form-item label="博物馆名称" prop="name">
            <el-input v-model="form.name" placeholder="如：故宫博物院" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="选填" />
          </el-form-item>
          <el-form-item label="开放时间" prop="openTime">
            <el-time-picker
              v-model="form.openTime"
              value-format="HH:mm"
              format="HH:mm"
              placeholder="09:00"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="闭馆时间" prop="closeTime">
            <el-time-picker
              v-model="form.closeTime"
              value-format="HH:mm"
              format="HH:mm"
              placeholder="17:00"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="日容量（人/天）">
            <el-input-number v-model="form.dailyCapacity" :min="1" :max="100000" controls-position="right" style="width: 100%" />
            <span class="field-hint">普通票额度 = 日容量 − 当天各入馆批次额定容量之和</span>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="每周闭馆日">
          <el-checkbox-group v-model="form.closedDays">
            <el-checkbox-button v-for="day in weekDays" :key="day.value" :value="day.value">
              {{ day.label }}
            </el-checkbox-button>
          </el-checkbox-group>
          <span class="field-hint">闭馆日当天不开放预约，已有批次会显示为关闭且不可修改状态。</span>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="封面图片">
          <ImageUpload v-model="form.coverUrl" height="190px" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" show-word-limit />
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

.museum-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 9px;
  background: var(--ink);
  color: var(--gold-light);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.museum-cell strong {
  display: block;
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
}

.museum-cell small {
  display: block;
  max-width: 280px;
  margin-top: 4px;
  overflow: hidden;
  color: var(--muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.field-hint {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-size: 9.5px;
  line-height: 1.5;
}

@media (max-width: 600px) {
  .form-cols {
    grid-template-columns: 1fr;
  }
}
</style>
