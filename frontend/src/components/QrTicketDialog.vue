<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import type { Ticket } from '@/types'

const props = defineProps<{
  modelValue: boolean
  ticket: Ticket | null
}>()

const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const showCode = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const dateText = computed(() => {
  if (!props.ticket) return ''
  return dayjs(props.ticket.visitDate).format('YYYY 年 MM 月 DD 日')
})

async function copyCode() {
  if (!props.ticket) return
  try {
    await navigator.clipboard.writeText(props.ticket.ticketCode)
    ElMessage.success('票码已复制')
  } catch {
    showCode.value = true
    ElMessage.info('请手动选择票码复制')
  }
}
</script>

<template>
  <el-dialog v-model="visible" width="520px" class="ticket-dialog" :show-close="true">
    <div v-if="ticket" class="ticket-wrap">
      <div class="ticket-top">
        <div>
          <span class="ticket-eyebrow">ADMISSION TICKET</span>
          <h3 class="serif">{{ ticket.museumName }}</h3>
          <p v-if="ticket.exhibitionName">{{ ticket.exhibitionName }}</p>
        </div>
        <span class="ticket-order">{{ ticket.orderNo }}</span>
      </div>

      <div class="ticket-body">
        <div class="ticket-info">
          <div>
            <span>参观日期</span>
            <strong class="serif">{{ dateText }}</strong>
          </div>
          <div>
            <span>入场方式</span>
            <strong class="serif">
              {{ ticket.startTime && ticket.endTime ? `${ticket.startTime.slice(0, 5)} - ${ticket.endTime.slice(0, 5)}` : '开放时间内任意时间' }}
            </strong>
          </div>
          <div>
            <span>参观人数</span>
            <strong>{{ ticket.visitorCount }} 人</strong>
          </div>
          <div>
            <span>联系人</span>
            <strong>{{ ticket.contactName }}</strong>
          </div>
        </div>
        <div class="ticket-qr">
          <img :src="ticket.qrImage" alt="电子票二维码" />
          <small>入馆时出示此码</small>
        </div>
      </div>

      <div class="ticket-foot">
        <div class="code-line">
          <span>票码</span>
          <code v-if="showCode">{{ ticket.ticketCode }}</code>
          <code v-else>{{ ticket.ticketCode.slice(0, 18) }}······</code>
        </div>
        <el-button text size="small" @click="showCode = !showCode">
          {{ showCode ? '收起' : '展开完整票码' }}
        </el-button>
        <el-button text size="small" @click="copyCode">复制票码</el-button>
      </div>
      <p class="ticket-tip">
        请于参观当天携带本人有效身份证件，在预约时段内到馆核验。此票仅为一次入场凭证。
      </p>
    </div>
  </el-dialog>
</template>

<style scoped>
.ticket-wrap {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  background:
    radial-gradient(circle at 100% 0, rgba(176, 138, 79, 0.18), transparent 36%),
    linear-gradient(145deg, #1a2336, #111827);
  color: rgba(255, 255, 255, 0.82);
  padding: 28px;
}

.ticket-wrap::before,
.ticket-wrap::after {
  content: '';
  position: absolute;
  top: 56%;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--paper-light);
  transform: translateY(-50%);
}

.ticket-wrap::before {
  left: -11px;
}

.ticket-wrap::after {
  right: -11px;
}

.ticket-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.ticket-eyebrow {
  color: var(--gold-light);
  font-size: 9px;
  letter-spacing: 0.26em;
}

.ticket-top h3 {
  margin-top: 8px;
  color: #f6f0e4;
  font-size: 26px;
  letter-spacing: 0.06em;
}

.ticket-top p {
  margin-top: 6px;
  color: rgba(255, 255, 255, 0.48);
  font-size: 12px;
}

.ticket-order {
  color: rgba(255, 255, 255, 0.36);
  font-size: 10px;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

.ticket-body {
  display: grid;
  grid-template-columns: 1fr 168px;
  gap: 24px;
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px dashed rgba(255, 255, 255, 0.18);
}

.ticket-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.ticket-info div {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.ticket-info span {
  color: rgba(255, 255, 255, 0.42);
  font-size: 10px;
  letter-spacing: 0.1em;
}

.ticket-info strong {
  color: #f3ead9;
  font-size: 15px;
  font-weight: 500;
}

.ticket-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 10px;
  background: #fffdf8;
}

.ticket-qr img {
  width: 100%;
  aspect-ratio: 1;
}

.ticket-qr small {
  color: #7f7767;
  font-size: 9px;
}

.ticket-foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px dashed rgba(255, 255, 255, 0.18);
}

.code-line {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
  color: rgba(255, 255, 255, 0.4);
  font-size: 10px;
}

.code-line code {
  overflow: hidden;
  color: rgba(255, 255, 255, 0.72);
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-foot :deep(.el-button) {
  color: var(--gold-light);
  font-size: 11px;
}

.ticket-tip {
  margin-top: 16px;
  color: rgba(255, 255, 255, 0.34);
  font-size: 10.5px;
  line-height: 1.8;
}

@media (max-width: 560px) {
  .ticket-body {
    grid-template-columns: 1fr;
  }

  .ticket-qr {
    width: 180px;
    margin: 0 auto;
  }

  .ticket-foot {
    flex-wrap: wrap;
  }
}
</style>
