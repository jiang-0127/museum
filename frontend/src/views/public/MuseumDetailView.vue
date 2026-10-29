<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import {
  getBasicQuota,
  getMuseum,
  getMuseumExhibitions,
  getMuseumTicketTypes,
  getSiteSummary,
  getSlots,
} from '@/api/public'
import { useAuthStore } from '@/stores/auth'
import { audienceLabels, groupPriceRange, groupTickets, ticketAudience, type TicketGroup } from '@/utils/ticket'
import { formatClosedDays, isClosedOn } from '@/utils/schedule'
import type { BasicQuota, Exhibition, Museum, Slot, TicketType } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const museum = ref<Museum | null>(null)
const exhibitions = ref<Exhibition[]>([])
const tickets = ref<TicketType[]>([])
const slots = ref<Slot[]>([])
const selectedDate = ref('')
const selectedSlotId = ref<number | null>(null)
const loading = ref(true)
const maxVisitors = ref(6)
const advanceDays = ref(14)
const now = ref(dayjs())
let clockTimer: number | undefined

const basicTicket = computed(() =>
  tickets.value.find((ticket) => (ticket.ticketKind || 'BASIC') === 'BASIC'),
)

const dateOptions = computed(() => {
  const dates = new Set(slots.value.map((slot) => slot.visitDate))
  if (basicTicket.value) {
    const total = Math.max(1, advanceDays.value)
    for (let i = 0; i < total; i += 1) {
      dates.add(dayjs().add(i, 'day').format('YYYY-MM-DD'))
    }
  }
  const closedDays = museum.value?.closedDays
  return Array.from(dates)
    .filter((date) => !isClosedOn(closedDays, date))
    .sort()
})

const basicQuotaInfo = ref<BasicQuota | null>(null)

async function loadBasicQuota() {
  if (!museum.value || !selectedDate.value) {
    basicQuotaInfo.value = null
    return
  }
  try {
    basicQuotaInfo.value = await getBasicQuota(museum.value.id, selectedDate.value)
  } catch {
    basicQuotaInfo.value = null
  }
}

const basicQuota = computed(() => basicQuotaInfo.value?.basicQuota ?? 0)
const basicRemaining = computed(() => basicQuotaInfo.value?.basicRemaining ?? 0)

watch(selectedDate, () => {
  loadBasicQuota()
})

const slotsOfDate = computed(() =>
  slots.value.filter((slot) => slot.visitDate === selectedDate.value),
)

const selectedSlot = computed(
  () => slots.value.find((slot) => slot.id === selectedSlotId.value) || null,
)

const minPrice = computed(() => {
  if (!tickets.value.length) return null
  return Math.min(...tickets.value.map((ticket) => Number(ticket.price)))
})

const exhibitionNames = computed(() =>
  Object.fromEntries(exhibitions.value.map((exhibition) => [exhibition.id, exhibition.name])),
)

const ticketGroups = computed(() => groupTickets(tickets.value, exhibitionNames.value))

function groupPriceLabel(group: TicketGroup) {
  const { min, max } = groupPriceRange(group)
  if (min === 0 && max === 0) return '免费'
  return min === max ? `¥${min}` : `¥${min} 起`
}

function audiencePrices(group: TicketGroup) {
  return group.variants
    .map((ticket) => {
      const price = Number(ticket.price)
      return `${audienceLabels[ticketAudience(ticket)]} ${price === 0 ? '免费' : `¥${price}`}`
    })
    .join(' · ')
}

const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

type SlotPhase = {
  label: '展览中' | '待展出' | '已结束'
  bookable: boolean
  tone: 'ongoing' | 'upcoming' | 'ended'
}

function slotPhase(slot: Slot): SlotPhase {
  const start = dayjs(`${slot.visitDate} ${slot.startTime}`)
  const end = dayjs(`${slot.visitDate} ${slot.endTime}`)
  if (now.value.isAfter(end)) {
    return { label: '已结束', bookable: false, tone: 'ended' }
  }
  if (now.value.isBefore(start)) {
    return { label: '待展出', bookable: true, tone: 'upcoming' }
  }
  return { label: '展览中', bookable: false, tone: 'ongoing' }
}

function isSlotPast(slot: Slot) {
  return dayjs(`${slot.visitDate} ${slot.endTime}`).isBefore(now.value)
}

function isSlotSelectable(slot: Slot) {
  return slotPhase(slot).bookable && slot.remaining > 0
}

function dateLabel(date: string) {
  const day = dayjs(date)
  return {
    day: day.format('DD'),
    month: day.format('MM'),
    week: weekdays[day.day()],
    full: day.format('YYYY年MM月DD日'),
  }
}

function splitLines(value?: string) {
  return (value || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
}

async function load() {
  loading.value = true
  try {
    const museumId = route.params.id as string
    const [museumData, exhibitionData, ticketData, slotData, summary] = await Promise.all([
      getMuseum(museumId),
      getMuseumExhibitions(museumId),
      getMuseumTicketTypes(museumId),
      getSlots({ museumId, fromDate: dayjs().format('YYYY-MM-DD') }),
      getSiteSummary(),
    ])
    museum.value = museumData
    exhibitions.value = exhibitionData
    tickets.value = ticketData
    maxVisitors.value = summary.maxVisitorsPerOrder || 6
    advanceDays.value = summary.advanceBookingDays || 14
    slots.value = slotData.sort((a, b) =>
      `${a.visitDate}${a.startTime}`.localeCompare(`${b.visitDate}${b.startTime}`),
    )
    const queryDate = route.query.date as string | undefined
    if (queryDate && dateOptions.value.includes(queryDate)) {
      selectedDate.value = queryDate
    } else {
      selectedDate.value = dateOptions.value[0] || ''
    }
    await loadBasicQuota()
    const firstAvailable = slots.value.find(
      (slot) => slot.visitDate === selectedDate.value && isSlotSelectable(slot),
    )
    selectedSlotId.value = firstAvailable?.id || null
    if (route.hash === '#booking') {
      await nextTick()
      document.getElementById('booking')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  } finally {
    loading.value = false
  }
}

function selectDate(date: string) {
  selectedDate.value = date
  const firstAvailable = slotsOfDate.value.find((slot) => isSlotSelectable(slot))
  selectedSlotId.value = firstAvailable?.id || null
}

function book() {
  if (!selectedSlot.value) {
    ElMessage.info('请先选择一个参观时段')
    return
  }
  if (museum.value && isClosedOn(museum.value.closedDays, selectedSlot.value.visitDate)) {
    ElMessage.warning('该日期为博物馆闭馆日，暂不可预约')
    return
  }
  const phase = slotPhase(selectedSlot.value)
  if (!phase.bookable) {
    ElMessage.warning(`该时段${phase.label}，暂无法预约`)
    return
  }
  if (selectedSlot.value.remaining <= 0) {
    ElMessage.warning('该时段余票不足，请选择其他时段')
    return
  }
  const target = `/booking?slotId=${selectedSlot.value.id}`
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: target } })
    return
  }
  router.push(target)
}

function bookBasic() {
  if (!museum.value || !basicTicket.value || !selectedDate.value) {
    ElMessage.info('请先选择参观日期')
    return
  }
  if (basicQuotaInfo.value?.closed || isClosedOn(museum.value.closedDays, selectedDate.value)) {
    ElMessage.warning('该日期为博物馆闭馆日，暂不可预约')
    return
  }
  if (basicRemaining.value <= 0) {
    ElMessage.warning('该日期普通预约票已约满，请选择其他日期')
    return
  }
  const target = `/booking?museumId=${museum.value.id}&date=${selectedDate.value}&ticketTypeId=${basicTicket.value.id}`
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: target } })
    return
  }
  router.push(target)
}

onMounted(() => {
  load()
  clockTimer = window.setInterval(() => {
    now.value = dayjs()
  }, 60_000)
})

onUnmounted(() => {
  if (clockTimer) window.clearInterval(clockTimer)
})
</script>

<template>
  <div v-if="museum" class="museum-detail">
    <section class="museum-hero">
      <img v-if="museum.coverUrl" :src="museum.coverUrl" :alt="museum.name" />
      <div class="hero-shade"></div>
      <div class="container hero-content">
        <router-link to="/museums" class="back-link">
          <el-icon><Back /></el-icon>
          返回博物馆列表
        </router-link>
        <span class="hero-eyebrow">MUSEUM PROFILE</span>
        <h1 class="serif">{{ museum.name }}</h1>
        <div class="hero-meta">
          <span><el-icon><Location /></el-icon>{{ museum.address }}</span>
          <span><el-icon><Clock /></el-icon>{{ museum.openTime }} - {{ museum.closeTime }}</span>
          <span v-if="museum.phone"><el-icon><Phone /></el-icon>{{ museum.phone }}</span>
        </div>
      </div>
    </section>

    <div class="container detail-grid">
      <div class="detail-main">
        <section class="about block">
          <span class="eyebrow">ABOUT</span>
          <h2 class="serif">关于{{ museum.name }}</h2>
          <p class="about-text">{{ museum.description }}</p>
          <div class="about-facts">
            <div>
              <span>场馆开放时间</span>
              <strong>{{ museum.openTime }} - {{ museum.closeTime }}</strong>
            </div>
            <div v-if="museum.closedDays">
              <span>每周闭馆日</span>
              <strong>{{ formatClosedDays(museum.closedDays) }}</strong>
            </div>
            <div>
              <span>票种</span>
              <strong>{{ tickets.length }} 类</strong>
            </div>
            <div>
              <span>可预约日期</span>
              <strong>{{ dateOptions.length }} 天</strong>
            </div>
          </div>
        </section>

        <section class="block">
          <div class="block-head">
            <div>
              <span class="eyebrow">EXHIBITIONS</span>
              <h2 class="serif">馆内展览</h2>
            </div>
            <router-link to="/exhibitions" class="block-link">全部展览 <el-icon><Right /></el-icon></router-link>
          </div>
          <div v-if="exhibitions.length" class="exhibition-list">
            <div v-for="exhibition in exhibitions" :key="exhibition.id" class="exhibition-row">
              <div class="exhibition-image">
                <img v-if="exhibition.coverUrl" :src="exhibition.coverUrl" :alt="exhibition.name" loading="lazy" />
              </div>
              <div class="exhibition-info">
                <span class="exhibition-date">
                  {{ dayjs(exhibition.startDate).format('YYYY.MM.DD') }} — {{ dayjs(exhibition.endDate).format('YYYY.MM.DD') }}
                </span>
                <h3 class="serif">{{ exhibition.name }}</h3>
                <p>{{ exhibition.description }}</p>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无展览信息" :image-size="80" />
        </section>

        <section class="block">
          <div class="block-head">
            <div>
              <span class="eyebrow">TICKETS</span>
              <h2 class="serif">票种与说明</h2>
            </div>
            <span class="block-note">票价以预约页为准</span>
          </div>
          <div class="ticket-grid">
            <div v-for="group in ticketGroups" :key="group.key" class="ticket-type">
              <div class="ticket-type-head">
                <div>
                  <span v-if="group.tagline" class="ticket-tagline">{{ group.tagline }}</span>
                  <h4>{{ group.title }}</h4>
                  <p v-if="group.subtitle" class="ticket-subtitle">{{ group.subtitle }}</p>
                  <p>{{ group.description }}</p>
                </div>
                <strong class="serif" :class="{ free: groupPriceRange(group).max === 0 }">
                  {{ groupPriceLabel(group) }}
                </strong>
              </div>
              <p class="ticket-audience">{{ audiencePrices(group) }}</p>
              <ul v-if="splitLines(group.benefits).length" class="ticket-benefits">
                <li v-for="benefit in splitLines(group.benefits)" :key="benefit">{{ benefit }}</li>
              </ul>
            </div>
          </div>
        </section>

        <section class="visit-note">
          <el-icon><InfoFilled /></el-icon>
          <div>
            <strong>参观提示</strong>
            <p>
              请按预约日期与入馆批次到馆，普通预约票可在开放时间内任意时间入馆，携带与预约信息一致的有效身份证件。
              电子票仅在参观当天有效，入场时请出示电子票并核验票码。如行程有变，请在参观开始前取消预约。
            </p>
          </div>
        </section>
      </div>

      <aside id="booking" class="booking-panel">
        <div class="panel-head">
          <span class="eyebrow light">RESERVE A VISIT</span>
          <h2 class="serif">预约参观</h2>
          <p>选择日期与入馆批次，或直接预约普通票</p>
        </div>

        <div v-if="loading" class="panel-loading">
          <el-skeleton :rows="5" animated />
        </div>
        <template v-else>
          <div class="date-strip">
            <button
              v-for="date in dateOptions"
              :key="date"
              class="date-chip"
              :class="{ active: selectedDate === date }"
              @click="selectDate(date)"
            >
              <span>{{ dateLabel(date).week }}</span>
              <strong class="serif">{{ dateLabel(date).day }}</strong>
              <small>{{ dateLabel(date).month }}月</small>
            </button>
          </div>

          <div class="slot-section">
            <div class="slot-title">
              <span>{{ selectedDate ? dateLabel(selectedDate).full : '选择日期' }}</span>
              <small>余票实时更新</small>
            </div>
            <div v-if="slotsOfDate.length" class="slot-list">
              <button
                v-for="slot in slotsOfDate"
                :key="slot.id"
                class="slot-item"
                :class="{
                  active: selectedSlotId === slot.id,
                  soldout: !isSlotPast(slot) && slot.remaining <= 0,
                  expired: isSlotPast(slot),
                  blocked: !slotPhase(slot).bookable,
                }"
                :disabled="!isSlotSelectable(slot)"
                @click="selectedSlotId = slot.id"
              >
                <span class="slot-time">
                  {{ slot.startTime.slice(0, 5) }} - {{ slot.endTime.slice(0, 5) }}
                  <em
                    class="slot-exhibition"
                    :class="slotPhase(slot).tone"
                  >
                    {{ slotPhase(slot).label }}
                  </em>
                </span>
                <span class="slot-left">
                  <template v-if="!slotPhase(slot).bookable">
                    无法预约
                  </template>
                  <template v-else>
                    {{ slot.remaining <= 0 ? '已约满' : `余 ${slot.remaining} 张` }}
                  </template>
                </span>
              </button>
              <p v-if="slotsOfDate.every((slot) => isSlotPast(slot))" class="slot-all-passed">
                今日时段已结束，请选择其他日期
              </p>
              <p v-else-if="slotsOfDate.every((slot) => !isSlotSelectable(slot))" class="slot-all-passed">
                当前日期暂无可预约时段，请选择其他日期
              </p>
            </div>
            <el-empty v-else description="该日期暂无可预约时段" :image-size="64" />
          </div>

          <div v-if="basicTicket" class="basic-book">
            <div class="basic-book-head">
              <div>
                <strong>普通预约票 · 免选批次</strong>
                <small>开放时间内任意时间入馆，仅需选择参观日期</small>
              </div>
              <span class="basic-quota">本日额度 {{ basicQuota }} 张 · 余 {{ basicRemaining }} 张</span>
            </div>
            <el-button
              class="basic-btn"
              :disabled="basicRemaining <= 0"
              @click="bookBasic"
            >
              {{ basicRemaining <= 0 ? '本日普通票已约满' : '预约普通票' }}
            </el-button>
          </div>

          <div class="panel-summary">
            <div>
              <span>当前选择</span>
              <strong v-if="selectedSlot">
                {{ dateLabel(selectedSlot.visitDate).full }}<br />
                {{ selectedSlot.startTime.slice(0, 5) }} - {{ selectedSlot.endTime.slice(0, 5) }}
              </strong>
              <strong v-else class="placeholder">尚未选择时段</strong>
            </div>
            <div class="price">
              <span>起</span>
              <strong class="serif">
                {{ minPrice === null ? '—' : minPrice === 0 ? '免费' : `¥${minPrice}` }}
              </strong>
            </div>
          </div>

          <el-button
            type="primary"
            class="book-btn"
            size="large"
            @click="book"
          >
            {{ auth.isLoggedIn ? '立即预约' : '登录后预约' }}
          </el-button>
          <p class="panel-tip">
            <el-icon><Lock /></el-icon>
            实名预约，每个订单最多 {{ maxVisitors }} 人
          </p>
        </template>
      </aside>
    </div>
  </div>

  <div v-else-if="loading" class="loading-page">
    <el-skeleton :rows="8" animated />
  </div>
</template>

<style scoped>
.museum-detail {
  background: var(--paper);
}

.museum-hero {
  position: relative;
  height: 480px;
  background: var(--ink);
  overflow: hidden;
}

.museum-hero > img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(12, 17, 29, 0.78), rgba(12, 17, 29, 0.24) 60%),
    linear-gradient(180deg, rgba(12, 17, 29, 0.2), transparent 45%, rgba(12, 17, 29, 0.6));
}

.hero-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding-bottom: 56px;
  color: #fff;
}

.back-link {
  position: absolute;
  top: 32px;
  left: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.64);
  font-size: 11px;
}

.back-link:hover {
  color: var(--gold-light);
}

.hero-eyebrow {
  color: var(--gold-light);
  font-size: 10px;
  letter-spacing: 0.28em;
}

.hero-content h1 {
  margin-top: 14px;
  font-size: clamp(42px, 5.6vw, 70px);
  font-weight: 500;
  letter-spacing: 0.06em;
  line-height: 1.15;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-top: 24px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 52px;
  padding: 58px 0 90px;
  align-items: start;
}

.detail-main {
  min-width: 0;
}

.block {
  padding-bottom: 48px;
  margin-bottom: 48px;
  border-bottom: 1px solid var(--line);
}

.block h2 {
  margin-top: 12px;
  color: var(--ink);
  font-size: 30px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.about-text {
  margin-top: 22px;
  color: var(--text-soft);
  font-size: 14px;
  line-height: 2.1;
  text-align: justify;
}

.about-text::first-letter {
  float: left;
  margin: 6px 10px 0 0;
  color: var(--gold);
  font-family: var(--font-serif);
  font-size: 46px;
  line-height: 0.8;
}

.about-facts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  margin-top: 30px;
  background: var(--line);
  border: 1px solid var(--line);
  border-radius: 12px;
  overflow: hidden;
}

.about-facts div {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 20px;
  background: #fffdf9;
}

.about-facts span {
  color: var(--muted);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.about-facts strong {
  color: var(--ink);
  font-size: 14px;
  font-weight: 500;
}

.block-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.block-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--gold-deep);
  font-size: 11px;
}

.block-note {
  color: var(--muted);
  font-size: 11px;
}

.exhibition-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 26px;
}

.exhibition-row {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 24px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
  transition: box-shadow 0.25s ease;
}

.exhibition-row:hover {
  box-shadow: var(--shadow-sm);
}

.exhibition-image {
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 9px;
  background: var(--paper-deep);
}

.exhibition-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.exhibition-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-right: 12px;
}

.exhibition-date {
  color: var(--gold-deep);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.exhibition-info h3 {
  margin-top: 8px;
  color: var(--ink);
  font-size: 21px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.exhibition-info p {
  margin-top: 8px;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
}

.ticket-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 26px;
}

.ticket-type {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fffdf9;
}

.ticket-type-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.ticket-tagline {
  display: inline-block;
  margin-bottom: 7px;
  padding: 2px 8px;
  border: 1px solid #e8dcc4;
  border-radius: 999px;
  background: #fbf5e8;
  color: var(--gold-deep);
  font-size: 9px;
  letter-spacing: 0.06em;
}

.ticket-type h4 {
  color: var(--ink);
  font-size: 14px;
  font-weight: 600;
}

.ticket-type-head p {
  margin-top: 7px;
  color: var(--muted);
  font-size: 11px;
  line-height: 1.7;
}

.ticket-type-head p.ticket-subtitle {
  margin-top: 4px;
  color: var(--gold-deep);
  font-size: 10px;
  letter-spacing: 0.02em;
}

.ticket-type-head > strong {
  color: var(--gold-deep);
  font-size: 22px;
  font-weight: 500;
  white-space: nowrap;
}

.ticket-type-head > strong.free {
  color: var(--success);
  font-size: 16px;
}

.ticket-audience {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--line);
  color: var(--text-soft);
  font-size: 10.5px;
  line-height: 1.6;
}

.ticket-benefits {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-top: 10px;
  padding: 0;
  list-style: none;
}

.ticket-benefits li {
  position: relative;
  padding-left: 13px;
  color: var(--text-soft);
  font-size: 10.5px;
  line-height: 1.6;
}

.ticket-benefits li::before {
  content: '·';
  position: absolute;
  left: 2px;
  color: var(--gold);
  font-weight: 700;
}

.ticket-rules {
  margin-top: 10px;
  padding: 9px 10px;
  border-radius: 8px;
  background: var(--paper-light);
}

.ticket-rules span {
  display: block;
  margin-bottom: 4px;
  color: var(--gold-deep);
  font-size: 9px;
  letter-spacing: 0.08em;
}

.ticket-rules p {
  color: var(--muted);
  font-size: 9.5px;
  line-height: 1.6;
}

.visit-note {
  display: flex;
  gap: 16px;
  padding: 22px;
  border: 1px solid #e8dcc4;
  border-radius: 12px;
  background: #fbf5e8;
  color: var(--text-soft);
}

.visit-note > .el-icon {
  color: var(--gold-deep);
  font-size: 20px;
  flex-shrink: 0;
}

.visit-note strong {
  display: block;
  margin-bottom: 6px;
  color: var(--ink);
  font-size: 13px;
}

.visit-note p {
  font-size: 12px;
  line-height: 1.9;
}

.booking-panel {
  position: sticky;
  top: 100px;
  overflow: hidden;
  border-radius: 18px;
  background:
    radial-gradient(circle at 100% 0%, rgba(176, 138, 79, 0.2), transparent 42%),
    linear-gradient(160deg, #1b2538, #121a2a);
  color: rgba(255, 255, 255, 0.7);
  box-shadow: var(--shadow-lg);
}

.panel-head {
  padding: 28px 26px 22px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.eyebrow.light {
  color: var(--gold-light);
}

.panel-head h2 {
  margin-top: 10px;
  color: #f7f3eb;
  font-size: 29px;
  font-weight: 500;
  letter-spacing: 0.06em;
}

.panel-head p {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 11px;
}

.panel-loading {
  padding: 26px;
}

.date-strip {
  display: flex;
  gap: 8px;
  padding: 20px 22px 10px;
  overflow-x: auto;
}

.date-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  min-width: 54px;
  padding: 9px 4px;
  border: 1px solid rgba(255, 255, 255, 0.13);
  border-radius: 9px;
  background: transparent;
  color: rgba(255, 255, 255, 0.45);
  cursor: pointer;
  transition: all 0.2s ease;
}

.date-chip span,
.date-chip small {
  font-size: 9px;
}

.date-chip strong {
  color: rgba(255, 255, 255, 0.8);
  font-size: 19px;
  font-weight: 500;
}

.date-chip:hover {
  border-color: rgba(213, 187, 139, 0.5);
}

.date-chip.active {
  border-color: var(--gold);
  background: rgba(176, 138, 79, 0.18);
}

.date-chip.active strong,
.date-chip.active span {
  color: var(--gold-light);
}

.slot-section {
  padding: 12px 22px 18px;
}

.slot-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 11px;
}

.slot-title small {
  color: rgba(255, 255, 255, 0.3);
  font-size: 9px;
}

.slot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.slot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 13px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all 0.2s ease;
}

.slot-item:hover:not(.soldout) {
  border-color: rgba(213, 187, 139, 0.5);
}

.slot-item.active {
  border-color: var(--gold);
  background: rgba(176, 138, 79, 0.16);
}

.slot-item.soldout,
.slot-item.expired,
.slot-item.blocked {
  color: rgba(255, 255, 255, 0.5);
  cursor: not-allowed;
}

.slot-item.expired .slot-left,
.slot-item.blocked .slot-left {
  color: rgba(255, 255, 255, 0.55);
}

.slot-exhibition {
  margin-left: 8px;
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(240, 198, 116, 0.16);
  color: #f0c674;
  font-size: 8px;
  font-style: normal;
  letter-spacing: 0.04em;
}

.slot-exhibition.ongoing {
  background: rgba(240, 198, 116, 0.16);
  color: #f0c674;
}

.slot-exhibition.upcoming {
  background: rgba(123, 196, 150, 0.16);
  color: #8fd3a8;
}

.slot-exhibition.ended {
  background: rgba(224, 112, 112, 0.16);
  color: #f08a8a;
}

.slot-all-passed {
  margin-top: 4px;
  color: #e6bd85;
  font-size: 10px;
  text-align: center;
}

.slot-time {
  font-size: 12px;
  letter-spacing: 0.04em;
}

.slot-left {
  color: var(--gold-light);
  font-size: 10px;
}

.slot-item.soldout .slot-left {
  color: rgba(255, 255, 255, 0.5);
}

.basic-book {
  margin: 4px 22px 0;
  padding: 14px;
  border: 1px solid rgba(240, 198, 116, 0.3);
  border-radius: 10px;
  background: rgba(176, 138, 79, 0.12);
}

.basic-book-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.basic-book-head strong {
  display: block;
  color: #f0d9a8;
  font-size: 12px;
  font-weight: 500;
}

.basic-book-head small {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 9.5px;
  line-height: 1.5;
}

.basic-quota {
  color: var(--gold-light);
  font-size: 10px;
  white-space: nowrap;
}

.basic-btn {
  width: 100%;
  margin-top: 12px;
  border-color: rgba(240, 198, 116, 0.5);
  background: transparent;
  color: #f0d9a8;
}

.basic-btn:hover:not(.is-disabled) {
  border-color: var(--gold);
  background: rgba(176, 138, 79, 0.2);
  color: #fff;
}

.panel-summary {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 4px 22px 0;
  padding: 16px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-summary span {
  display: block;
  margin-bottom: 6px;
  color: rgba(255, 255, 255, 0.35);
  font-size: 9px;
  letter-spacing: 0.1em;
}

.panel-summary strong {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  line-height: 1.6;
  font-weight: 400;
}

.panel-summary .placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.price {
  text-align: right;
}

.price strong {
  color: var(--gold-light);
  font-size: 26px;
  font-weight: 500;
}

.book-btn {
  width: calc(100% - 44px);
  margin: 0 22px;
  height: 48px;
  border-color: var(--gold);
  letter-spacing: 0.16em;
}

.panel-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 16px 22px 22px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 9px;
}

.loading-page {
  width: min(1200px, calc(100% - 48px));
  margin: 80px auto;
}

@media (max-width: 1100px) {
  .detail-grid {
    grid-template-columns: 1fr 340px;
    gap: 36px;
  }
}

@media (max-width: 900px) {
  .museum-hero {
    height: 400px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .booking-panel {
    position: static;
    order: -1;
  }

  .ticket-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .museum-hero {
    height: 360px;
  }

  .hero-meta {
    flex-direction: column;
    gap: 8px;
  }

  .about-facts {
    grid-template-columns: 1fr;
  }

  .exhibition-row {
    grid-template-columns: 1fr;
  }

  .exhibition-image {
    aspect-ratio: 16 / 9;
  }

  .hero-content {
    padding-bottom: 34px;
  }
}
</style>
