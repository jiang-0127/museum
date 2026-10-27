<script setup lang="ts">
defineProps<{
  eyebrow?: string
  title: string
  desc?: string
  linkText?: string
  to?: string
}>()
</script>

<template>
  <div class="section-head">
    <div>
      <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
      <h2 class="section-title">{{ title }}</h2>
      <p v-if="desc" class="section-desc">{{ desc }}</p>
    </div>
    <div class="head-actions">
      <slot name="actions" />
      <router-link v-if="linkText" :to="to || '/museums'" class="head-link">
        {{ linkText }}
        <el-icon><ArrowRight /></el-icon>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.head-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 6px;
}

.head-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--gold-deep);
  font-size: 13px;
  letter-spacing: 0.04em;
  white-space: nowrap;
  transition: gap 0.2s ease;
}

.head-link:hover {
  gap: 12px;
}
</style>
