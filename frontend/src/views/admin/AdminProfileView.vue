<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import ImageUpload from '@/components/ImageUpload.vue'
import { updateProfileApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

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
})
</script>

<template>
  <div class="admin-profile-page">
    <div class="profile-head">
      <span class="eyebrow">MY ACCOUNT</span>
      <h1 class="serif">个人资料</h1>
      <p>维护管理员账号的实名信息与联系方式。</p>
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
            <h2 class="serif">账号信息</h2>
            <p>填写后可自动带入管理操作，减少重复输入。</p>
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
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-profile-page {
  max-width: 1100px;
}

.profile-head {
  margin-bottom: 24px;
}

.profile-head h1 {
  margin-top: 12px;
  color: var(--ink);
  font-size: clamp(28px, 3vw, 38px);
  font-weight: 500;
  letter-spacing: 0.05em;
}

.profile-head p {
  margin-top: 8px;
  color: var(--muted);
  font-size: 12.5px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  align-items: start;
}

.profile-card {
  padding: 28px 22px;
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
  width: 76px;
  height: 76px;
  margin: 0 auto 16px;
  overflow: hidden;
  border: 1px solid rgba(213, 187, 139, 0.5);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.05);
  color: var(--gold-light);
  font-family: var(--font-serif);
  font-size: 30px;
}

.avatar-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload {
  width: 160px;
}

.profile-card h2 {
  color: #f7f3eb;
  font-size: 22px;
  font-weight: 500;
  letter-spacing: 0.06em;
}

.username {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.38);
  font-size: 10.5px;
}

.role-tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.role-tags :deep(.el-tag) {
  border-color: rgba(213, 187, 139, 0.35);
  background: rgba(213, 187, 139, 0.08);
  color: var(--gold-light);
}

.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 20px;
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
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: #fffdf9;
  box-shadow: var(--shadow-sm);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.card-head h2 {
  color: var(--ink);
  font-size: 22px;
  font-weight: 600;
}

.card-head p {
  margin-top: 6px;
  color: var(--muted);
  font-size: 11px;
}

.head-icon {
  color: var(--gold);
  font-size: 28px;
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

@media (max-width: 900px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
