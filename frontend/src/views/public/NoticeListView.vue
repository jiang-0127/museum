<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { getNotices } from '@/api/public'
import type { Notice } from '@/types'

const notices = ref<Notice[]>([])
const total = ref(0)
const page = ref(1)
const size = 8
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getNotices({ page: page.value, size })
    notices.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function changePage(value: number) {
  page.value = value
  load()
}

onMounted(load)
</script>

<template>
  <div class="notice-page">
    <section class="notice-hero">
      <div class="container">
        <span class="eyebrow">VISITOR NOTICE</span>
        <h1 class="serif">参观公告</h1>
        <p>预约规则、开放时间与临时调整。出发前，请花一分钟确认。</p>
      </div>
    </section>

    <div class="container notice-content">
      <div v-if="loading" class="notice-list">
        <el-skeleton v-for="i in 4" :key="i" :rows="2" animated />
      </div>
      <div v-else-if="notices.length" class="notice-list">
        <router-link v-for="notice in notices" :key="notice.id" :to="`/notices/${notice.id}`" class="notice-card">
          <div class="notice-date">
            <strong class="serif">{{ notice.publishTime ? dayjs(notice.publishTime).format('DD') : '—' }}</strong>
            <span>{{ notice.publishTime ? dayjs(notice.publishTime).format('YYYY.MM') : '' }}</span>
          </div>
          <div class="notice-main">
            <h3 class="serif">{{ notice.title }}</h3>
            <p>{{ notice.summary }}</p>
            <span class="read-more">阅读全文 <el-icon><Right /></el-icon></span>
          </div>
          <div v-if="notice.coverUrl" class="notice-thumb">
            <img :src="notice.coverUrl" :alt="notice.title" loading="lazy" />
          </div>
        </router-link>
      </div>
      <el-empty v-else description="暂无公告" />

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
  </div>
</template>

<style scoped>
.notice-hero {
  padding: 80px 0 66px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.2), transparent),
    var(--paper-deep);
  border-bottom: 1px solid var(--line);
}

.notice-hero h1 {
  margin-top: 16px;
  color: var(--ink);
  font-size: clamp(42px, 5vw, 64px);
  font-weight: 500;
  letter-spacing: 0.08em;
}

.notice-hero p {
  margin-top: 16px;
  color: var(--text-soft);
  font-size: 13px;
}

.notice-content {
  padding: 46px 0 90px;
  max-width: 980px;
}

.notice-list {
  border-top: 1px solid var(--line);
}

.notice-card {
  display: flex;
  align-items: center;
  gap: 30px;
  padding: 28px 8px;
  border-bottom: 1px solid var(--line);
  transition: padding 0.25s ease;
}

.notice-card:hover {
  padding-left: 16px;
}

.notice-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 68px;
  flex-shrink: 0;
}

.notice-date strong {
  color: var(--gold);
  font-size: 40px;
  font-weight: 400;
  line-height: 1;
}

.notice-date span {
  margin-top: 7px;
  color: var(--muted);
  font-size: 9px;
  letter-spacing: 0.08em;
}

.notice-main {
  flex: 1;
  min-width: 0;
}

.notice-main h3 {
  color: var(--ink);
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.03em;
}

.notice-main p {
  margin-top: 9px;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.8;
}

.read-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  color: var(--gold-deep);
  font-size: 10.5px;
}

.notice-thumb {
  width: 160px;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  border-radius: 10px;
  flex-shrink: 0;
}

.notice-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}

.notice-card:hover .notice-thumb img {
  transform: scale(1.05);
}

@media (max-width: 640px) {
  .notice-card {
    align-items: flex-start;
    gap: 16px;
  }

  .notice-date {
    width: 52px;
  }

  .notice-date strong {
    font-size: 30px;
  }

  .notice-thumb {
    display: none;
  }

  .notice-main h3 {
    font-size: 18px;
  }
}
</style>
