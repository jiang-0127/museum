<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MuseumCard from '@/components/MuseumCard.vue'
import { getMuseums } from '@/api/public'
import type { Museum } from '@/types'

const museums = ref<Museum[]>([])
const total = ref(0)
const page = ref(1)
const size = 9
const keyword = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await getMuseums({ page: page.value, size, keyword: keyword.value || undefined })
    museums.value = data.records
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
  <div class="museum-list-page">
    <section class="page-hero">
      <div class="container hero-grid">
        <div>
          <span class="eyebrow">OUR MUSEUMS</span>
          <h1 class="serif">选择一座馆，<br />开始一次观看</h1>
          <p>
            从宫城到江南，从青铜到书画。每座博物馆都有自己的光线、节奏与叙事方式。
            查看开放时段与实时余票，预约你的参观。
          </p>
        </div>
        <div class="hero-index">
          <span>COLLECTION OF</span>
          <strong class="serif">03</strong>
          <small>MUSEUMS</small>
        </div>
      </div>
    </section>

    <section class="container content">
      <div class="toolbar">
        <div class="result-count">
          <span class="serif">{{ total }}</span>
          座博物馆
        </div>
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索博物馆名称或地址"
            clearable
            size="large"
            @keyup.enter="search"
            @clear="search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" @click="search">搜索</el-button>
        </div>
      </div>

      <div v-if="loading" class="grid">
        <div v-for="i in 3" :key="i" class="skeleton-card">
          <el-skeleton animated>
            <template #template>
              <el-skeleton-item variant="image" style="width: 100%; height: 250px; border-radius: 18px 18px 0 0" />
              <div style="padding: 24px">
                <el-skeleton-item variant="h3" style="width: 50%" />
                <el-skeleton-item variant="text" style="margin-top: 16px" />
                <el-skeleton-item variant="text" style="width: 80%; margin-top: 8px" />
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <div v-else-if="museums.length" class="grid">
        <MuseumCard v-for="(museum, index) in museums" :key="museum.id" :museum="museum" :index="index" />
      </div>

      <el-empty v-else description="没有找到匹配的博物馆">
        <el-button @click="keyword = ''; search()">查看全部</el-button>
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
    </section>

    <section class="visit-strip">
      <div class="container strip-inner">
        <div>
          <span class="eyebrow light">PLAN YOUR VISIT</span>
          <h2 class="serif">不确定去哪一座？</h2>
          <p>从正在展出的特展开始，也许更容易做决定。</p>
        </div>
        <router-link to="/exhibitions" class="strip-link">
          查看正在展出
          <el-icon><Right /></el-icon>
        </router-link>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-hero {
  padding: 86px 0 70px;
  background:
    radial-gradient(circle at 80% 20%, rgba(176, 138, 79, 0.16), transparent 35%),
    linear-gradient(180deg, #f2ecdf, #f7f3eb);
  border-bottom: 1px solid var(--line);
}

.hero-grid {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 40px;
}

h1 {
  margin-top: 18px;
  color: var(--ink);
  font-size: clamp(40px, 5vw, 64px);
  font-weight: 500;
  line-height: 1.25;
  letter-spacing: 0.04em;
}

.hero-grid p {
  max-width: 620px;
  margin-top: 24px;
  color: var(--text-soft);
  font-size: 14px;
  line-height: 2;
}

.hero-index {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  flex-shrink: 0;
}

.hero-index span {
  color: var(--muted);
  font-size: 9px;
  letter-spacing: 0.22em;
}

.hero-index strong {
  color: var(--gold);
  font-size: 90px;
  font-weight: 400;
  line-height: 1.1;
}

.hero-index small {
  color: var(--muted);
  font-size: 9px;
  letter-spacing: 0.24em;
}

.content {
  padding: 42px 0 90px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 34px;
}

.result-count {
  color: var(--muted);
  font-size: 12px;
}

.result-count span {
  margin-right: 6px;
  color: var(--ink);
  font-size: 26px;
}

.search-box {
  display: flex;
  gap: 10px;
  width: min(420px, 100%);
}

.search-box .el-input {
  flex: 1;
}

.grid,
.skeleton-card {
  display: grid;
  gap: 24px;
}

.grid {
  grid-template-columns: repeat(3, 1fr);
}

.skeleton-card {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: #fffdf9;
}

.visit-strip {
  padding: 58px 0;
  background:
    radial-gradient(circle at 85% 0%, rgba(176, 138, 79, 0.28), transparent 40%),
    var(--ink);
  color: #fff;
}

.strip-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.eyebrow.light {
  color: var(--gold-light);
}

.strip-inner h2 {
  margin-top: 12px;
  font-size: 32px;
  font-weight: 500;
  letter-spacing: 0.05em;
}

.strip-inner p {
  margin-top: 10px;
  color: rgba(255, 255, 255, 0.48);
  font-size: 12px;
}

.strip-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 24px;
  border: 1px solid rgba(213, 187, 139, 0.5);
  border-radius: 999px;
  color: var(--gold-light);
  font-size: 12px;
  white-space: nowrap;
  transition: all 0.25s ease;
}

.strip-link:hover {
  background: rgba(213, 187, 139, 0.1);
  gap: 16px;
}

@media (max-width: 960px) {
  .hero-grid {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-index {
    align-items: flex-start;
  }

  .hero-index strong {
    font-size: 56px;
  }

  .grid {
    grid-template-columns: 1fr 1fr;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-box {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .strip-inner {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-hero {
    padding: 60px 0 50px;
  }
}
</style>
