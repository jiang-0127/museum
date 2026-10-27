<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import ImageUpload from '@/components/ImageUpload.vue'
import { updateProfileApi } from '@/api/auth'
import { getMyReservations } from '@/api/reservation'
import { useAuthStore } from '@/stores/auth'
import type { Reservation } from '@/types'

const auth = useAuthStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const reservations = ref<Reservation[]>([])

const form = reactive({
  realName: '',
  phone: '',
  idCard: '',
  email: '',
  avatar: '',
})

const rules: FormRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  idCard: [
    {
      pattern: /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$/,
      message: '身份证号格式不正确',
      trigger: 'blur',
    },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const visitedCount = ref(0)
const reservedCount = ref(0)

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const user = await updateProfileApi({
      realName: form.realName,
      phone: form.phone || undefined,
      idCard: form.idCard || undefined,
      email: form.email || undefined,
      avatar: form.avatar || undefined,
    })
    auth.setUser(user)
    ElMessage.success('个人资料已更新')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const user = await auth.fetchMe()
  form.realName = user.realName || ''
  form.phone = user.phone || ''
  form.idCard = user.idCard || ''
  form.email = user.email || ''
  form.avatar = user.avatar || ''
  try {
    const data = await getMyReservations({ page: 1, size: 100 })
    reservations.value = data.records
    visitedCount.value = data.records.filter((item) => item.status === 'CHECKED_IN').length
    reservedCount.value = data.records.filter((item) => item.status === 'RESERVED').length
  } catch {
    // 统计信息失败不影响资料展示
  }
})
</script>

<template>
  <div class="profile-page">
    <div class="container">
      <div class="profile-head">
        <span class="eyebrow">MY ACCOUNT</span>
        <h1 class="serif">个人资料</h1>
        <p>实名信息将用于预约与入馆核验，请确保真实准确。</p>
      </div>

      <div class="profile-layout">
        <aside class="profile-card">
          <div class="avatar-large">
            <img v-if="auth.user?.avatar" :src="auth.user.avatar" :alt="auth.displayName" />
            <template v-else>{{ auth.displayName.slice(0, 1) }}</template>
          </div>
          <h2 class="serif">{{ auth.displayName }}</h2>
          <span class="username">@{{ auth.user?.username }}</span>
          <div class="role-tags">
            <el-tag
              v-for="name in auth.user?.roleNames"
              :key="name"
              effect="plain"
              round
              size="small"
            >
              {{ name }}
            </el-tag>
          </div>

          <div class="profile-stats">
            <div>
              <strong class="serif">{{ reservedCount }}</strong>
              <span>待参观</span>
            </div>
            <div>
              <strong class="serif">{{ visitedCount }}</strong>
              <span>已参观</span>
            </div>
            <div>
              <strong class="serif">{{ reservations.length }}</strong>
              <span>总预约</span>
            </div>
          </div>

          <div class="profile-meta">
            <div>
              <span>注册时间</span>
              <strong>{{ auth.user?.createdAt ? dayjs(auth.user.createdAt).format('YYYY-MM-DD') : '—' }}</strong>
            </div>
            <div>
              <span>最近登录</span>
              <strong>{{ auth.user?.lastLoginTime ? dayjs(auth.user.lastLoginTime).format('YYYY-MM-DD HH:mm') : '—' }}</strong>
            </div>
          </div>
        </aside>

        <div class="form-card">
          <div class="card-head">
            <div>
              <h2 class="serif">实名信息</h2>
              <p>填写后可自动带入预约表单，减少重复输入。</p>
            </div>
            <el-icon class="head-icon"><Postcard /></el-icon>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
            <el-form-item label="头像">
              <div class="avatar-upload">
                <ImageUpload v-model="form.avatar" height="128px" tip="上传本地头像，建议使用正方形图片" />
              </div>
            </el-form-item>
            <div class="form-grid">
              <el-form-item label="真实姓名" prop="realName">
                <el-input v-model="form.realName" placeholder="请输入真实姓名" />
              </el-form-item>
              <el-form-item label="用户名">
                <el-input :model-value="auth.user?.username" disabled />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" placeholder="请输入邮箱" />
              </el-form-item>
            </div>
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" />
            </el-form-item>
            <el-button type="primary" class="save-btn" :loading="saving" @click="save">保存修改</el-button>
          </el-form>

          <div class="security-note">
            <el-icon><Lock /></el-icon>
            <div>
              <strong>隐私说明</strong>
              <p>身份证号仅用于参观预约与入馆核验，在订单详情与管理后台均以脱敏形式展示。</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 70vh;
  padding: 54px 0 90px;
  background:
    radial-gradient(circle at 12% 0%, rgba(176, 138, 79, 0.08), transparent 30%),
    var(--paper);
}

.profile-head {
  margin-bottom: 32px;
}

.profile-head h1 {
  margin-top: 12px;
  color: var(--ink);
  font-size: clamp(34px, 4vw, 48px);
  font-weight: 500;
  letter-spacing: 0.05em;
}

.profile-head p {
  margin-top: 10px;
  color: var(--muted);
  font-size: 12.5px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 310px 1fr;
  gap: 26px;
  align-items: start;
}

.profile-card {
  padding: 32px 26px;
  border-radius: 16px;
  background:
    radial-gradient(circle at 90% 0%, rgba(176, 138, 79, 0.28), transparent 42%),
    linear-gradient(155deg, #1c2639, #111827);
  color: rgba(255, 255, 255, 0.7);
  text-align: center;
  box-shadow: var(--shadow-md);
}

.avatar-large {
  display: grid;
  place-items: center;
  width: 86px;
  height: 86px;
  margin: 0 auto 18px;
  overflow: hidden;
  border: 1px solid rgba(213, 187, 139, 0.5);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  color: var(--gold-light);
  font-family: var(--font-serif);
  font-size: 34px;
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload {
  width: 168px;
}

.profile-card h2 {
  color: #f7f3eb;
  font-size: 26px;
  font-weight: 500;
  letter-spacing: 0.06em;
}

.username {
  display: block;
  margin-top: 5px;
  color: rgba(255, 255, 255, 0.38);
  font-size: 10.5px;
  letter-spacing: 0.06em;
}

.role-tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}

.role-tags :deep(.el-tag) {
  border-color: rgba(213, 187, 139, 0.35);
  background: rgba(213, 187, 139, 0.08);
  color: var(--gold-light);
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 26px 0;
  padding: 18px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.profile-stats div {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.profile-stats strong {
  color: var(--gold-light);
  font-size: 26px;
  font-weight: 500;
}

.profile-stats span {
  color: rgba(255, 255, 255, 0.38);
  font-size: 9.5px;
}

.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
}

.profile-meta div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 10px;
}

.profile-meta span {
  color: rgba(255, 255, 255, 0.35);
}

.profile-meta strong {
  color: rgba(255, 255, 255, 0.66);
  font-weight: 400;
}

.form-card {
  padding: 32px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 26px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.card-head h2 {
  color: var(--ink);
  font-size: 25px;
  font-weight: 600;
}

.card-head p {
  margin-top: 7px;
  color: var(--muted);
  font-size: 11px;
}

.head-icon {
  color: var(--gold);
  font-size: 30px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.save-btn {
  width: 160px;
  height: 44px;
  margin-top: 6px;
}

.security-note {
  display: flex;
  gap: 12px;
  margin-top: 28px;
  padding: 16px;
  border-radius: 10px;
  background: #f8f1e4;
  color: #8b754f;
}

.security-note > .el-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.security-note strong {
  display: block;
  margin-bottom: 4px;
  color: var(--text-soft);
  font-size: 11px;
}

.security-note p {
  font-size: 10.5px;
  line-height: 1.8;
}

@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
