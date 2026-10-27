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
const formRef = ref<FormInstance>()

const emptyForm = () => ({
  name: '',
  address: '',
  description: '',
  openTime: '09:00',
  closeTime: '17:00',
  phone: '',
  coverUrl: '',
  status: 1,
  sort: 0,
})

const form = reactive(emptyForm())

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
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: Museum) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    address: row.address,
    description: row.description || '',
    openTime: row.openTime || '09:00',
    closeTime: row.closeTime || '17:00',
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
  saving.value = true
  try {
    if (editingId.value) {
      await updateMuseum(editingId.value, { ...form })
      ElMessage.success('博物馆信息已更新')
    } else {
      await createMuseum({ ...form })
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
    await ElMessageBox.confirm(`确认删除「${row.name}」吗？相关展览与时段需先处理。`, '删除博物馆', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
    await deleteMuseum(row.id)
    ElMessage.success('已删除')
    load()
  } catch {
    // 取消或后端拒绝
  }
}

async function toggleStatus(row: Museum) {
  try {
    await updateMuseum(row.id, { ...row, status: row.status === 1 ? 0 : 1 })
    ElMessage.success(row.status === 1 ? '已停用该博物馆' : '已启用该博物馆')
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
      desc="维护博物馆基本信息、开放时间与展示状态。停用后游客端不再展示。"
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
        <el-table-column label="博物馆" min-width="230">
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
        <el-table-column label="开放时间" width="150">
          <template #default="{ row }">{{ row.openTime }} - {{ row.closeTime }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
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
            <el-input v-model="form.openTime" placeholder="08:30" />
          </el-form-item>
          <el-form-item label="闭馆时间" prop="closeTime">
            <el-input v-model="form.closeTime" placeholder="17:00" />
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

@media (max-width: 600px) {
  .form-cols {
    grid-template-columns: 1fr;
  }
}
</style>
