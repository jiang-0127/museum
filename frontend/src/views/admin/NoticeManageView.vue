<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import {
  adminNotices,
  createNotice,
  deleteNotice,
  updateNotice,
} from '@/api/admin'
import type { Notice } from '@/types'

const rows = ref<Notice[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const status = ref<number | undefined>(undefined)
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const emptyForm = () => ({
  title: '',
  summary: '',
  content: '',
  coverUrl: '',
  status: 1,
})

const form = reactive(emptyForm())

const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const data = await adminNotices({
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

function openEdit(row: Notice) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    summary: row.summary || '',
    content: row.content || '',
    coverUrl: row.coverUrl || '',
    status: row.status,
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingId.value) {
      await updateNotice(editingId.value, { ...form })
      ElMessage.success('公告已更新')
    } else {
      await createNotice({ ...form })
      ElMessage.success('公告已发布')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: Notice) {
  try {
    await ElMessageBox.confirm(`确认下线公告「${row.title}」吗？游客端将不再展示。`, '下线公告', {
      type: 'warning',
      confirmButtonText: '确认下线',
      cancelButtonText: '取消',
    })
    await deleteNotice(row.id)
    ElMessage.success('公告已下线')
    load()
  } catch {
    // 取消
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
      eyebrow="NOTICE MANAGEMENT"
      title="公告管理"
      desc="发布预约须知、开放时间调整与临时通知。下线后游客端立即隐藏。"
    >
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        发布公告
      </el-button>
    </PageHeader>

    <div class="admin-toolbar">
      <div class="toolbar-filters">
        <el-input v-model="keyword" placeholder="搜索公告标题" clearable @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="status" placeholder="全部状态" clearable @change="search">
          <el-option label="展示中" :value="1" />
          <el-option label="已下线" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 条公告</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="公告标题" min-width="280">
          <template #default="{ row }">
            <strong class="notice-title">{{ row.title }}</strong>
            <small class="notice-summary">{{ row.summary || row.content }}</small>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">
            {{ row.publishTime ? dayjs(row.publishTime).format('YYYY-MM-DD HH:mm') : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain" round>
              {{ row.status === 1 ? '展示中' : '已下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openEdit(row)">编辑</el-button>
            <el-button text size="small" type="danger" @click="remove(row)">下线</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑公告' : '发布公告'" width="680px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" placeholder="列表页展示的一句话摘要" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="公告内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="支持换行，游客端将保留段落格式" />
        </el-form-item>
        <el-form-item label="封面图片">
          <ImageUpload v-model="form.coverUrl" height="170px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">立即展示</el-radio>
            <el-radio :value="0">暂不展示</el-radio>
          </el-radio-group>
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

.notice-title {
  display: block;
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
}

.notice-summary {
  display: block;
  max-width: 420px;
  margin-top: 4px;
  overflow: hidden;
  color: var(--muted);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
