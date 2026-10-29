<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import StatusTag from '@/components/StatusTag.vue'
import QrTicketDialog from '@/components/QrTicketDialog.vue'
import { cancelReservation, getMyReservations, getTicket, mockPay } from '@/api/reservation'
import type { Reservation, Ticket } from '@/types'

const router = useRouter()
const reservations = ref<Reservation[]>([])
const total = ref(0)
const page = ref(1)
const size = 6
const activeStatus = ref('')
const loading = ref(false)
const ticketVisible = ref(false)
const ticket = ref<Ticket | null>(null)
const ticketLoadingId = ref<number | null>(null)

const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待参观', value: 'RESERVED' },
  { label: '已核销', value: 'CHECKED_IN' },
  { label: '已取消', value: 'CANCELED' },
  { label: '已过期', value: 'EXPIRED' },
]

async function load() {
  loading.value = true
  try {
    const data = await getMyReservations({
      page: page.value,
      size,
      status: activeStatus.value || undefined,
    })
    reservations.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function changeTab(value: string) {
  activeStatus.value = value
  page.value = 1
  load()
}

function changePage(value: number) {
  page.value = value
  load()
}

async function openTicket(item: Reservation) {
  ticketLoadingId.value = item.id
  try {
    ticket.value = await getTicket(item.id)
    ticketVisible.value = true
  } finally {
    ticketLoadingId.value = null
  }
}

async function handlePay(item: Reservation) {
  try {
    await ElMessageBox.confirm(
      `即将模拟支付 ¥${Number(item.totalAmount).toFixed(2)}，支付后生成电子票。`,
      '模拟支付',
      { confirmButtonText: '确认支付', cancelButtonText: '稍后支付', type: 'info' },
    )
    await mockPay(item.orderNo)
    ElMessage.success('支付成功，电子票已生成')
    load()
  } catch {
    // 用户取消
  }
}

async function handleCancel(item: Reservation) {
  try {
    await ElMessageBox.confirm('取消后票额将被释放，确认取消该预约吗？', '取消预约', {
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
      type: 'warning',
    })
    await cancelReservation(item.id)
    ElMessage.success('预约已取消')
    load()
  } catch {
    // 用户取消
  }
}

function dateWeekday(date: string) {
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dayjs(date).day()]
}

onMounted(load)
</script>

<template>
  <div class="orders-page">
    <div class="container">
      <div class="orders-head">
        <div>
          <span class="eyebrow">MY RESERVATIONS</span>
          <h1 class="serif">我的预约</h1>
          <p>查看预约状态、完成支付，或出示电子票入馆。</p>
        </div>
        <el-button type="primary" round @click="router.push('/museums')">
          继续预约
          <el-icon class="el-icon--right"><Right /></el-icon>
        </el-button>
      </div>

      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          :class="{ active: activeStatus === tab.value }"
          @click="changeTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="loading" class="order-list">
        <el-skeleton v-for="i in 3" :key="i" :rows="4" animated class="loading-item" />
      </div>

      <div v-else-if="reservations.length" class="order-list">
        <article v-for="item in reservations" :key="item.id" class="order-card">
          <div class="order-top">
            <div class="order-no">
              <span>订单号</span>
              <code>{{ item.orderNo }}</code>
            </div>
            <StatusTag :status="item.status" />
          </div>

          <div class="order-body">
            <div class="museum-mark">
              <span class="serif">{{ item.museumName.slice(0, 1) }}</span>
            </div>
            <div class="order-info">
              <h3 class="serif">{{ item.museumName }}</h3>
              <p v-if="item.exhibitionName" class="exhibition">{{ item.exhibitionName }}</p>
              <div class="meta">
                <span>
                  <el-icon><Calendar /></el-icon>
                  {{ dayjs(item.visitDate).format('YYYY.MM.DD') }}（{{ dateWeekday(item.visitDate) }}）
                </span>
                <span>
                  <el-icon><Clock /></el-icon>
                  {{ item.startTime && item.endTime ? `${item.startTime.slice(0, 5)} - ${item.endTime.slice(0, 5)}` : '开放时间内任意时间' }}
                </span>
                <span>
                  <el-icon><User /></el-icon>
                  {{ item.visitorCount }} 人 · {{ item.ticketTypeName }}
                </span>
              </div>
            </div>
            <div class="order-amount">
              <span>{{ item.status === 'PENDING_PAYMENT' ? '待支付' : '订单金额' }}</span>
              <strong class="serif">
                {{ Number(item.totalAmount) === 0 ? '免费' : `¥${Number(item.totalAmount).toFixed(2)}` }}
              </strong>
            </div>
          </div>

          <div class="order-foot">
            <span class="created">创建于 {{ dayjs(item.createdAt).format('YYYY-MM-DD HH:mm') }}</span>
            <div class="actions">
              <el-button text size="small" @click="router.push(`/orders/${item.id}`)">订单详情</el-button>
              <el-button
                v-if="item.status === 'RESERVED'"
                type="primary"
                plain
                size="small"
                :loading="ticketLoadingId === item.id"
                @click="openTicket(item)"
              >
                查看电子票
              </el-button>
              <el-button
                v-if="item.status === 'PENDING_PAYMENT'"
                type="primary"
                size="small"
                @click="handlePay(item)"
              >
                去支付
              </el-button>
              <el-button
                v-if="['PENDING_PAYMENT', 'RESERVED'].includes(item.status)"
                text
                size="small"
                @click="handleCancel(item)"
              >
                取消预约
              </el-button>
            </div>
          </div>
        </article>
      </div>

      <el-empty v-else description="还没有相关预约">
        <el-button type="primary" round @click="router.push('/museums')">去预约参观</el-button>
      </el-empty>

      <el-pagination
        v-if="total > size"
        layout="prev, pager, next"
        :total="total"
        :page-size="size"
        :current-page="page"
        background
        @current-change="changePage"
      />
    </div>

    <QrTicketDialog v-model="ticketVisible" :ticket="ticket" />
  </div>
</template>

<style scoped>
.orders-page {
  min-height: 70vh;
  padding: 54px 0 90px;
  background:
    radial-gradient(circle at 90% 0%, rgba(176, 138, 79, 0.08), transparent 30%),
    var(--paper);
}

.orders-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 34px;
}

.orders-head h1 {
  margin-top: 12px;
  color: var(--ink);
  font-size: clamp(34px, 4vw, 48px);
  font-weight: 500;
  letter-spacing: 0.05em;
}

.orders-head p {
  margin-top: 10px;
  color: var(--muted);
  font-size: 12.5px;
}

.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--line);
  overflow-x: auto;
}

.tabs button {
  position: relative;
  padding: 13px 18px;
  border: none;
  background: transparent;
  color: var(--muted);
  font-size: 12.5px;
  white-space: nowrap;
  cursor: pointer;
}

.tabs button::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -1px;
  width: 0;
  height: 2px;
  background: var(--gold);
  transition: all 0.25s ease;
  transform: translateX(-50%);
}

.tabs button.active {
  color: var(--ink);
}

.tabs button.active::after {
  width: 70%;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-item {
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
}

.order-card {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.25s ease;
}

.order-card:hover {
  box-shadow: var(--shadow-md);
}

.order-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 22px;
  border-bottom: 1px solid var(--line);
  background: #fbf7f0;
}

.order-no {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted);
  font-size: 10px;
}

.order-no code {
  color: var(--text-soft);
  font-size: 10.5px;
  letter-spacing: 0.03em;
}

.order-body {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 22px;
}

.museum-mark {
  display: grid;
  place-items: center;
  width: 74px;
  height: 74px;
  flex-shrink: 0;
  border-radius: 12px;
  background:
    radial-gradient(circle at 30% 20%, rgba(213, 187, 139, 0.28), transparent 50%),
    var(--ink);
  color: var(--gold-light);
}

.museum-mark span {
  font-size: 30px;
}

.order-info {
  flex: 1;
  min-width: 0;
}

.order-info h3 {
  color: var(--ink);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.exhibition {
  margin-top: 3px;
  color: var(--gold-deep);
  font-size: 10.5px;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 12px;
  color: var(--muted);
  font-size: 11px;
}

.meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.order-amount {
  flex-shrink: 0;
  text-align: right;
}

.order-amount span {
  display: block;
  color: var(--muted);
  font-size: 10px;
}

.order-amount strong {
  display: block;
  margin-top: 5px;
  color: var(--gold-deep);
  font-size: 25px;
  font-weight: 500;
}

.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 22px;
  border-top: 1px solid var(--line);
  background: #fdfbf7;
}

.created {
  color: var(--muted);
  font-size: 9.5px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

@media (max-width: 700px) {
  .orders-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .order-body {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .museum-mark {
    width: 54px;
    height: 54px;
  }

  .museum-mark span {
    font-size: 22px;
  }

  .order-amount {
    width: 100%;
    padding-top: 10px;
    border-top: 1px dashed var(--line);
    text-align: left;
  }

  .order-foot {
    align-items: flex-start;
    flex-direction: column;
  }

  .actions {
    flex-wrap: wrap;
  }
}
</style>
