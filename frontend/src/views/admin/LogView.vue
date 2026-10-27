<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import { adminLogs } from '@/api/admin'
import type { OperationLog } from '@/types'

const rows = ref<OperationLog[]>([])
const total = ref(0)
const page = ref(1)
const size = 15
const keyword = ref('')
const module = ref('')
const success = ref<number | undefined>(undefined)
const loading = ref(false)

const modules = ['预约', '核销', '系统管理', '博物馆', '展览', '时段', '票种', '公告', '认证']

async function load() {
  loading.value = true
  try {
    const data = await adminLogs({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
      module: module.value || undefined,
      success: success.value,
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

function changePage(value: number) {
  page.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="AUDIT TRAIL"
      title="操作日志"
      desc="记录关键业务操作与后台变更，便于审计与问题追踪。密码等敏感参数不会被写入日志。"
    />

    <div class="admin-toolbar">
      <div class="toolbar-filters">
        <el-input v-model="keyword" placeholder="用户/操作/模块" clearable @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="module" placeholder="全部模块" clearable @change="search">
          <el-option v-for="item in modules" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="success" placeholder="全部结果" clearable @change="search">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 条日志</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="log-expand">
              <div class="log-block">
                <span>请求方法</span>
                <code>{{ row.method || '—' }}</code>
              </div>
              <div class="log-block">
                <span>请求参数</span>
                <pre>{{ row.params || '—' }}</pre>
              </div>
              <div v-if="row.errorMsg" class="log-block error">
                <span>错误信息</span>
                <pre>{{ row.errorMsg }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="165">
          <template #default="{ row }">{{ dayjs(row.createTime).format('YYYY-MM-DD HH:mm:ss') }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="120">
          <template #default="{ row }">
            <span class="operator">{{ row.username || '匿名' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="模块 / 操作" min-width="180">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" class="module-tag">{{ row.module }}</el-tag>
            <span class="operation">{{ row.operation }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130">
          <template #default="{ row }"><span class="mono">{{ row.ip || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">
            <span :class="{ slow: (row.durationMs || 0) > 500 }">{{ row.durationMs ?? 0 }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" effect="plain" round>
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
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
  </div>
</template>

<style scoped>
.total-note {
  color: var(--muted);
  font-size: 11px;
}

.operator {
  color: var(--text);
  font-size: 11.5px;
}

.module-tag {
  margin-right: 8px;
}

.operation {
  color: var(--text-soft);
  font-size: 11.5px;
}

.slow {
  color: var(--danger);
}

.log-expand {
  padding: 8px 20px 16px 48px;
}

.log-block {
  display: flex;
  gap: 18px;
  padding: 8px 0;
}

.log-block > span {
  width: 80px;
  flex-shrink: 0;
  color: var(--muted);
  font-size: 10.5px;
}

.log-block code,
.log-block pre {
  margin: 0;
  flex: 1;
  overflow-x: auto;
  color: var(--text-soft);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-block.error pre {
  color: var(--danger);
}
</style>
