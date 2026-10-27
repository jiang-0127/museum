<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { adminConfigs, updateConfig } from '@/api/admin'
import type { SystemConfig } from '@/types'

const configs = ref<SystemConfig[]>([])
const values = reactive<Record<string, string>>({})
const loading = ref(false)
const savingKey = ref('')

const icons: Record<string, string> = {
  site_name: 'Monitor',
  visit_notice: 'ChatDotRound',
  max_visitors_per_order: 'User',
  advance_booking_days: 'Calendar',
  payment_timeout_minutes: 'Timer',
  maintenance_mode: 'Warning',
}

const configList = computed(() => configs.value)

async function load() {
  loading.value = true
  try {
    configs.value = await adminConfigs()
    configs.value.forEach((item) => {
      values[item.configKey] = item.configValue
    })
  } finally {
    loading.value = false
  }
}

async function save(item: SystemConfig) {
  savingKey.value = item.configKey
  try {
    await updateConfig(item.configKey, values[item.configKey] ?? '')
    item.configValue = values[item.configKey]
    ElMessage.success('参数已保存')
  } finally {
    savingKey.value = ''
  }
}

function changed(item: SystemConfig) {
  return values[item.configKey] !== item.configValue
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="SYSTEM SETTINGS"
      title="参数配置"
      desc="调整站点名称、预约提示、单笔人数与支付时限。修改保存后对新请求生效。"
    >
      <el-button :loading="loading" plain @click="load">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </PageHeader>

    <div class="config-intro">
      <el-icon><InfoFilled /></el-icon>
      <div>
        <strong>配置说明</strong>
        <p>系统参数保存在数据库中，修改会记录到操作日志。维护模式开关当前作为展示参数，正式启用前可扩展为全站拦截。</p>
      </div>
    </div>

    <div v-loading="loading" class="config-grid">
      <div v-for="item in configList" :key="item.configKey" class="config-card">
        <div class="config-head">
          <div class="config-icon">
            <el-icon><component :is="icons[item.configKey] || 'Setting'" /></el-icon>
          </div>
          <div>
            <h3>{{ item.description || item.configKey }}</h3>
            <code>{{ item.configKey }}</code>
          </div>
          <el-tag v-if="changed(item)" type="warning" size="small" effect="plain" round>未保存</el-tag>
        </div>

        <div class="config-body">
          <el-input
            v-if="item.configKey === 'visit_notice'"
            v-model="values[item.configKey]"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
          />
          <el-input
            v-else-if="item.configKey === 'maintenance_mode'"
            :model-value="values[item.configKey]"
            disabled
          >
            <template #append>
              <el-switch
                v-model="values[item.configKey]"
                active-value="true"
                inactive-value="false"
                :disabled="true"
              />
            </template>
          </el-input>
          <el-input
            v-else-if="
              ['max_visitors_per_order', 'advance_booking_days', 'payment_timeout_minutes'].includes(
                item.configKey,
              )
            "
            v-model="values[item.configKey]"
            type="number"
            min="1"
          >
            <template #append>
              {{
                item.configKey === 'max_visitors_per_order'
                  ? '人'
                  : item.configKey === 'advance_booking_days'
                    ? '天'
                    : '分钟'
              }}
            </template>
          </el-input>
          <el-input v-else v-model="values[item.configKey]" maxlength="200" />
        </div>

        <div class="config-foot">
          <span>保存后立即生效</span>
          <el-button
            type="primary"
            plain
            size="small"
            :disabled="!changed(item)"
            :loading="savingKey === item.configKey"
            @click="save(item)"
          >
            保存
          </el-button>
        </div>
      </div>
      <el-empty v-if="!configList.length && !loading" description="暂无参数配置" />
    </div>
  </div>
</template>

<style scoped>
.config-intro {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px 18px;
  border: 1px solid #e8dcc4;
  border-radius: 12px;
  background: #fbf5e8;
  color: #8b754f;
}

.config-intro > .el-icon {
  margin-top: 2px;
  font-size: 17px;
}

.config-intro strong {
  display: block;
  color: var(--text-soft);
  font-size: 12px;
}

.config-intro p {
  margin-top: 4px;
  font-size: 10.5px;
  line-height: 1.7;
}

.config-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  min-height: 200px;
}

.config-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.config-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.config-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 10px;
  background: #f8f1e4;
  color: var(--gold-deep);
  font-size: 18px;
}

.config-head > div:nth-child(2) {
  flex: 1;
}

.config-head h3 {
  color: var(--ink);
  font-size: 13px;
  font-weight: 600;
}

.config-head code {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 9.5px;
}

.config-body {
  flex: 1;
  padding: 18px 0;
}

.config-body :deep(.el-textarea__inner),
.config-body :deep(.el-input__wrapper) {
  font-size: 12px;
}

.config-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.config-foot span {
  color: var(--muted);
  font-size: 9.5px;
}

@media (max-width: 900px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
