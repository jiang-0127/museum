<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import SectionHead from '@/components/SectionHead.vue'
import MuseumCard from '@/components/MuseumCard.vue'
import {
  getExhibitions,
  getLatestNotices,
  getMuseums,
  getSiteSummary,
} from '@/api/public'
import type { Exhibition, Museum, Notice, SiteSummary } from '@/types'
import { isClosedOn } from '@/utils/schedule'

const router = useRouter()
const museums = ref<Museum[]>([])
const exhibitions = ref<Exhibition[]>([])
const notices = ref<Notice[]>([])
const siteSummary = ref<SiteSummary>({
  museumCount: 0,
  advanceBookingDays: 14,
  maxVisitorsPerOrder: 6,
})
const loading = ref(true)

const advanceDays = computed(() => siteSummary.value.advanceBookingDays || 14)
const maxVisitors = computed(() => siteSummary.value.maxVisitorsPerOrder || 6)
const museumCount = computed(() => siteSummary.value.museumCount)
const museumSectionTitle = computed(() =>
  museumCount.value > 0 ? `${museumCount.value} 座馆，各有观看方式` : '选择一座馆，开始观看',
)

function timeToMinutes(value: string) {
  const [hour, minute] = value.split(':').map(Number)
  return hour * 60 + (minute || 0)
}

function minutesToTime(value: number) {
  return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}`
}

const visitTimeRange = computed(() => {
  const opens = museums.value
    .map((museum) => museum.openTime)
    .filter(Boolean)
    .map((value) => timeToMinutes(value as string))
  const closes = museums.value
    .map((museum) => museum.closeTime)
    .filter(Boolean)
    .map((value) => timeToMinutes(value as string))
  if (!opens.length || !closes.length) return null
  return {
    open: minutesToTime(Math.min(...opens)),
    close: minutesToTime(Math.max(...closes)),
  }
})

const quick = reactive({
  museumId: undefined as number | undefined,
  date: dayjs().format('YYYY-MM-DD'),
})

const disabledDate = (date: Date) => {
  const day = dayjs(date)
  if (
    day.isBefore(dayjs().startOf('day')) ||
    day.isAfter(dayjs().add(advanceDays.value - 1, 'day').endOf('day'))
  ) {
    return true
  }
  const museum = museums.value.find((item) => item.id === quick.museumId)
  return isClosedOn(museum?.closedDays, day.format('YYYY-MM-DD'))
}

const steps = computed(() => [
  { no: '01', title: '选择博物馆', desc: '浏览各馆正在展出的展览与开放时段。' },
  { no: '02', title: '选择日期时段', desc: `查看未来 ${advanceDays.value} 天的实时余票，选择适合你的参观时间。` },
  {
    no: '03',
    title: '填写实名信息',
    desc: `每位参观者均需实名登记，一次最多 ${maxVisitors.value} 人。`,
  },
  { no: '04', title: '凭电子票入馆', desc: '到馆出示电子票，工作人员核验票码后入场。' },
])

async function load() {
  loading.value = true
  try {
    const [museumPage, exhibitionPage, noticeList, summary] = await Promise.all([
      getMuseums({ page: 1, size: 6 }),
      getExhibitions({ page: 1, size: 3 }),
      getLatestNotices(3),
      getSiteSummary(),
    ])
    museums.value = museumPage.records
    exhibitions.value = exhibitionPage.records
    notices.value = noticeList
    siteSummary.value = summary
  } finally {
    loading.value = false
  }
}

function quickBook() {
  if (!quick.museumId) {
    ElMessage.info('请先选择想去的博物馆')
    return
  }
  router.push({
    path: `/museums/${quick.museumId}`,
    query: { date: quick.date },
  })
}

function formatDate(value?: string) {
  return value ? dayjs(value).format('MM.DD') : ''
}

onMounted(load)
</script>

<template>
  <div class="home">
    <section class="hero">
      <div class="hero-bg">
        <img
          src="/images/seed/museum-1.jpg"
          alt="博物馆展厅"
        />
      </div>
      <div class="hero-shade"></div>
      <div class="hero-lines"></div>
      <div class="container hero-inner">
        <div class="hero-copy">
          <span class="hero-eyebrow">MUSEUM · TIME · ENCOUNTER</span>
          <h1 class="serif">
            预约一段<br />
            与文明相遇的时间
          </h1>
          <p>
            提前选馆、选时段、实名预约。让排队与不确定留在门外，<br class="desktop-only" />
            把更多时间留给展厅里的凝视与呼吸。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="quickBook">
              立即预约
              <el-icon class="el-icon--right"><Right /></el-icon>
            </el-button>
            <el-button size="large" round class="ghost-btn" @click="router.push('/museums')">
              浏览博物馆
            </el-button>
          </div>
          <div class="hero-stats">
            <div>
              <strong class="serif">{{ loading ? '—' : museumCount }}</strong>
              <span>座博物馆</span>
            </div>
            <div>
              <strong class="serif">{{ advanceDays }}</strong>
              <span>天可预约</span>
            </div>
            <div>
              <strong class="serif">{{ maxVisitors }}</strong>
              <span>人 / 单笔</span>
            </div>
          </div>
        </div>
        <div class="hero-side">
          <span>SCROLL TO EXPLORE</span>
          <i></i>
        </div>
      </div>
    </section>

    <div class="container">
      <div class="quick-bar">
        <div class="quick-heading">
          <span class="quick-icon"><el-icon><Calendar /></el-icon></span>
          <div>
            <strong class="serif">快速预约</strong>
            <small>选择博物馆与日期，查看当日余票</small>
          </div>
        </div>
        <div class="quick-fields">
          <el-select v-model="quick.museumId" placeholder="选择博物馆" size="large" class="quick-select">
            <el-option
              v-for="museum in museums"
              :key="museum.id"
              :label="museum.name"
              :value="museum.id"
            />
          </el-select>
          <el-date-picker
            v-model="quick.date"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY 年 MM 月 DD 日"
            placeholder="选择参观日期"
            size="large"
            :disabled-date="disabledDate"
            class="quick-date"
          />
          <el-button type="primary" size="large" @click="quickBook">查询余票</el-button>
        </div>
      </div>
    </div>

    <section class="section">
      <div class="container">
        <SectionHead
          eyebrow="NOW EXHIBITING"
          title="展览推荐"
          desc="各馆同期呈现书画、青铜与江南工艺的重量级特展。展览不单独售票，入馆请预约博物馆时段。"
          link-text="查看全部展览"
          to="/exhibitions"
        />
        <div class="exhibition-grid">
          <router-link
            v-for="(exhibition, index) in exhibitions"
            :key="exhibition.id"
            :to="`/museums/${exhibition.museumId}`"
            class="exhibition-card hover-lift"
            :class="`exhibition-${index}`"
          >
            <img v-if="exhibition.coverUrl" :src="exhibition.coverUrl" :alt="exhibition.name" loading="lazy" />
            <div class="exhibition-shade"></div>
            <div class="exhibition-body">
              <div class="exhibition-labels">
                <span class="museum-label">{{ exhibition.museumName }}</span>
              </div>
              <h3 class="serif">{{ exhibition.name }}</h3>
              <p>{{ exhibition.description }}</p>
              <div class="exhibition-meta">
                <span>
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(exhibition.startDate) }} - {{ formatDate(exhibition.endDate) }}
                </span>
                <span class="enter">
                  查看所属博物馆
                  <el-icon><Right /></el-icon>
                </span>
              </div>
            </div>
          </router-link>
        </div>
      </div>
    </section>

    <section class="section section-soft museum-section">
      <div class="container">
        <SectionHead
          eyebrow="OUR MUSEUMS"
          :title="museumSectionTitle"
          desc="从宫城到江南园林，从青铜礼器到文人书画。选择一座馆，开始你的下一次观看。"
          link-text="全部博物馆"
          to="/museums"
        />
        <div v-if="loading" class="museum-grid">
          <el-skeleton v-for="i in 3" :key="i" animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 260px; border-radius: 18px" />
            </template>
          </el-skeleton>
        </div>
        <el-empty v-else-if="!museums.length" description="暂无可预约的博物馆" />
        <div v-else class="museum-grid">
          <MuseumCard
            v-for="(museum, index) in museums"
            :key="museum.id"
            :museum="museum"
            :index="index"
          />
        </div>
      </div>
    </section>

    <section class="section">
      <div class="container">
        <div class="story-grid">
          <div class="story-intro">
            <span class="eyebrow">HOW IT WORKS</span>
            <h2 class="section-title">一次预约，<br />四步抵达展厅</h2>
            <p class="section-desc">
              我们把预约流程压缩到最少的步骤。你只需要决定何时出发，剩下的交给我们。
            </p>
            <router-link to="/museums" class="story-link">
              现在开始
              <el-icon><Right /></el-icon>
            </router-link>
          </div>
          <div class="steps">
            <div v-for="(step, index) in steps" :key="step.no" class="step-item">
              <span class="step-no serif">{{ step.no }}</span>
              <div>
                <h4>{{ step.title }}</h4>
                <p>{{ step.desc }}</p>
              </div>
              <i v-if="index < steps.length - 1" class="step-line"></i>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="section notices-section">
      <div class="container">
        <div class="notice-layout">
          <div>
            <SectionHead eyebrow="VISITOR NOTICE" title="参观公告" link-text="全部公告" to="/notices" />
            <div class="notice-list">
              <router-link v-for="notice in notices" :key="notice.id" :to="`/notices/${notice.id}`" class="notice-item">
                <div class="notice-date">
                  <strong class="serif">{{ notice.publishTime ? dayjs(notice.publishTime).format('DD') : '—' }}</strong>
                  <span>{{ notice.publishTime ? dayjs(notice.publishTime).format('MMM').toUpperCase() : '' }}</span>
                </div>
                <div class="notice-copy">
                  <h4>{{ notice.title }}</h4>
                  <p>{{ notice.summary }}</p>
                </div>
                <el-icon><Right /></el-icon>
              </router-link>
            </div>
          </div>
          <div class="visit-card">
            <div class="visit-card-head">
              <span class="eyebrow light">BEFORE YOU VISIT</span>
              <h3 class="serif">到馆之前</h3>
            </div>
            <ul>
              <li><span>01</span>请携带预约使用的有效身份证件</li>
              <li><span>02</span>建议提前 10 分钟到馆，预留安检时间</li>
              <li><span>03</span>展厅内请勿使用闪光灯与三脚架</li>
              <li><span>04</span>如行程变化，请在时段开始前取消预约</li>
            </ul>
            <div class="visit-time">
              <span>各馆开放时间</span>
              <strong v-if="visitTimeRange" class="serif">
                {{ visitTimeRange.open }} — {{ visitTimeRange.close }}
              </strong>
              <strong v-else class="serif">以场馆详情为准</strong>
              <small>不同场馆时间不同，具体场次与闭馆安排请以场馆详情和公告为准</small>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  overflow: hidden;
}

.hero {
  position: relative;
  min-height: 660px;
  display: flex;
  align-items: center;
  background: var(--ink);
  color: #fff;
}

.hero-bg,
.hero-shade,
.hero-lines {
  position: absolute;
  inset: 0;
}

.hero-bg img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  animation: heroScale 18s ease-out both;
}

@keyframes heroScale {
  from {
    transform: scale(1.06);
  }
  to {
    transform: scale(1);
  }
}

.hero-shade {
  background:
    linear-gradient(90deg, rgba(12, 17, 29, 0.9) 0%, rgba(12, 17, 29, 0.55) 45%, rgba(12, 17, 29, 0.28)),
    linear-gradient(180deg, rgba(12, 17, 29, 0.3), transparent 40%, rgba(12, 17, 29, 0.7));
}

.hero-lines {
  background-image: linear-gradient(90deg, rgba(213, 187, 139, 0.08) 1px, transparent 1px);
  background-size: 25% 100%;
  opacity: 0.6;
}

.hero-inner {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 80px 0 110px;
}

.hero-copy {
  max-width: 660px;
  animation: floatUp 0.8s ease both;
}

.hero-eyebrow {
  color: var(--gold-light);
  font-size: 11px;
  letter-spacing: 0.28em;
}

.hero h1 {
  margin-top: 22px;
  font-size: clamp(46px, 6vw, 78px);
  font-weight: 500;
  line-height: 1.22;
  letter-spacing: 0.04em;
  text-shadow: 0 2px 30px rgba(0, 0, 0, 0.2);
}

.hero-copy > p {
  max-width: 560px;
  margin-top: 26px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 14px;
  line-height: 2;
}

.hero-actions {
  display: flex;
  gap: 14px;
  margin-top: 36px;
}

.ghost-btn {
  border-color: rgba(255, 255, 255, 0.3);
  background: transparent;
  color: #fff;
}

.ghost-btn:hover {
  border-color: var(--gold-light);
  background: rgba(255, 255, 255, 0.08);
  color: var(--gold-light);
}

.hero-stats {
  display: flex;
  gap: 40px;
  margin-top: 52px;
  padding-top: 26px;
  border-top: 1px solid rgba(255, 255, 255, 0.16);
  max-width: 420px;
}

.hero-stats div {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.hero-stats strong {
  color: var(--gold-light);
  font-size: 30px;
  font-weight: 400;
}

.hero-stats span {
  color: rgba(255, 255, 255, 0.45);
  font-size: 11px;
  letter-spacing: 0.06em;
}

.hero-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: rgba(255, 255, 255, 0.36);
  font-size: 9px;
  letter-spacing: 0.24em;
  writing-mode: vertical-rl;
}

.hero-side i {
  width: 1px;
  height: 70px;
  background: linear-gradient(180deg, var(--gold-light), transparent);
}

.quick-bar {
  position: relative;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  margin-top: -52px;
  padding: 22px 28px;
  border: 1px solid rgba(229, 221, 207, 0.8);
  border-radius: 18px;
  background: rgba(255, 253, 249, 0.96);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(18px);
}

.quick-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.quick-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--ink);
  color: var(--gold-light);
  font-size: 18px;
}

.quick-heading strong {
  display: block;
  color: var(--ink);
  font-size: 19px;
  letter-spacing: 0.08em;
}

.quick-heading small {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-size: 10px;
}

.quick-fields {
  display: flex;
  gap: 12px;
  flex: 1;
  justify-content: flex-end;
}

.quick-select {
  width: 190px;
}

.quick-date {
  width: 220px;
}

.section-soft {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.35), transparent),
    var(--paper-deep);
}

.exhibition-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr 0.9fr;
  gap: 22px;
}

.exhibition-card {
  position: relative;
  min-height: 430px;
  overflow: hidden;
  border-radius: 20px;
  background: var(--ink);
  color: #fff;
}

.exhibition-card img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.9s ease;
}

.exhibition-card:hover img {
  transform: scale(1.05);
}

.exhibition-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(12, 17, 29, 0.05), rgba(12, 17, 29, 0.88));
}

.exhibition-body {
  position: absolute;
  inset: auto 0 0 0;
  padding: 28px;
}

.exhibition-labels {
  display: flex;
  align-items: center;
  gap: 8px;
}

.museum-label {
  display: inline-block;
  padding: 4px 10px;
  border: 1px solid rgba(213, 187, 139, 0.45);
  border-radius: 999px;
  color: var(--gold-light);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.exhibition-body h3 {
  margin-top: 14px;
  font-size: clamp(22px, 2.2vw, 30px);
  font-weight: 500;
  line-height: 1.35;
  letter-spacing: 0.04em;
}

.exhibition-body p {
  display: -webkit-box;
  margin-top: 10px;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
  line-height: 1.8;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.exhibition-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.54);
  font-size: 11px;
}

.exhibition-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.enter {
  color: var(--gold-light);
}

.exhibition-1 {
  min-height: 430px;
}

.exhibition-2 {
  min-height: 430px;
}

.museum-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.story-grid {
  display: grid;
  grid-template-columns: 0.8fr 1.2fr;
  gap: 90px;
  align-items: start;
}

.story-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 30px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--gold);
  color: var(--gold-deep);
  font-size: 12px;
  letter-spacing: 0.08em;
}

.steps {
  display: flex;
  flex-direction: column;
}

.step-item {
  position: relative;
  display: flex;
  gap: 26px;
  padding: 26px 0;
  border-bottom: 1px solid var(--line);
}

.step-no {
  color: var(--gold);
  font-size: 13px;
  letter-spacing: 0.1em;
  padding-top: 3px;
}

.step-item h4 {
  color: var(--ink);
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.step-item p {
  margin-top: 8px;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.8;
}

.notices-section {
  background: #fbf8f2;
}

.notice-layout {
  display: grid;
  grid-template-columns: 1.35fr 0.65fr;
  gap: 64px;
}

.notice-list {
  border-top: 1px solid var(--line);
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 22px 4px;
  border-bottom: 1px solid var(--line);
  transition: padding 0.25s ease;
}

.notice-item:hover {
  padding-left: 10px;
}

.notice-date {
  width: 54px;
  flex-shrink: 0;
  text-align: center;
}

.notice-date strong {
  display: block;
  color: var(--ink);
  font-size: 28px;
  font-weight: 500;
  line-height: 1;
}

.notice-date span {
  display: block;
  margin-top: 5px;
  color: var(--muted);
  font-size: 9px;
  letter-spacing: 0.12em;
}

.notice-copy {
  flex: 1;
  min-width: 0;
}

.notice-copy h4 {
  color: var(--text);
  font-size: 15px;
  font-weight: 500;
}

.notice-copy p {
  margin-top: 6px;
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-item > .el-icon {
  color: var(--gold);
}

.visit-card {
  align-self: start;
  padding: 34px;
  border-radius: 20px;
  background:
    radial-gradient(circle at 100% 0%, rgba(176, 138, 79, 0.25), transparent 40%),
    linear-gradient(150deg, #1c263a, #111827);
  color: rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-md);
}

.visit-card-head .eyebrow.light {
  color: var(--gold-light);
}

.visit-card-head h3 {
  margin-top: 12px;
  color: #f7f3eb;
  font-size: 28px;
  font-weight: 500;
}

.visit-card ul {
  margin: 26px 0;
  padding: 0;
  list-style: none;
}

.visit-card li {
  display: flex;
  gap: 12px;
  padding: 11px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 12px;
  line-height: 1.7;
}

.visit-card li span {
  color: var(--gold);
  font-size: 10px;
  padding-top: 2px;
}

.visit-time {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.visit-time > span {
  color: rgba(255, 255, 255, 0.4);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.visit-time strong {
  color: var(--gold-light);
  font-size: 24px;
  font-weight: 400;
}

.visit-time small {
  color: rgba(255, 255, 255, 0.36);
  font-size: 10px;
}

@media (max-width: 1100px) {
  .exhibition-grid {
    grid-template-columns: 1fr 1fr;
  }

  .exhibition-2 {
    display: none;
  }
}

@media (max-width: 960px) {
  .hero {
    min-height: 580px;
  }

  .hero-inner {
    padding: 70px 0 100px;
  }

  .hero-side {
    display: none;
  }

  .quick-bar {
    align-items: stretch;
    flex-direction: column;
    margin-top: -40px;
  }

  .quick-fields {
    flex-wrap: wrap;
    justify-content: stretch;
  }

  .quick-select,
  .quick-date {
    flex: 1;
    min-width: 160px;
    width: auto;
  }

  .museum-grid {
    grid-template-columns: 1fr 1fr;
  }

  .story-grid,
  .notice-layout {
    grid-template-columns: 1fr;
    gap: 40px;
  }
}

@media (max-width: 680px) {
  .hero h1 {
    font-size: 42px;
  }

  .hero-copy > p {
    font-size: 13px;
  }

  .hero-actions {
    flex-wrap: wrap;
  }

  .hero-stats {
    gap: 24px;
  }

  .exhibition-grid {
    grid-template-columns: 1fr;
  }

  .exhibition-2 {
    display: block;
  }

  .exhibition-card {
    min-height: 360px;
  }

  .museum-grid {
    grid-template-columns: 1fr;
  }

  .quick-fields .el-button {
    width: 100%;
  }

  .quick-select,
  .quick-date {
    width: 100%;
    flex: auto;
  }

  .desktop-only {
    display: none;
  }

  .visit-card {
    padding: 26px;
  }
}
</style>
