<script setup lang="ts">
import { ref } from 'vue'
import type { Museum } from '@/types'

defineProps<{
  museum: Museum
  index?: number
}>()

const imageFailed = ref(false)
</script>

<template>
  <router-link :to="`/museums/${museum.id}`" class="museum-card hover-lift">
    <div class="cover" :class="{ fallback: imageFailed || !museum.coverUrl }">
      <img
        v-if="museum.coverUrl && !imageFailed"
        :src="museum.coverUrl"
        :alt="museum.name"
        loading="lazy"
        @error="imageFailed = true"
      />
      <div class="cover-shade"></div>
      <span v-if="index !== undefined" class="index">{{ String(index + 1).padStart(2, '0') }}</span>
      <span class="open-badge">
        <el-icon><Clock /></el-icon>
        {{ museum.openTime || '09:00' }} - {{ museum.closeTime || '17:00' }}
      </span>
    </div>
    <div class="card-body">
      <h3 class="serif">{{ museum.name }}</h3>
      <p class="address">
        <el-icon><Location /></el-icon>
        {{ museum.address }}
      </p>
      <p class="desc">{{ museum.description || '走进展厅，在器物与光影之间，与时间相遇。' }}</p>
      <span class="card-link">
        查看展览与余票
        <el-icon><Right /></el-icon>
      </span>
    </div>
  </router-link>
</template>

<style scoped>
.museum-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 18px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.cover {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: linear-gradient(135deg, #283146, #111827);
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s ease;
}

.museum-card:hover .cover img {
  transform: scale(1.06);
}

.cover-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(13, 18, 30, 0.08), rgba(13, 18, 30, 0.62));
}

.cover.fallback::before {
  content: 'MUSEUM';
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(213, 187, 139, 0.38);
  font-family: var(--font-serif);
  letter-spacing: 0.3em;
}

.index {
  position: absolute;
  top: 18px;
  left: 20px;
  color: rgba(255, 255, 255, 0.75);
  font-family: var(--font-serif);
  font-size: 13px;
  letter-spacing: 0.1em;
}

.index::after {
  content: '';
  display: block;
  width: 24px;
  height: 1px;
  margin-top: 8px;
  background: var(--gold-light);
}

.open-badge {
  position: absolute;
  right: 16px;
  bottom: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.82);
  background: rgba(15, 20, 32, 0.35);
  backdrop-filter: blur(8px);
  font-size: 11px;
}

.card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 24px 24px 22px;
}

h3 {
  color: var(--ink);
  font-size: 23px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.address {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  color: var(--muted);
  font-size: 12px;
}

.desc {
  display: -webkit-box;
  margin-top: 14px;
  overflow: hidden;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.8;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  color: var(--gold-deep);
  font-size: 12.5px;
  letter-spacing: 0.05em;
}

.card-link .el-icon {
  transition: transform 0.25s ease;
}

.museum-card:hover .card-link .el-icon {
  transform: translateX(4px);
}
</style>
