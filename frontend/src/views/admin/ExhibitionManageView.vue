<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import {
  adminExhibitions,
  adminMuseums,
  createExhibition,
  deleteExhibition,
  updateExhibition,
} from '@/api/admin'
import type { Exhibition, Museum } from '@/types'

const rows = ref<Exhibition[]>([])
const museums = ref<Museum[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const museumId = ref<number | undefined>(undefined)
const status = ref<number | undefined>(undefined)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const editingWasEnabled = ref(false)
const formRef = ref<FormInstance>()

const emptyForm = () => ({
  museumId: undefined as number | undefined,
  name: '',
  description: '',
  coverUrl: '',
  dateRange: [] as string[],
  status: 1,
})

const form = reactive(emptyForm())

const selectedMuseum = computed(
  () => museums.value.find((item) => item.id === form.museumId) || null,
)

function disabledDate(d: Date) {
  if (editingId.value) return false
  return dayjs(d).isBefore(dayjs(), 'day')
}

const rules: FormRules = {
  museumId: [{ required: true, message: '请选择所属博物馆', trigger: 'change' }],
  name: [{ required: true, message: '请输入展览名称', trigger: 'blur' }],
  dateRange: [{ required: true, type: 'array', min: 2, message: '请选择起止日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const data = await adminExhibitions({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
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

function openCreate() {
  editingId.value = null
  editingWasEnabled.value = false
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: Exhibition) {
  editingId.value = row.id
  editingWasEnabled.value = row.status === 1
  Object.assign(form, {
    museumId: row.museumId,
    name: row.name,
    description: row.description || '',
    coverUrl: row.coverUrl || '',
    dateRange: [row.startDate, row.endDate],
    status: row.status,
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!form.dateRange?.length) return
  if (!editingId.value && dayjs(form.dateRange[0]).isBefore(dayjs(), 'day')) {
    ElMessage.warning('展览开始日期不能早于今天')
    return
  }
  if (form.status === 1 && dayjs(form.dateRange[1]).isBefore(dayjs(), 'day')) {
    ElMessage.warning('展览结束日期不能早于今天')
    return
  }
  saving.value = true
  try {
    const payload = {
      museumId: form.museumId,
      name: form.name,
      description: form.description,
      coverUrl: form.coverUrl,
      startDate: form.dateRange[0],
      endDate: form.dateRange[1],
      status: form.status,
    }
    if (editingId.value) {
      await updateExhibition(editingId.value, payload)
      if (editingWasEnabled.value && form.status === 0) {
        ElMessage.success('已更新，并级联停用该展览的单展票与关联的未来批次')
      } else {
        ElMessage.success('展览信息已更新')
      }
    } else {
      await createExhibition(payload)
      ElMessage.success('展览已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Exhibition) {
  try {
    await ElMessageBox.confirm(
      `确认删除展览「${row.name}」吗？存在有效预约时不能删除。`,
      '删除展览',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
      },
    )
    await deleteExhibition(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    // 取消或后端拒绝
  }
}

async function toggleStatus(row: Exhibition) {
  const nextStatus = row.status === 1 ? 0 : 1
  if (nextStatus === 0) {
    try {
      await ElMessageBox.confirm(
        `停用「${row.name}」将同时停用其单展票与关联的未来入馆批次，确认停用吗？`,
        '停用展览',
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
    await updateExhibition(row.id, {
      museumId: row.museumId,
      name: row.name,
      description: row.description || '',
      coverUrl: row.coverUrl || '',
      startDate: row.startDate,
      endDate: row.endDate,
      status: nextStatus,
    })
    ElMessage.success(
      nextStatus === 0
        ? '已停用，并级联停用其单展票与关联的未来批次'
        : '已启用该展览（相关票种与批次需按需单独启用）',
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

onMounted(async () => {
  const data = await adminMuseums({ page: 1, size: 100 })
  museums.value = data.records
  load()
})
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="EXHIBITION MANAGEMENT"
      title="展览管理"
      desc="维护各博物馆的常设展与特展信息。停用后记录保留并显示为停用；删除后不再展示。停用或删除都会级联处理相关单展票与入馆批次。"
    >
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增展览
      </el-button>
    </PageHeader>

    <div class="admin-toolbar">
      <div class="toolbar-filters">
        <el-input v-model="keyword" placeholder="搜索展览名称" clearable @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="museumId" placeholder="全部博物馆" clearable @change="search">
          <el-option v-for="museum in museums" :key="museum.id" :label="museum.name" :value="museum.id" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable @change="search">
          <el-option label="展出中" :value="1" />
          <el-option label="已停用" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 场展览</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="展览" min-width="280">
          <template #default="{ row }">
            <div class="exhibition-cell">
              <div class="thumb">
                <img v-if="row.coverUrl" :src="row.coverUrl" alt="" />
                <span v-else class="serif">{{ row.name.slice(0, 1) }}</span>
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <small>{{ row.description || '暂无简介' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="museumName" label="所属博物馆" width="150" />
        <el-table-column label="展期" width="200">
          <template #default="{ row }">
            <span class="date-text">
              {{ dayjs(row.startDate).format('YYYY.MM.DD') }}<br />
              <em>至 {{ dayjs(row.endDate).format('YYYY.MM.DD') }}</em>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" round>
              {{ row.status === 1 ? '展出中' : '停用' }}
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑展览' : '新增展览'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-cols">
          <el-form-item label="所属博物馆" prop="museumId">
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
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="展出中" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="展览名称" prop="name">
          <el-input v-model="form.name" placeholder="如：千里江山：宋代绘画特展" />
        </el-form-item>
        <el-form-item label="展期" prop="dateRange">
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
          <span v-if="selectedMuseum" class="range-hint">
            所属博物馆开放时间 {{ selectedMuseum.openTime }} - {{ selectedMuseum.closeTime }}，展览按天展示，每日开放以该时间为准。
          </span>
        </el-form-item>
        <el-form-item label="封面图片">
          <ImageUpload v-model="form.coverUrl" height="190px" />
        </el-form-item>
        <el-form-item label="展览简介">
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

.range-hint {
  display: block;
  margin-top: 6px;
  color: var(--gold-deep);
  font-size: 10.5px;
  line-height: 1.6;
}

.exhibition-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thumb {
  display: grid;
  place-items: center;
  width: 52px;
  height: 40px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 7px;
  background: var(--ink);
  color: var(--gold-light);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.exhibition-cell strong {
  display: block;
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
}

.exhibition-cell small {
  display: block;
  max-width: 260px;
  margin-top: 4px;
  overflow: hidden;
  color: var(--muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date-text {
  color: var(--text-soft);
  font-size: 11px;
  line-height: 1.6;
}

.date-text em {
  color: var(--muted);
  font-style: normal;
  font-size: 10px;
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
