<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { getExhibitions, getMuseums } from '@/api/public'
import type { Exhibition, Museum } from '@/types'

const exhibitions = ref<Exhibition[]>([])
const museums = ref<Museum[]>([])
const total = ref(0)
const page = ref(1)
const size = 9
const museumId = ref<number | undefined>(undefined)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getExhibitions({
      page: page.value,
      size,
      museumId: museumId.value,
    })
    exhibitions.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function filter() {
  page.value = 1
  load()
}

function changePage(value: number) {
  page.value = value
  load()
}

onMounted(async () => {
  const museumData = await getMuseums({ page: 1, size: 50 })
  museums.value = museumData.records
  load()
})
</script>

<template>
  <div class="exhibition-page">
    <section class="exhibition-hero">
      <div class="container">
        <span class="eyebrow">EXHIBITIONS</span>
        <h1 class="serif">展览一览</h1>
        <p>书画、青铜、江南工艺与城市记忆。每一场展览，都是一次重新组织世界的方式。</p>
      </div>
    </section>

    <div class="container content">
      <div class="filter-row">
        <div class="filter-tabs">
          <button :class="{ active: museumId === undefined }" @click="museumId = undefined; filter()">
            全部
          </button>
          <button
            v-for="museum in museums"
            :key="museum.id"
            :class="{ active: museumId === museum.id }"
            @click="museumId = museum.id; filter()"
          >
            {{ museum.name }}
          </button>
        </div>
        <span class="count">共 {{ total }} 场展览</span>
      </div>

      <div v-if="loading" class="grid">
        <div v-for="i in 3" :key="i" class="loading-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 280px" />
              <div style="padding: 24px">
                <el-skeleton-item variant="h3" style="width: 60%" />
                <el-skeleton-item variant="text" style="margin-top: 14px" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <div v-else-if="exhibitions.length" class="grid">
        <router-link
          v-for="exhibition in exhibitions"
          :key="exhibition.id"
          :to="`/museums/${exhibition.museumId}`"
          class="exhibition-card hover-lift"
        >
          <div class="cover">
            <img v-if="exhibition.coverUrl" :src="exhibition.coverUrl" :alt="exhibition.name" loading="lazy" />
            <span class="museum-tag">{{ exhibition.museumName }}</span>
          </div>
          <div class="card-body">
            <span class="date">
              {{ dayjs(exhibition.startDate).format('YYYY.MM.DD') }} — {{ dayjs(exhibition.endDate).format('YYYY.MM.DD') }}
            </span>
            <h3 class="serif">{{ exhibition.name }}</h3>
            <p>{{ exhibition.description }}</p>
            <span class="link">
              查看所属博物馆
              <el-icon><Right /></el-icon>
            </span>
            <span class="not-bookable">展览不单独售票，入馆请预约博物馆时段</span>
          </div>
        </router-link>
      </div>

      <el-empty v-else description="暂无展览" />

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
.exhibition-hero {
  position: relative;
  padding: 90px 0 76px;
  overflow: hidden;
  background:
    radial-gradient(circle at 78% 30%, rgba(176, 138, 79, 0.22), transparent 34%),
    linear-gradient(150deg, #1a2336, #111827);
  color: #fff;
}

.exhibition-hero::after {
  content: 'EXHIBITION';
  position: absolute;
  right: 4%;
  bottom: -34px;
  color: rgba(255, 255, 255, 0.035);
  font-family: var(--font-serif);
  font-size: clamp(80px, 16vw, 220px);
  letter-spacing: 0.06em;
  pointer-events: none;
}

.eyebrow {
  color: var(--gold-light);
}

.exhibition-hero h1 {
  margin-top: 16px;
  font-size: clamp(46px, 6vw, 74px);
  font-weight: 500;
  letter-spacing: 0.08em;
}

.exhibition-hero p {
  max-width: 560px;
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.48);
  font-size: 13px;
  line-height: 1.9;
}

.content {
  padding: 44px 0 90px;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 32px;
}

.filter-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-tabs button {
  padding: 8px 16px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: transparent;
  color: var(--text-soft);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.filter-tabs button:hover {
  border-color: var(--gold-light);
}

.filter-tabs button.active {
  border-color: var(--ink);
  background: var(--ink);
  color: var(--paper);
}

.count {
  color: var(--muted);
  font-size: 11px;
  white-space: nowrap;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.loading-card,
.exhibition-card {
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.cover {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--paper-deep);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s ease;
}

.exhibition-card:hover .cover img {
  transform: scale(1.06);
}

.museum-tag {
  position: absolute;
  left: 16px;
  top: 16px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(14, 19, 31, 0.55);
  color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  font-size: 10px;
}

.not-bookable {
  display: block;
  margin-top: 8px;
  color: var(--muted);
  font-size: 9.5px;
}

.card-body {
  padding: 24px;
}

.date {
  color: var(--gold-deep);
  font-size: 9.5px;
  letter-spacing: 0.08em;
}

.card-body h3 {
  margin-top: 10px;
  color: var(--ink);
  font-size: 23px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.card-body p {
  display: -webkit-box;
  margin-top: 10px;
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.8;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 20px;
  color: var(--gold-deep);
  font-size: 11px;
}

@media (max-width: 960px) {
  .grid {
    grid-template-columns: 1fr 1fr;
  }

  .filter-row {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .exhibition-hero {
    padding: 64px 0 54px;
  }
}
</style>
