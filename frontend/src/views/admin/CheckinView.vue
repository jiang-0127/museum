<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import { checkinRecords, checkinVerify } from '@/api/admin'
import type { CheckinRecord, CheckinResult } from '@/types'

const ticketCode = ref('')
const gate = ref('正门闸机')
const result = ref<CheckinResult | null>(null)
const processing = ref(false)
const records = ref<CheckinRecord[]>([])
const recordsLoading = ref(false)

async function loadRecords() {
  recordsLoading.value = true
  try {
    const data = await checkinRecords({ page: 1, size: 8 })
    records.value = data.records
  } finally {
    recordsLoading.value = false
  }
}

async function submit() {
  const code = ticketCode.value.trim()
  if (!code) {
    ElMessage.info('请输入电子票票码')
    return
  }
  processing.value = true
  result.value = null
  try {
    result.value = await checkinVerify({
      ticketCode: code,
      gate: gate.value.trim() || undefined,
    })
    ticketCode.value = ''
    await loadRecords()
  } catch (error) {
    result.value = {
      success: false,
      message: (error as Error).message || '核销失败，请核对票码',
    }
  } finally {
    processing.value = false
  }
}

onMounted(loadRecords)
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="TICKET CHECK-IN"
      title="票码核销"
      desc="输入游客电子票中的票码完成核销。核销成功后订单状态将自动更新，并写入核销记录。"
    >
      <el-tag type="success" effect="plain" round>票码核销</el-tag>
    </PageHeader>

    <div class="checkin-layout">
      <div class="display-card">
        <div class="card-head">
          <div>
            <h3>电子票票码核销</h3>
            <p>二维码仅作为游客端电子票展示，管理员端通过票码完成核销，不再调用摄像头扫码。</p>
          </div>
          <span class="display-mark">TICKET CODE</span>
        </div>

        <div class="gate-row">
          <label>闸机名称</label>
          <el-input v-model="gate" placeholder="正门闸机" style="width: 220px">
            <template #prefix><el-icon><Location /></el-icon></template>
          </el-input>
        </div>

        <div class="input-box">
          <label>电子票票码</label>
          <el-input
            v-model="ticketCode"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="请粘贴以 MUSEUM-TICKET: 开头的电子票票码"
            @keyup.ctrl.enter="submit"
          />
          <div class="input-foot">
            <small>核销后不可重复入场，电子票仅限参观当天有效</small>
            <el-button type="primary" class="display-btn" :loading="processing" @click="submit">
              <el-icon><CircleCheck /></el-icon>
              核销电子票
            </el-button>
          </div>
        </div>

        <transition name="fade-slide">
          <div v-if="result" class="result-card" :class="result.success ? 'success' : 'error'">
            <div class="result-icon">
              <el-icon><CircleCheck v-if="result.success" /><CircleClose v-else /></el-icon>
            </div>
            <div class="result-copy">
              <strong>{{ result.success ? '核销成功' : '核销失败' }}</strong>
              <p>{{ result.message }}</p>
              <template v-if="result.success">
                <div class="result-grid">
                  <span>订单号<strong>{{ result.orderNo }}</strong></span>
                  <span>博物馆<strong>{{ result.museumName }}</strong></span>
                  <span>联系人<strong>{{ result.contactName }}</strong></span>
                  <span>参观人数<strong>{{ result.visitorCount }} 人</strong></span>
                  <span>
                    参观时段
                    <strong>
                      {{ result.visitDate }} {{ result.startTime?.slice(0, 5) }} -
                      {{ result.endTime?.slice(0, 5) }}
                    </strong>
                  </span>
                  <span>核销时间<strong>{{ result.checkinTime ? dayjs(result.checkinTime).format('HH:mm:ss') : '' }}</strong></span>
                </div>
              </template>
            </div>
          </div>
        </transition>

        <div v-if="!result" class="empty-result">
          <div class="empty-icon"><el-icon><Tickets /></el-icon></div>
          <strong>等待票码</strong>
          <p>输入游客电子票中的票码，核销结果会显示在这里。</p>
        </div>
      </div>

      <aside class="records-card">
        <div class="records-head">
          <div>
            <span class="eyebrow">RECENT RECORDS</span>
            <h3 class="serif">最近核销记录</h3>
            <small>最新 8 条票码核销</small>
          </div>
          <el-button text size="small" @click="loadRecords">刷新</el-button>
        </div>

        <div v-loading="recordsLoading" class="record-list">
          <div v-for="record in records" :key="record.id" class="record-item">
            <div class="record-icon"><el-icon><CircleCheck /></el-icon></div>
            <div class="record-copy">
              <strong>{{ record.orderNo }}</strong>
              <span>{{ record.gate }} · {{ record.operatorName || '系统' }}</span>
            </div>
            <time>{{ dayjs(record.checkinTime).format('HH:mm') }}</time>
          </div>
          <el-empty v-if="!records.length && !recordsLoading" description="暂无核销记录" :image-size="50" />
        </div>

        <div class="record-tip">
          <el-icon><InfoFilled /></el-icon>
          <p>仅支持票码核销，不提供二维码摄像头扫描。核销成功后会生成核销记录并更新订单状态。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.checkin-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(300px, 0.6fr);
  gap: 18px;
  align-items: start;
}

.display-card,
.records-card {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.display-card {
  padding: 24px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.card-head h3 {
  color: var(--ink);
  font-size: 16px;
  font-weight: 600;
}

.card-head p {
  margin-top: 6px;
  color: var(--muted);
  font-size: 10.5px;
}

.display-mark {
  flex-shrink: 0;
  color: #b08a4f;
  font-size: 8.5px;
  letter-spacing: 0.18em;
}

.gate-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 0 0;
}

.gate-row > label {
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 600;
}

.input-box {
  padding: 16px 0 0;
}

.input-box > label {
  display: block;
  margin-bottom: 8px;
  color: var(--text-soft);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.input-box :deep(.el-textarea__inner) {
  padding: 13px 14px;
  border-radius: 10px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11px;
  line-height: 1.7;
}

.input-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
}

.input-foot small {
  color: var(--muted);
  font-size: 9.5px;
}

.display-btn {
  min-width: 150px;
  height: 40px;
}

.empty-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  padding: 42px 20px;
  border: 1px dashed var(--line);
  border-radius: 12px;
  background: #fcfaf5;
  text-align: center;
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 50px;
  height: 50px;
  margin-bottom: 2px;
  border-radius: 50%;
  background: #f5ecdd;
  color: var(--gold-deep);
  font-size: 23px;
}

.empty-result strong {
  color: var(--text-soft);
  font-size: 12px;
}

.empty-result p {
  color: var(--muted);
  font-size: 10px;
}

.result-card {
  display: flex;
  gap: 14px;
  margin-top: 20px;
  padding: 18px;
  border: 1px solid;
  border-radius: 12px;
}

.result-card.success {
  border-color: #cfe3d4;
  background: #f4faf5;
}

.result-card.error {
  border-color: #efd3d0;
  background: #fdf6f5;
}

.result-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 50%;
  color: #fff;
  font-size: 22px;
}

.result-card.success .result-icon {
  background: #3f7a5c;
}

.result-card.error .result-icon {
  background: var(--danger);
}

.result-copy {
  flex: 1;
  min-width: 0;
}

.result-copy > strong {
  color: var(--ink);
  font-size: 14px;
}

.result-copy > p {
  margin-top: 4px;
  color: var(--text-soft);
  font-size: 11px;
}

.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 18px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #d9e6dc;
}

.result-grid span {
  display: flex;
  flex-direction: column;
  gap: 3px;
  color: var(--muted);
  font-size: 9px;
}

.result-grid strong {
  color: var(--text);
  font-size: 10.5px;
  font-weight: 500;
}

.records-card {
  padding: 24px;
}

.records-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.records-head .eyebrow {
  color: var(--gold-deep);
}

.records-head h3 {
  margin-top: 8px;
  color: var(--ink);
  font-size: 23px;
  font-weight: 500;
  letter-spacing: 0.04em;
}

.records-head small {
  display: block;
  margin-top: 5px;
  color: var(--muted);
  font-size: 9.5px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 120px;
  margin-top: 14px;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
}

.record-icon {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #eef5f0;
  color: var(--success);
  font-size: 13px;
}

.record-copy {
  flex: 1;
  min-width: 0;
}

.record-copy strong {
  display: block;
  overflow: hidden;
  color: var(--text);
  font-size: 10px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-copy span {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: var(--muted);
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-item time {
  color: var(--muted);
  font-size: 9.5px;
}

.record-tip {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  border-radius: 8px;
  background: #f8f1e4;
  color: #8b754f;
}

.record-tip > .el-icon {
  margin-top: 2px;
  flex-shrink: 0;
  font-size: 13px;
}

.record-tip p {
  font-size: 9.5px;
  line-height: 1.8;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 1000px) {
  .checkin-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .gate-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .gate-row :deep(.el-input) {
    width: 100% !important;
  }

  .input-foot {
    align-items: stretch;
    flex-direction: column;
  }

  .display-btn {
    width: 100%;
  }

  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
