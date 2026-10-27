<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import { getMuseum, getMuseumExhibitions, getMuseumTicketTypes, getSlot, getSiteSummary } from '@/api/public'
import { createReservation, mockPay } from '@/api/reservation'
import { useAuthStore } from '@/stores/auth'
import { audienceLabels, defaultVariant, groupTickets, ticketAudience, type TicketGroup } from '@/utils/ticket'
import type { Exhibition, Museum, ReservationDetail, Slot, TicketType } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const slot = ref<Slot | null>(null)
const museum = ref<Museum | null>(null)
const tickets = ref<TicketType[]>([])
const exhibitions = ref<Exhibition[]>([])
const ticketTypeId = ref<number | null>(null)
const activeStep = ref(0)
const loading = ref(true)
const submitting = ref(false)
const agreed = ref(false)
const payDialog = ref(false)
const paying = ref(false)
const createdOrder = ref<ReservationDetail | null>(null)

const contactFormRef = ref<FormInstance>()
const contact = reactive({
  name: '',
  phone: '',
})
const contactRules: FormRules = {
  name: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系人手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
}

const visitors = ref<Array<{ name: string; idCard: string; phone: string }>>([])
const remark = ref('')
const maxVisitors = ref(6)

const selectedTicket = computed(
  () => tickets.value.find((ticket) => ticket.id === ticketTypeId.value) || null,
)

const exhibitionNames = computed(() =>
  Object.fromEntries(exhibitions.value.map((exhibition) => [exhibition.id, exhibition.name])),
)

const ticketGroups = computed(() => groupTickets(tickets.value, exhibitionNames.value))

const selectedTicketLabel = computed(() => {
  if (!selectedTicket.value) return ''
  const audience = ticketAudience(selectedTicket.value)
  return audience === 'GENERAL'
    ? selectedTicket.value.name
    : `${selectedTicket.value.name} · ${audienceLabels[audience]}`
})

function displayVariant(group: TicketGroup) {
  return group.variants.find((ticket) => ticket.id === ticketTypeId.value) || defaultVariant(group)
}

function isGroupActive(group: TicketGroup) {
  return group.variants.some((ticket) => ticket.id === ticketTypeId.value)
}

function selectTicketGroup(group: TicketGroup) {
  if (isGroupActive(group)) return
  ticketTypeId.value = defaultVariant(group).id
}

function selectTicketVariant(group: TicketGroup, ticket: TicketType) {
  selectTicketGroup(group)
  ticketTypeId.value = ticket.id
}

const totalAmount = computed(() => {
  if (!selectedTicket.value) return 0
  return Number(selectedTicket.value.price) * visitors.value.length
})

const formattedDate = computed(() => {
  if (!slot.value) return ''
  return dayjs(slot.value.visitDate).format('YYYY 年 MM 月 DD 日')
})

const weekday = computed(() => {
  if (!slot.value) return ''
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dayjs(slot.value.visitDate).day()]
})

function isSlotPast(value: Slot) {
  return dayjs(`${value.visitDate} ${value.endTime}`).isBefore(dayjs())
}

function splitLines(value?: string) {
  return value
    ? value
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean)
    : []
}

function changeSlot() {
  if (!museum.value || !slot.value) return
  router.push({
    path: `/museums/${museum.value.id}`,
    query: { date: slot.value.visitDate },
    hash: '#booking',
  })
}

function addVisitor() {
  if (visitors.value.length >= maxVisitors.value) {
    ElMessage.info(`单笔预约最多 ${maxVisitors.value} 人`)
    return
  }
  visitors.value.push({ name: '', idCard: '', phone: '' })
}

function removeVisitor(index: number) {
  if (visitors.value.length <= 1) {
    ElMessage.info('至少保留一位参观者')
    return
  }
  visitors.value.splice(index, 1)
}

function validateVisitors() {
  if (!visitors.value.length) {
    ElMessage.warning('请至少填写一位参观者')
    return false
  }
  if (visitors.value.length > maxVisitors.value) {
    ElMessage.warning(`单笔预约最多 ${maxVisitors.value} 人`)
    return false
  }
  const idPattern = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$/
  const ids = new Set<string>()
  for (let i = 0; i < visitors.value.length; i += 1) {
    const visitor = visitors.value[i]
    if (!visitor.name.trim()) {
      ElMessage.warning(`请填写第 ${i + 1} 位参观者姓名`)
      return false
    }
    if (!idPattern.test(visitor.idCard.trim())) {
      ElMessage.warning(`第 ${i + 1} 位参观者身份证号格式不正确`)
      return false
    }
    if (visitor.phone && !/^1[3-9]\d{9}$/.test(visitor.phone)) {
      ElMessage.warning(`第 ${i + 1} 位参观者手机号格式不正确`)
      return false
    }
    const normalized = visitor.idCard.trim().toUpperCase()
    if (ids.has(normalized)) {
      ElMessage.warning('同一订单中不能出现重复的身份证号')
      return false
    }
    ids.add(normalized)
  }
  if (slot.value && visitors.value.length > slot.value.remaining) {
    ElMessage.warning(`该时段仅剩 ${slot.value.remaining} 张余票`)
    return false
  }
  return true
}

async function nextStep() {
  if (slot.value && isSlotPast(slot.value)) {
    ElMessage.warning('该参观时段已过，请重新选择日期与时段')
    changeSlot()
    return
  }
  if (activeStep.value === 0) {
    if (!ticketTypeId.value) {
      ElMessage.info('请选择票种')
      return
    }
    activeStep.value = 1
    return
  }
  if (activeStep.value === 1) {
    const formValid = await contactFormRef.value?.validate().catch(() => false)
    if (!formValid || !validateVisitors()) return
    activeStep.value = 2
  }
}

function prevStep() {
  if (activeStep.value > 0) activeStep.value -= 1
}

async function submit() {
  if (!slot.value || !ticketTypeId.value || !selectedTicket.value) return
  if (isSlotPast(slot.value)) {
    ElMessage.warning('该参观时段已过，请重新选择日期与时段')
    changeSlot()
    return
  }
  if (!agreed.value) {
    ElMessage.info('请先阅读并同意预约须知')
    return
  }
  submitting.value = true
  try {
    const order = await createReservation({
      slotId: slot.value.id,
      ticketTypeId: ticketTypeId.value,
      contactName: contact.name,
      contactPhone: contact.phone,
      visitors: visitors.value.map((visitor) => ({
        name: visitor.name.trim(),
        idCard: visitor.idCard.trim().toUpperCase(),
        phone: visitor.phone || undefined,
      })),
      remark: remark.value || undefined,
    })
    createdOrder.value = order
    if (order.status === 'PENDING_PAYMENT') {
      payDialog.value = true
    } else {
      ElMessage.success('预约成功，电子票已生成')
      router.replace(`/orders/${order.id}?from=booking`)
    }
  } finally {
    submitting.value = false
  }
}

async function payNow() {
  if (!createdOrder.value) return
  paying.value = true
  try {
    await mockPay(createdOrder.value.orderNo)
    ElMessage.success('模拟支付成功，电子票已生成')
    payDialog.value = false
    router.replace(`/orders/${createdOrder.value.id}?from=booking`)
  } finally {
    paying.value = false
  }
}

function payLater() {
  payDialog.value = false
  if (createdOrder.value) {
    router.replace(`/orders/${createdOrder.value.id}`)
  }
}

async function load() {
  loading.value = true
  try {
    const slotId = route.query.slotId as string | undefined
    if (!slotId) {
      ElMessage.info('请先从博物馆页面选择参观时段')
      router.replace('/museums')
      return
    }
    const slotData = await getSlot(slotId)
    if (isSlotPast(slotData)) {
      ElMessage.warning('该参观时段已过，请重新选择日期与时段')
      router.replace({
        path: `/museums/${slotData.museumId}`,
        query: { date: slotData.visitDate },
        hash: '#booking',
      })
      return
    }
    slot.value = slotData
    const [museumData, ticketData, exhibitionData, summary] = await Promise.all([
      getMuseum(slotData.museumId),
      getMuseumTicketTypes(slotData.museumId),
      getMuseumExhibitions(slotData.museumId),
      getSiteSummary(),
    ])
    museum.value = museumData
    tickets.value = ticketData
    exhibitions.value = exhibitionData
    ticketTypeId.value = ticketData[0]?.id || null
    maxVisitors.value = summary.maxVisitorsPerOrder || 6

    contact.name = auth.user?.realName || ''
    contact.phone = auth.user?.phone || ''
    visitors.value = [
      {
        name: auth.user?.realName || '',
        idCard: auth.user?.idCard || '',
        phone: auth.user?.phone || '',
      },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="booking-page">
    <div class="container">
      <div v-if="loading" class="loading-box">
        <el-skeleton :rows="8" animated />
      </div>
      <template v-else-if="slot && museum">
        <div class="booking-head">
          <div class="booking-head-actions">
            <router-link :to="`/museums/${museum.id}`" class="back-link">
              <el-icon><Back /></el-icon>
              返回 {{ museum.name }}
            </router-link>
            <el-button plain @click="changeSlot">
              <el-icon><Calendar /></el-icon>
              更换日期与时段
            </el-button>
          </div>
          <span class="eyebrow">RESERVATION</span>
          <h1 class="serif">完成你的参观预约</h1>
          <p>订单信息提交后，请在规定时间内完成支付。免费票将直接生成电子票。</p>
        </div>

        <div class="booking-layout">
          <div class="booking-main">
            <div class="stepper">
              <el-steps :active="activeStep" finish-status="success" align-center>
                <el-step title="选择票种" />
                <el-step title="参观者信息" />
                <el-step title="确认并提交" />
              </el-steps>
            </div>

            <div class="form-card">
              <div v-show="activeStep === 0" class="step-panel">
                <div class="panel-heading">
                  <span class="step-index">01</span>
                  <div>
                    <h2 class="serif">选择票种</h2>
                    <p>不同票种对应不同参观权益，请按需选择。</p>
                  </div>
                </div>
                <div class="ticket-options">
                  <div
                    v-for="group in ticketGroups"
                    :key="group.key"
                    class="ticket-option"
                    role="button"
                    tabindex="0"
                    :class="{ active: isGroupActive(group) }"
                    @click="selectTicketGroup(group)"
                    @keydown.enter="selectTicketGroup(group)"
                  >
                    <span class="radio"><i></i></span>
                    <span class="ticket-copy">
                      <span class="ticket-name-line">
                        <strong>{{ group.title }}</strong>
                        <em v-if="group.tagline">{{ group.tagline }}</em>
                      </span>
                      <small class="ticket-subtitle">{{ group.subtitle }}</small>

                      <span
                        v-if="group.kind !== 'BASIC'"
                        class="audience-options"
                        @click.stop
                      >
                        <button
                          v-for="variant in group.variants"
                          :key="variant.id"
                          type="button"
                          class="audience-option"
                          :class="{ active: displayVariant(group).id === variant.id }"
                          @click="selectTicketVariant(group, variant)"
                        >
                          <span>{{ audienceLabels[ticketAudience(variant)] }}</span>
                          <b>{{ Number(variant.price) === 0 ? '免费' : `¥${Number(variant.price).toFixed(0)}` }}</b>
                        </button>
                      </span>

                      <small v-if="displayVariant(group).description">{{ displayVariant(group).description }}</small>
                      <span v-if="displayVariant(group).audience" class="ticket-audience">
                        适用人群 · {{ displayVariant(group).audience }}
                      </span>
                      <span v-if="splitLines(displayVariant(group).benefits).length" class="ticket-benefits">
                        <i v-for="benefit in splitLines(displayVariant(group).benefits)" :key="benefit">{{ benefit }}</i>
                      </span>
                      <span v-if="splitLines(displayVariant(group).rules).length" class="ticket-rules">
                        <i v-for="rule in splitLines(displayVariant(group).rules)" :key="rule">{{ rule }}</i>
                      </span>
                    </span>
                    <b class="serif" :class="{ free: Number(displayVariant(group).price) === 0 }">
                      {{ Number(displayVariant(group).price) === 0 ? '免费' : `¥${Number(displayVariant(group).price).toFixed(0)}` }}
                    </b>
                  </div>
                </div>
              </div>

              <div v-show="activeStep === 1" class="step-panel">
                <div class="panel-heading">
                  <span class="step-index">02</span>
                  <div>
                    <h2 class="serif">填写参观者信息</h2>
                    <p>请使用真实信息，入馆时需与身份证件一致。</p>
                  </div>
                </div>

                <el-form
                  ref="contactFormRef"
                  :model="contact"
                  :rules="contactRules"
                  label-position="top"
                  class="contact-form"
                >
                  <div class="two-cols">
                    <el-form-item label="联系人姓名" prop="name">
                      <el-input v-model="contact.name" placeholder="请输入联系人姓名" />
                    </el-form-item>
                    <el-form-item label="联系人手机号" prop="phone">
                      <el-input v-model="contact.phone" placeholder="用于接收预约通知" maxlength="11" />
                    </el-form-item>
                  </div>
                </el-form>

                <div class="visitor-head">
                  <div>
                    <strong>参观者</strong>
                    <small>共 {{ visitors.length }} 人，最多 {{ maxVisitors }} 人</small>
                  </div>
                  <el-button plain size="small" @click="addVisitor">
                    <el-icon><Plus /></el-icon>
                    添加参观者
                  </el-button>
                </div>

                <div class="visitor-list">
                  <div v-for="(visitor, index) in visitors" :key="index" class="visitor-card">
                    <div class="visitor-no serif">{{ String(index + 1).padStart(2, '0') }}</div>
                    <div class="visitor-fields">
                      <el-input v-model="visitor.name" placeholder="姓名" />
                      <el-input v-model="visitor.idCard" placeholder="身份证号" maxlength="18" />
                      <el-input v-model="visitor.phone" placeholder="手机号（选填）" maxlength="11" />
                    </div>
                    <button
                      v-if="visitors.length > 1"
                      class="remove-btn"
                      title="移除"
                      @click="removeVisitor(index)"
                    >
                      <el-icon><Close /></el-icon>
                    </button>
                  </div>
                </div>

                <el-input
                  v-model="remark"
                  type="textarea"
                  :rows="2"
                  maxlength="200"
                  show-word-limit
                  placeholder="备注（选填），如无障碍需求等"
                  class="remark"
                />
              </div>

              <div v-show="activeStep === 2" class="step-panel">
                <div class="panel-heading">
                  <span class="step-index">03</span>
                  <div>
                    <h2 class="serif">确认预约信息</h2>
                    <p>请核对参观日期、时段与实名信息，提交后将占用票额。</p>
                  </div>
                </div>

                <div class="confirm-grid">
                  <div class="confirm-block">
                    <span>博物馆</span>
                    <strong>{{ museum.name }}</strong>
                  </div>
                  <div class="confirm-block">
                    <span>参观日期</span>
                    <strong>{{ formattedDate }}（{{ weekday }}）</strong>
                  </div>
                  <div class="confirm-block">
                    <span>参观时段</span>
                    <strong>{{ slot.startTime.slice(0, 5) }} - {{ slot.endTime.slice(0, 5) }}</strong>
                  </div>
                  <div class="confirm-block">
                    <span>票种</span>
                    <strong>{{ selectedTicketLabel || '未选择' }}</strong>
                  </div>
                  <div class="confirm-block">
                    <span>联系人</span>
                    <strong>{{ contact.name }} · {{ contact.phone }}</strong>
                  </div>
                  <div class="confirm-block">
                    <span>参观人数</span>
                    <strong>{{ visitors.length }} 人</strong>
                  </div>
                </div>

                <div class="visitor-summary">
                  <div v-for="(visitor, index) in visitors" :key="index">
                    <span class="serif">{{ String(index + 1).padStart(2, '0') }}</span>
                    <strong>{{ visitor.name }}</strong>
                    <small>{{ visitor.idCard.replace(/^(.{4}).*(.{4})$/, '$1**********$2') }}</small>
                  </div>
                </div>

                <el-checkbox v-model="agreed" class="agree-check">
                  我已阅读并同意《预约须知》，承诺所填信息真实有效，并按预约时段到馆参观。
                </el-checkbox>
              </div>

              <div class="step-actions">
                <el-button v-if="activeStep > 0" plain @click="prevStep">上一步</el-button>
                <el-button v-if="activeStep < 2" type="primary" @click="nextStep">下一步</el-button>
                <el-button v-else type="primary" :loading="submitting" @click="submit">提交预约</el-button>
              </div>
            </div>
          </div>

          <aside class="order-summary">
            <div class="summary-cover">
              <img v-if="museum.coverUrl" :src="museum.coverUrl" :alt="museum.name" />
              <div class="summary-cover-shade"></div>
              <div class="summary-title">
                <span>YOUR VISIT</span>
                <h3 class="serif">{{ museum.name }}</h3>
              </div>
            </div>
            <div class="summary-body">
              <div class="summary-row">
                <span>参观日期</span>
                <strong>{{ formattedDate }}</strong>
              </div>
              <div class="summary-row">
                <span>参观时段</span>
                <strong class="summary-slot">
                  {{ slot.startTime.slice(0, 5) }} - {{ slot.endTime.slice(0, 5) }}
                  <button class="change-inline" @click="changeSlot">更换</button>
                </strong>
              </div>
              <div class="summary-row">
                <span>票种</span>
                <strong>{{ selectedTicketLabel || '未选择' }}</strong>
              </div>
              <div class="summary-row">
                <span>参观人数</span>
                <strong>{{ visitors.length }} 人</strong>
              </div>
              <div class="summary-row">
                <span>余票</span>
                <strong :class="{ low: slot.remaining < 20 }">{{ slot.remaining }} 张</strong>
              </div>
              <div class="summary-total">
                <span>应付金额</span>
                <strong class="serif">
                  {{ totalAmount === 0 ? '免费' : `¥${totalAmount.toFixed(2)}` }}
                </strong>
              </div>
              <div class="summary-note">
                <el-icon><InfoFilled /></el-icon>
                提交后票额将为你保留 15 分钟，请及时完成支付。
              </div>
            </div>
          </aside>
        </div>
      </template>
    </div>

    <el-dialog
      v-model="payDialog"
      width="420px"
      title="模拟支付"
      :close-on-click-modal="false"
      :show-close="false"
    >
      <div class="pay-dialog">
        <span class="pay-label">订单金额</span>
        <strong class="serif pay-amount">¥{{ Number(createdOrder?.totalAmount || 0).toFixed(2) }}</strong>
        <p>订单号：{{ createdOrder?.orderNo }}</p>
        <div class="pay-tip">
          <el-icon><InfoFilled /></el-icon>
          当前为课程演示环境，使用模拟支付。点击确认后自动完成支付并生成电子票。
        </div>
      </div>
      <template #footer>
        <el-button @click="payLater">稍后支付</el-button>
        <el-button type="primary" :loading="paying" @click="payNow">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.booking-page {
  min-height: 70vh;
  padding: 42px 0 90px;
  background:
    radial-gradient(circle at 100% 0%, rgba(176, 138, 79, 0.08), transparent 30%),
    var(--paper);
}

.loading-box {
  padding: 60px 0;
}

.booking-head {
  max-width: 760px;
  margin-bottom: 34px;
}

.booking-head-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--muted);
  font-size: 11px;
}

.back-link:hover {
  color: var(--gold-deep);
}

.booking-head h1 {
  margin-top: 12px;
  color: var(--ink);
  font-size: clamp(32px, 4vw, 46px);
  font-weight: 500;
  letter-spacing: 0.04em;
}

.booking-head p {
  margin-top: 12px;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.8;
}

.booking-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 34px;
  align-items: start;
}

.booking-main {
  min-width: 0;
}

.stepper {
  margin-bottom: 26px;
  padding: 22px 16px 10px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
}

.stepper :deep(.el-step__title) {
  font-size: 12px;
}

.stepper :deep(.el-step__head.is-success) {
  color: var(--gold);
  border-color: var(--gold);
}

.form-card {
  padding: 30px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.panel-heading {
  display: flex;
  gap: 18px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--line);
}

.step-index {
  color: var(--gold);
  font-family: var(--font-serif);
  font-size: 13px;
  padding-top: 7px;
}

.panel-heading h2 {
  color: var(--ink);
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.panel-heading p {
  margin-top: 6px;
  color: var(--muted);
  font-size: 11.5px;
}

.ticket-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 24px;
}

.ticket-option {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ticket-option:hover {
  border-color: var(--gold-light);
}

.ticket-option.active {
  border-color: var(--gold);
  background: #fdf8ee;
  box-shadow: 0 0 0 1px var(--gold) inset;
}

.radio {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  margin-top: 3px;
  border: 1px solid var(--line);
  border-radius: 50%;
  flex-shrink: 0;
}

.ticket-option.active .radio {
  border-color: var(--gold);
}

.ticket-option.active .radio i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--gold);
}

.ticket-copy {
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
}

.ticket-name-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.ticket-copy strong {
  color: var(--ink);
  font-size: 14px;
}

.ticket-name-line em {
  padding: 2px 8px;
  border: 1px solid #e8dcc4;
  border-radius: 999px;
  background: #fbf5e8;
  color: var(--gold-deep);
  font-size: 9px;
  font-style: normal;
}

.ticket-copy small {
  color: var(--muted);
  font-size: 11px;
}

.ticket-subtitle {
  color: var(--text-soft) !important;
  font-size: 10.5px !important;
}

.audience-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 5px 0 3px;
}

.audience-option {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 6px 11px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fffdf9;
  color: var(--text-soft);
  font-size: 10px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.audience-option:hover {
  border-color: var(--gold-light);
}

.audience-option.active {
  border-color: var(--gold);
  background: #f7efe0;
  color: var(--ink);
}

.ticket-option .audience-option b {
  color: var(--gold-deep);
  font-size: 10.5px;
  font-weight: 600;
}

.ticket-audience {
  margin-top: 3px;
  color: var(--text-soft);
  font-size: 10.5px;
}

.ticket-benefits,
.ticket-rules {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 12px;
  margin-top: 3px;
  font-style: normal;
}

.ticket-benefits i {
  position: relative;
  padding-left: 10px;
  color: var(--text-soft);
  font-size: 9.5px;
  font-style: normal;
}

.ticket-benefits i::before {
  content: '·';
  position: absolute;
  left: 1px;
  color: var(--gold);
  font-weight: 700;
}

.ticket-rules {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px dashed var(--line);
}

.ticket-rules i {
  color: var(--muted);
  font-size: 9px;
  font-style: normal;
}

.ticket-rules i::before {
  content: '规 ';
  color: var(--gold-deep);
}

.ticket-option b {
  color: var(--gold-deep);
  font-size: 22px;
  font-weight: 500;
}

.ticket-option b.free {
  color: var(--success);
  font-size: 15px;
}

.contact-form {
  margin-top: 24px;
}

.two-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.visitor-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 14px;
}

.visitor-head strong {
  display: block;
  color: var(--ink);
  font-size: 14px;
}

.visitor-head small {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-size: 10px;
}

.visitor-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.visitor-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
}

.visitor-no {
  color: var(--gold);
  font-size: 14px;
  flex-shrink: 0;
}

.visitor-fields {
  display: grid;
  grid-template-columns: 0.8fr 1.5fr 0.9fr;
  gap: 10px;
  flex: 1;
}

.remove-btn {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: #faf3e8;
  color: var(--muted);
  cursor: pointer;
}

.remove-btn:hover {
  color: var(--danger);
}

.remark {
  margin-top: 16px;
}

.confirm-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  margin-top: 24px;
  background: var(--line);
  border: 1px solid var(--line);
  border-radius: 12px;
  overflow: hidden;
}

.confirm-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  background: #fff;
}

.confirm-block span {
  color: var(--muted);
  font-size: 10px;
}

.confirm-block strong {
  color: var(--ink);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.6;
}

.visitor-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.visitor-summary > div {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
}

.visitor-summary span {
  color: var(--gold);
  font-size: 10px;
}

.visitor-summary strong {
  color: var(--text);
  font-size: 12px;
  font-weight: 500;
}

.visitor-summary small {
  color: var(--muted);
  font-size: 10px;
}

.agree-check {
  margin-top: 22px;
  color: var(--text-soft);
  font-size: 11px;
}

.step-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 30px;
  padding-top: 22px;
  border-top: 1px solid var(--line);
}

.order-summary {
  position: sticky;
  top: 100px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.summary-cover {
  position: relative;
  height: 160px;
  background: var(--ink);
}

.summary-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.68;
}

.summary-cover-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent, rgba(13, 18, 30, 0.82));
}

.summary-title {
  position: absolute;
  left: 22px;
  bottom: 18px;
}

.summary-title span {
  color: var(--gold-light);
  font-size: 8px;
  letter-spacing: 0.2em;
}

.summary-title h3 {
  margin-top: 6px;
  color: #fff;
  font-size: 23px;
  font-weight: 500;
}

.summary-body {
  padding: 22px;
}

.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 11.5px;
}

.summary-row span {
  color: var(--muted);
}

.summary-row strong {
  color: var(--text);
  font-weight: 500;
}

.summary-row strong.low {
  color: var(--danger);
}

.summary-slot {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.change-inline {
  padding: 0;
  border: none;
  background: transparent;
  color: var(--gold-deep);
  font-size: 10px;
  text-decoration: underline;
  text-underline-offset: 3px;
  cursor: pointer;
}

.change-inline:hover {
  color: var(--gold);
}

.summary-total {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 16px;
}

.summary-total span {
  color: var(--muted);
  font-size: 11px;
}

.summary-total strong {
  color: var(--gold-deep);
  font-size: 30px;
  font-weight: 500;
}

.summary-note {
  display: flex;
  gap: 8px;
  margin-top: 18px;
  padding: 12px;
  border-radius: 8px;
  background: #f8f1e4;
  color: #8b754f;
  font-size: 10px;
  line-height: 1.7;
}

.pay-dialog {
  text-align: center;
}

.pay-label {
  display: block;
  color: var(--muted);
  font-size: 11px;
}

.pay-amount {
  display: block;
  margin: 8px 0 16px;
  color: var(--gold-deep);
  font-size: 38px;
  font-weight: 500;
}

.pay-dialog p {
  color: var(--muted);
  font-size: 10px;
}

.pay-tip {
  display: flex;
  gap: 8px;
  margin-top: 18px;
  padding: 12px;
  border-radius: 8px;
  background: #f8f1e4;
  color: #8b754f;
  font-size: 10.5px;
  line-height: 1.7;
  text-align: left;
}

@media (max-width: 960px) {
  .booking-layout {
    grid-template-columns: 1fr;
  }

  .order-summary {
    position: static;
    order: -1;
  }
}

@media (max-width: 640px) {
  .form-card {
    padding: 20px;
  }

  .two-cols,
  .visitor-fields,
  .confirm-grid {
    grid-template-columns: 1fr;
  }

  .visitor-card {
    align-items: flex-start;
  }

  .visitor-no {
    padding-top: 10px;
  }

  .step-actions .el-button {
    flex: 1;
  }
}
</style>
