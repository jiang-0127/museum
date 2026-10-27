<script setup lang="ts">
import { computed } from 'vue'

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

const props = defineProps<{
  status?: string
  text?: string
}>()

const statusMap: Record<string, { label: string; type: TagType }> = {
  PENDING_PAYMENT: { label: '待支付', type: 'warning' },
  RESERVED: { label: '待参观', type: 'success' },
  CHECKED_IN: { label: '已核销', type: 'primary' },
  CANCELED: { label: '已取消', type: 'info' },
  EXPIRED: { label: '已过期', type: 'info' },
  UNPAID: { label: '未支付', type: 'warning' },
  PAID: { label: '已支付', type: 'success' },
  REFUNDED: { label: '已退款', type: 'info' },
  CLOSED: { label: '已关闭', type: 'info' },
  SUCCESS: { label: '成功', type: 'success' },
}

const config = computed(
  () => statusMap[props.status || ''] || { label: props.status || '未知', type: 'info' as TagType },
)
</script>

<template>
  <el-tag :type="config.type" effect="plain" round class="status-tag">
    {{ text || config.label }}
  </el-tag>
</template>

<style scoped>
.status-tag {
  font-size: 11px;
  letter-spacing: 0.04em;
}
</style>
