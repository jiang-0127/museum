<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import StatusTag from '@/components/StatusTag.vue'
import QrTicketDialog from '@/components/QrTicketDialog.vue'
import {
  cancelReservation,
  getReservation,
  getTicket,
  mockPay,
} from '@/api/reservation'
import type { ReservationDetail, Ticket } from '@/types'

const route = useRoute()
const router = useRouter()
const order = ref<ReservationDetail | null>(null)
const loading = ref(true)
const paying = ref(false)
const ticketLoading = ref(false)
const ticketVisible = ref(false)
const ticket = ref<Ticket | null>(null)

const statusText: Record<string, string> = {
  PENDING_PAYMENT: '订单已创建，请尽快完成支付',
  RESERVED: '预约成功，请在参观时段内到馆',
  CHECKED_IN: '已核销入场，祝参观愉快',
  CANCELED: '预约已取消，票额已释放',
  EXPIRED: '预约已过期',
}

const payTypeText: Record<string, string> = {
  MOCK: '模拟支付',
  FREE: '免费预约',
}

async function load() {
  loading.value = true
  try {
    order.value = await getReservation(route.params.id as string)
    if (route.query.from === 'booking' && order.value.status === 'RESERVED') {
      await openTicket()
    }
  } finally {
    loading.value = false
  }
}

async function openTicket() {
  if (!order.value) return
  ticketLoading.value = true
  try {
    ticket.value = await getTicket(order.value.id)
    ticketVisible.value = true
  } finally {
    ticketLoading.value = false
  }
}

async function handlePay() {
  if (!order.value) return
  try {
    await ElMessageBox.confirm(
      `确认模拟支付 ¥${Number(order.value.totalAmount).toFixed(2)}？支付完成后将生成电子票。`,
      '模拟支付',
      { confirmButtonText: '确认支付', cancelButtonText: '稍后支付', type: 'info' },
    )
    paying.value = true
    await mockPay(order.value.orderNo)
    ElMessage.success('支付成功，电子票已生成')
    await load()
  } catch {
    // 用户取消
  } finally {
    paying.value = false
  }
}

async function handleCancel() {
  if (!order.value) return
  try {
    await ElMessageBox.confirm('取消后票额将被释放，且不可恢复，确认取消吗？', '取消预约', {
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
      type: 'warning',
    })
    await cancelReservation(order.value.id)
    ElMessage.success('预约已取消')
    await load()
  } catch {
    // 用户取消
  }
}

function weekday(date: string) {
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dayjs(date).day()]
}

onMounted(load)
</script>

<template>
  <div class="detail-page">
    <div class="container">
      <div v-if="loading" class="loading">
        <el-skeleton :rows="9" animated />
      </div>

      <template v-else-if="order">
        <router-link to="/orders" class="back-link">
          <el-icon><Back /></el-icon>
          返回我的预约
        </router-link>

        <div class="order-hero" :class="`status-${order.status.toLowerCase()}`">
          <div class="hero-copy">
            <span class="eyebrow light">RESERVATION DETAIL</span>
            <div class="title-line">
              <h1 class="serif">{{ order.museumName }}</h1>
              <StatusTag :status="order.status" />
            </div>
            <p>{{ statusText[order.status] || '订单状态已更新' }}</p>
            <div class="hero-meta">
              <span><el-icon><Calendar /></el-icon>{{ dayjs(order.visitDate).format('YYYY 年 MM 月 DD 日') }}（{{ weekday(order.visitDate) }}）</span>
              <span><el-icon><Clock /></el-icon>{{ order.startTime.slice(0, 5) }} - {{ order.endTime.slice(0, 5) }}</span>
              <span><el-icon><User /></el-icon>{{ order.visitorCount }} 位参观者</span>
            </div>
          </div>
          <div class="hero-amount">
            <span>订单金额</span>
            <strong class="serif">
              {{ Number(order.totalAmount) === 0 ? '免费' : `¥${Number(order.totalAmount).toFixed(2)}` }}
            </strong>
            <small>{{ order.orderNo }}</small>
          </div>
        </div>

        <div class="detail-layout">
          <div class="detail-main">
            <section class="detail-card">
              <div class="card-title">
                <span class="serif">01</span>
                <h2>参观信息</h2>
              </div>
              <div class="info-grid">
                <div>
                  <span>博物馆</span>
                  <strong>{{ order.museumName }}</strong>
                </div>
                <div>
                  <span>展览</span>
                  <strong>{{ order.exhibitionName || '常设展' }}</strong>
                </div>
                <div>
                  <span>参观日期</span>
                  <strong>{{ dayjs(order.visitDate).format('YYYY-MM-DD') }}（{{ weekday(order.visitDate) }}）</strong>
                </div>
                <div>
                  <span>入场时段</span>
                  <strong>{{ order.startTime.slice(0, 5) }} - {{ order.endTime.slice(0, 5) }}</strong>
                </div>
                <div>
                  <span>票种</span>
                  <strong>{{ order.ticketTypeName }}</strong>
                </div>
                <div>
                  <span>单价 × 人数</span>
                  <strong>¥{{ Number(order.unitPrice).toFixed(2) }} × {{ order.visitorCount }}</strong>
                </div>
              </div>
              <div v-if="order.remark" class="remark">
                <span>备注</span>
                <p>{{ order.remark }}</p>
              </div>
            </section>

            <section class="detail-card">
              <div class="card-title">
                <span class="serif">02</span>
                <h2>参观者与联系人</h2>
              </div>
              <div class="contact-line">
                <div class="contact-avatar">{{ order.contactName.slice(0, 1) }}</div>
                <div>
                  <strong>{{ order.contactName }}</strong>
                  <span>{{ order.contactPhone }}</span>
                </div>
                <el-tag class="contact-tag" effect="plain" round>联系人</el-tag>
              </div>
              <div class="visitor-list">
                <div v-for="(visitor, index) in order.visitors" :key="visitor.id || index" class="visitor-line">
                  <span class="visitor-index serif">{{ String(index + 1).padStart(2, '0') }}</span>
                  <strong>{{ visitor.name }}</strong>
                  <span class="id-card">{{ visitor.idCard }}</span>
                  <span class="phone">{{ visitor.phone || '—' }}</span>
                </div>
              </div>
            </section>

            <section class="detail-card">
              <div class="card-title">
                <span class="serif">03</span>
                <h2>支付信息</h2>
              </div>
              <div class="info-grid">
                <div>
                  <span>支付状态</span>
                  <StatusTag :status="order.paymentStatus" />
                </div>
                <div>
                  <span>支付方式</span>
                  <strong>{{ order.paymentPayType ? payTypeText[order.paymentPayType] || order.paymentPayType : '—' }}</strong>
                </div>
                <div>
                  <span>支付单号</span>
                  <strong class="mono">{{ order.paymentNo || '—' }}</strong>
                </div>
                <div>
                  <span>支付时间</span>
                  <strong>{{ order.paymentPaidTime ? dayjs(order.paymentPaidTime).format('YYYY-MM-DD HH:mm:ss') : '—' }}</strong>
                </div>
                <div v-if="order.paymentRefundTime">
                  <span>退款时间</span>
                  <strong>{{ dayjs(order.paymentRefundTime).format('YYYY-MM-DD HH:mm:ss') }}</strong>
                </div>
                <div>
                  <span>下单时间</span>
                  <strong>{{ dayjs(order.createdAt).format('YYYY-MM-DD HH:mm:ss') }}</strong>
                </div>
              </div>
            </section>
          </div>

          <aside class="action-panel">
            <div class="panel-head">
              <span class="eyebrow">ORDER ACTION</span>
              <h3 class="serif">订单操作</h3>
            </div>

            <div v-if="order.status === 'PENDING_PAYMENT'" class="pay-block">
              <span>待支付金额</span>
              <strong class="serif">¥{{ Number(order.totalAmount).toFixed(2) }}</strong>
              <p>请在订单创建后 15 分钟内完成支付，超时后票额自动释放。</p>
              <el-button type="primary" size="large" :loading="paying" @click="handlePay">
                模拟支付
              </el-button>
            </div>

            <div v-else-if="order.status === 'RESERVED'" class="ticket-block">
              <div class="ticket-icon"><el-icon><Ticket /></el-icon></div>
              <strong>电子票已就绪</strong>
              <p>请在参观当天出示电子票，工作人员核验票码后入场。</p>
              <el-button type="primary" size="large" :loading="ticketLoading" @click="openTicket">
                查看电子票
              </el-button>
            </div>

            <div v-else-if="order.status === 'CHECKED_IN'" class="done-block">
              <div class="done-icon"><el-icon><CircleCheck /></el-icon></div>
              <strong>已完成参观核销</strong>
              <p>核销时间：{{ order.checkinTime ? dayjs(order.checkinTime).format('YYYY-MM-DD HH:mm:ss') : '—' }}</p>
            </div>

            <div v-else class="done-block muted">
              <div class="done-icon"><el-icon><Clock /></el-icon></div>
              <strong>{{ order.status === 'CANCELED' ? '订单已取消' : '订单已过期' }}</strong>
              <p>{{ order.cancelTime ? `取消时间：${dayjs(order.cancelTime).format('YYYY-MM-DD HH:mm')}` : '如需参观，请重新预约。' }}</p>
            </div>

            <div class="panel-actions">
              <el-button plain @click="router.push('/orders')">返回列表</el-button>
              <el-button
                v-if="['PENDING_PAYMENT', 'RESERVED'].includes(order.status)"
                text
                @click="handleCancel"
              >
                取消预约
              </el-button>
            </div>

            <div class="help-box">
              <el-icon><Service /></el-icon>
              <div>
                <strong>需要帮助？</strong>
                <p>如需修改参观者信息或遇到入场问题，请联系博物馆服务台。</p>
              </div>
            </div>
          </aside>
        </div>
      </template>
    </div>

    <QrTicketDialog v-model="ticketVisible" :ticket="ticket" />
  </div>
</template>

<style scoped>
.detail-page {
  min-height: 70vh;
  padding: 42px 0 90px;
  background: var(--paper);
}

.loading {
  padding: 60px 0;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  color: var(--muted);
  font-size: 11px;
}

.back-link:hover {
  color: var(--gold-deep);
}

.order-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  padding: 38px 38px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 90% 0%, rgba(176, 138, 79, 0.28), transparent 38%),
    linear-gradient(150deg, #1c2639, #111827);
  color: #fff;
  box-shadow: var(--shadow-md);
}

.order-hero.status-canceled,
.order-hero.status-expired {
  background:
    radial-gradient(circle at 90% 0%, rgba(120, 120, 120, 0.18), transparent 38%),
    linear-gradient(150deg, #2a2e36, #1a1d23);
}

.hero-copy {
  min-width: 0;
}

.eyebrow.light {
  color: var(--gold-light);
}

.title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 12px;
}

.title-line h1 {
  font-size: clamp(30px, 3.5vw, 44px);
  font-weight: 500;
  letter-spacing: 0.05em;
}

.hero-copy > p {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 22px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 11.5px;
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.hero-amount {
  flex-shrink: 0;
  text-align: right;
}

.hero-amount span {
  display: block;
  color: rgba(255, 255, 255, 0.4);
  font-size: 10px;
}

.hero-amount strong {
  display: block;
  margin-top: 4px;
  color: var(--gold-light);
  font-size: 36px;
  font-weight: 500;
}

.hero-amount small {
  display: block;
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 9px;
  letter-spacing: 0.04em;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 28px;
  margin-top: 24px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.detail-card {
  padding: 26px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.card-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.card-title span {
  color: var(--gold);
  font-size: 12px;
}

.card-title h2 {
  color: var(--ink);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: var(--line);
  border: 1px solid var(--line);
  border-radius: 10px;
  overflow: hidden;
}

.info-grid > div {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  background: #fff;
}

.info-grid span {
  color: var(--muted);
  font-size: 10px;
}

.info-grid strong {
  color: var(--text);
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.6;
}

.info-grid strong.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 10.5px;
  word-break: break-all;
}

.remark {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #fbf6eb;
}

.remark span {
  color: var(--muted);
  font-size: 10px;
}

.remark p {
  margin-top: 5px;
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.7;
}

.contact-line {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
}

.contact-avatar {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: var(--ink);
  color: var(--gold-light);
  font-family: var(--font-serif);
}

.contact-line strong {
  display: block;
  color: var(--ink);
  font-size: 13px;
}

.contact-line span {
  display: block;
  margin-top: 3px;
  color: var(--muted);
  font-size: 10.5px;
}

.contact-tag {
  margin-left: auto;
}

.visitor-list {
  margin-top: 14px;
}

.visitor-line {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 4px;
  border-bottom: 1px dashed var(--line);
  font-size: 12px;
}

.visitor-line:last-child {
  border-bottom: none;
}

.visitor-index {
  color: var(--gold);
  font-size: 10px;
}

.visitor-line strong {
  min-width: 70px;
  color: var(--text);
  font-weight: 500;
}

.id-card {
  flex: 1;
  color: var(--muted);
  font-size: 10.5px;
}

.phone {
  color: var(--muted);
  font-size: 10.5px;
}

.action-panel {
  position: sticky;
  top: 100px;
  padding: 26px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.panel-head h3 {
  margin-top: 8px;
  color: var(--ink);
  font-size: 22px;
  font-weight: 600;
}

.pay-block,
.ticket-block,
.done-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 22px;
  padding: 22px 18px;
  border-radius: 12px;
  background: #fbf6eb;
  text-align: center;
}

.pay-block > span {
  color: var(--muted);
  font-size: 10px;
}

.pay-block > strong {
  margin-top: 4px;
  color: var(--gold-deep);
  font-size: 36px;
  font-weight: 500;
}

.pay-block p,
.ticket-block p,
.done-block p {
  margin: 10px 0 16px;
  color: var(--muted);
  font-size: 10.5px;
  line-height: 1.7;
}

.pay-block .el-button,
.ticket-block .el-button {
  width: 100%;
  height: 44px;
}

.ticket-icon,
.done-icon {
  display: grid;
  place-items: center;
  width: 50px;
  height: 50px;
  margin-bottom: 12px;
  border-radius: 50%;
  background: var(--ink);
  color: var(--gold-light);
  font-size: 22px;
}

.done-block {
  background: #f2f7f3;
}

.done-block.muted {
  background: #f4f2ee;
}

.done-block.muted .done-icon {
  background: #6e6a62;
}

.ticket-block strong,
.done-block strong {
  color: var(--ink);
  font-size: 14px;
}

.panel-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
}

.help-box {
  display: flex;
  gap: 10px;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--line);
  color: var(--muted);
}

.help-box > .el-icon {
  color: var(--gold);
  margin-top: 1px;
}

.help-box strong {
  display: block;
  color: var(--text-soft);
  font-size: 11px;
}

.help-box p {
  margin-top: 4px;
  font-size: 9.5px;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .order-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-amount {
    text-align: left;
  }

  .detail-layout {
    grid-template-columns: 1fr;
  }

  .action-panel {
    position: static;
    order: -1;
  }
}

@media (max-width: 600px) {
  .order-hero {
    padding: 26px 22px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .visitor-line {
    flex-wrap: wrap;
    gap: 6px 12px;
  }

  .id-card {
    width: 100%;
    flex: auto;
  }
}
</style>
