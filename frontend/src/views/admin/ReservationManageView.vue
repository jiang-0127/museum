<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import {
  adminCancelReservation,
  adminMuseums,
  adminReservationDetail,
  adminReservations,
} from '@/api/admin'
import type { Museum, Reservation, ReservationDetail } from '@/types'

const rows = ref<Reservation[]>([])
const museums = ref<Museum[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const status = ref('')
const museumId = ref<number | undefined>(undefined)
const dateRange = ref<string[]>([])
const loading = ref(false)
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<ReservationDetail | null>(null)

const statusOptions = [
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待参观', value: 'RESERVED' },
  { label: '已核销', value: 'CHECKED_IN' },
  { label: '已取消', value: 'CANCELED' },
  { label: '已过期', value: 'EXPIRED' },
]

async function load() {
  loading.value = true
  try {
    const data = await adminReservations({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      museumId: museumId.value,
      dateStart: dateRange.value?.[0],
      dateEnd: dateRange.value?.[1],
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

async function openDetail(row: Reservation) {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await adminReservationDetail(row.id)
  } finally {
    detailLoading.value = false
  }
}

async function cancel(row: Reservation | ReservationDetail) {
  try {
    await ElMessageBox.confirm(
      `确认取消订单 ${row.orderNo} 吗？票额将被释放，已支付订单将模拟退款。`,
      '取消预约',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '返回' },
    )
    await adminCancelReservation(row.id)
    ElMessage.success('订单已取消')
    drawerVisible.value = false
    load()
  } catch {
    // 取消
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
      eyebrow="RESERVATION ORDERS"
      title="预约订单"
      desc="查询全部预约订单、查看参观者与支付信息，必要时可强制取消并释放票额。"
    />

    <div class="admin-toolbar">
      <div class="toolbar-filters">
        <el-input
          v-model="keyword"
          placeholder="订单号/联系人/手机号"
          clearable
          style="width: 220px"
          @keyup.enter="search"
          @clear="search"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="status" placeholder="全部状态" clearable @change="search" style="width: 130px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="museumId" placeholder="全部博物馆" clearable @change="search">
          <el-option v-for="museum in museums" :key="museum.id" :label="museum.name" :value="museum.id" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="search"
        />
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 笔订单</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="订单号" width="175">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="museumName" label="博物馆" min-width="130" />
        <el-table-column label="参观时段" width="165">
          <template #default="{ row }">
            <span class="visit-date">{{ row.visitDate ? dayjs(row.visitDate).format('YYYY.MM.DD') : '—' }}</span>
            <small>{{ row.startTime && row.endTime ? `${row.startTime.slice(0, 5)} - ${row.endTime.slice(0, 5)}` : '开放时间内任意时间' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="联系人" width="130">
          <template #default="{ row }">
            <span>{{ row.contactName }}</span>
            <small class="phone">{{ row.contactPhone }}</small>
          </template>
        </el-table-column>
        <el-table-column label="人数" width="70" align="center">
          <template #default="{ row }">{{ row.visitorCount }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">
            <span class="amount">{{ Number(row.totalAmount) === 0 ? '免费' : `¥${Number(row.totalAmount).toFixed(2)}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="['PENDING_PAYMENT', 'RESERVED'].includes(row.status)"
              text
              size="small"
              type="danger"
              @click="cancel(row)"
            >
              取消
            </el-button>
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

    <el-drawer v-model="drawerVisible" title="预约订单详情" size="480px">
      <div v-loading="detailLoading" class="detail-drawer">
        <template v-if="detail">
          <div class="detail-top">
            <div>
              <span class="mono">{{ detail.orderNo }}</span>
              <h3 class="serif">{{ detail.museumName }}</h3>
            </div>
            <StatusTag :status="detail.status" />
          </div>

          <div class="detail-section">
            <h4>参观信息</h4>
            <div class="detail-row"><span>参观日期</span><strong>{{ detail.visitDate }}（{{ ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dayjs(detail.visitDate).day()] }}）</strong></div>
            <div class="detail-row"><span>入场方式</span><strong>{{ detail.startTime && detail.endTime ? `${detail.startTime.slice(0, 5)} - ${detail.endTime.slice(0, 5)}` : '开放时间内任意时间' }}</strong></div>
            <div class="detail-row"><span>展览</span><strong>{{ detail.exhibitionName || '常设展' }}</strong></div>
            <div class="detail-row"><span>票种</span><strong>{{ detail.ticketTypeName }}</strong></div>
            <div class="detail-row"><span>金额</span><strong>{{ Number(detail.totalAmount) === 0 ? '免费' : `¥${Number(detail.totalAmount).toFixed(2)}` }}</strong></div>
          </div>

          <div class="detail-section">
            <h4>参观者（{{ detail.visitors?.length || 0 }} 人）</h4>
            <div v-for="(visitor, index) in detail.visitors" :key="index" class="visitor-row">
              <span class="serif">{{ String(index + 1).padStart(2, '0') }}</span>
              <strong>{{ visitor.name }}</strong>
              <small>{{ visitor.idCard }}</small>
            </div>
            <div class="detail-row"><span>联系人</span><strong>{{ detail.contactName }} · {{ detail.contactPhone }}</strong></div>
          </div>

          <div class="detail-section">
            <h4>支付与时间</h4>
            <div class="detail-row"><span>支付状态</span><StatusTag :status="detail.paymentStatus" /></div>
            <div class="detail-row"><span>支付单号</span><strong class="mono">{{ detail.paymentNo || '—' }}</strong></div>
            <div class="detail-row"><span>下单时间</span><strong>{{ dayjs(detail.createdAt).format('YYYY-MM-DD HH:mm:ss') }}</strong></div>
            <div class="detail-row"><span>支付时间</span><strong>{{ detail.paymentPaidTime ? dayjs(detail.paymentPaidTime).format('YYYY-MM-DD HH:mm:ss') : '—' }}</strong></div>
            <div v-if="detail.cancelTime" class="detail-row"><span>取消时间</span><strong>{{ dayjs(detail.cancelTime).format('YYYY-MM-DD HH:mm:ss') }}</strong></div>
            <div v-if="detail.checkinTime" class="detail-row"><span>核销时间</span><strong>{{ dayjs(detail.checkinTime).format('YYYY-MM-DD HH:mm:ss') }}</strong></div>
          </div>

          <div v-if="detail.remark" class="detail-section">
            <h4>备注</h4>
            <p class="remark">{{ detail.remark }}</p>
          </div>

          <el-button
            v-if="['PENDING_PAYMENT', 'RESERVED'].includes(detail.status)"
            type="danger"
            plain
            class="cancel-btn"
            @click="cancel(detail)"
          >
            强制取消订单
          </el-button>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.total-note {
  color: var(--muted);
  font-size: 11px;
}

.order-no {
  color: var(--text-soft);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
}

.visit-date {
  display: block;
  color: var(--text);
  font-size: 11.5px;
}

.visit-date + small {
  display: block;
  margin-top: 3px;
  color: var(--muted);
  font-size: 9.5px;
}

.phone {
  display: block;
  margin-top: 3px;
  color: var(--muted);
  font-size: 9.5px;
}

.amount {
  color: var(--gold-deep);
  font-size: 11.5px;
  font-weight: 500;
}

.detail-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 8px;
}

.detail-top .mono {
  color: var(--muted);
  font-size: 9.5px;
}

.detail-top h3 {
  margin-top: 7px;
  color: var(--ink);
  font-size: 23px;
  font-weight: 600;
}

.visitor-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
}

.visitor-row > span {
  color: var(--gold);
  font-size: 10px;
}

.visitor-row strong {
  color: var(--text);
  font-size: 12px;
  font-weight: 500;
}

.visitor-row small {
  margin-left: auto;
  color: var(--muted);
  font-size: 10px;
}

.remark {
  padding: 12px;
  border-radius: 8px;
  background: #fbf6eb;
  color: var(--text-soft);
  font-size: 11px;
  line-height: 1.8;
}

.cancel-btn {
  width: 100%;
  margin-top: 22px;
}
</style>
