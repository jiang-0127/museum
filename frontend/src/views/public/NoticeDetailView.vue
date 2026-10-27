<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { getLatestNotices, getNotice } from '@/api/public'
import type { Notice } from '@/types'

const route = useRoute()
const notice = ref<Notice | null>(null)
const latest = ref<Notice[]>([])
const loading = ref(true)

onMounted(async () => {
  loading.value = true
  try {
    const [data, latestData] = await Promise.all([
      getNotice(route.params.id as string),
      getLatestNotices(4),
    ])
    notice.value = data
    latest.value = latestData.filter((item) => item.id !== data.id).slice(0, 3)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="notice-detail-page">
    <div v-if="loading" class="container loading">
      <el-skeleton :rows="10" animated />
    </div>
    <template v-else-if="notice">
      <section class="detail-hero">
        <div class="container">
          <router-link to="/notices" class="back-link">
            <el-icon><Back /></el-icon>
            返回公告列表
          </router-link>
          <span class="eyebrow">VISITOR NOTICE</span>
          <h1 class="serif">{{ notice.title }}</h1>
          <div class="meta">
            <span><el-icon><Calendar /></el-icon>{{ notice.publishTime ? dayjs(notice.publishTime).format('YYYY 年 MM 月 DD 日') : '—' }}</span>
            <span><el-icon><CollectionTag /></el-icon>参观公告</span>
          </div>
        </div>
      </section>

      <div class="container detail-layout">
        <article class="article">
          <img v-if="notice.coverUrl" :src="notice.coverUrl" :alt="notice.title" class="cover" />
          <p v-if="notice.summary" class="lead">{{ notice.summary }}</p>
          <div class="content">{{ notice.content }}</div>
          <div class="article-foot">
            <span>云观博物馆预约平台</span>
            <router-link to="/notices">更多公告 <el-icon><Right /></el-icon></router-link>
          </div>
        </article>

        <aside class="sidebar">
          <h3 class="serif">更多公告</h3>
          <router-link v-for="item in latest" :key="item.id" :to="`/notices/${item.id}`" class="side-item">
            <span>{{ item.publishTime ? dayjs(item.publishTime).format('MM.DD') : '' }}</span>
            <strong>{{ item.title }}</strong>
          </router-link>
          <router-link to="/museums" class="side-cta">
            查看博物馆余票
            <el-icon><Right /></el-icon>
          </router-link>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.loading {
  padding: 80px 0;
}

.detail-hero {
  padding: 72px 0 52px;
  background:
    radial-gradient(circle at 86% 20%, rgba(176, 138, 79, 0.14), transparent 36%),
    var(--paper-deep);
  border-bottom: 1px solid var(--line);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 30px;
  color: var(--muted);
  font-size: 11px;
}

.back-link:hover {
  color: var(--gold-deep);
}

.detail-hero h1 {
  max-width: 900px;
  margin-top: 16px;
  color: var(--ink);
  font-size: clamp(32px, 4.2vw, 52px);
  font-weight: 500;
  line-height: 1.4;
  letter-spacing: 0.03em;
}

.meta {
  display: flex;
  gap: 24px;
  margin-top: 24px;
  color: var(--muted);
  font-size: 11px;
}

.meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 70px;
  padding: 54px 0 90px;
  align-items: start;
}

.article {
  min-width: 0;
}

.cover {
  width: 100%;
  max-height: 440px;
  margin-bottom: 36px;
  border-radius: 16px;
  object-fit: cover;
}

.lead {
  padding: 18px 22px;
  border-left: 3px solid var(--gold);
  background: #fbf6eb;
  color: var(--text-soft);
  font-size: 14px;
  line-height: 1.9;
}

.content {
  margin-top: 28px;
  color: #3d3b36;
  font-size: 14.5px;
  line-height: 2.2;
  white-space: pre-line;
}

.article-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 48px;
  padding-top: 24px;
  border-top: 1px solid var(--line);
  color: var(--muted);
  font-size: 11px;
}

.article-foot a {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--gold-deep);
}

.sidebar {
  position: sticky;
  top: 110px;
  padding-top: 8px;
}

.sidebar h3 {
  margin-bottom: 16px;
  color: var(--ink);
  font-size: 21px;
  font-weight: 600;
}

.side-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}

.side-item span {
  color: var(--gold-deep);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.side-item strong {
  color: var(--text-soft);
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.7;
  transition: color 0.2s ease;
}

.side-item:hover strong {
  color: var(--ink);
}

.side-cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24px;
  padding: 16px 18px;
  border-radius: 10px;
  background: var(--ink);
  color: var(--gold-light);
  font-size: 11.5px;
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
    gap: 42px;
  }

  .sidebar {
    position: static;
  }
}
</style>
