<script setup lang="ts">
defineProps<{
  title: string
  desc?: string
  eyebrow?: string
}>()
</script>

<template>
  <div class="admin-page-head">
    <div>
      <span v-if="eyebrow" class="page-eyebrow">{{ eyebrow }}</span>
      <h1 class="serif">{{ title }}</h1>
      <p v-if="desc">{{ desc }}</p>
    </div>
    <div class="page-actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.admin-page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.page-eyebrow {
  display: inline-block;
  margin-bottom: 6px;
  color: var(--gold-deep);
  font-size: 10px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

h1 {
  color: var(--ink);
  font-size: clamp(24px, 2.4vw, 32px);
  font-weight: 600;
  letter-spacing: 0.03em;
}

p {
  max-width: 620px;
  margin-top: 8px;
  color: var(--muted);
  font-size: 12.5px;
  line-height: 1.7;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 2px;
}

@media (max-width: 700px) {
  .admin-page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
  }
}
</style>
