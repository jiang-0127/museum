<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const demoAccounts = [
  { label: '游客', username: 'visitor', password: 'Visitor@123' },
  { label: '预约管理员', username: 'manager', password: 'Manager@123' },
  { label: '系统管理员', username: 'admin', password: 'Admin@123' },
]

function fillDemo(account: (typeof demoAccounts)[number]) {
  form.username = account.username
  form.password = account.password
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const user = await auth.login(form.username, form.password)
    ElMessage.success(`欢迎回来，${user.realName || user.username}`)
    const redirect = route.query.redirect as string | undefined
    if (redirect) {
      router.push(redirect)
    } else if (auth.isAdmin) {
      router.push('/admin/dashboard')
    } else {
      router.push('/')
    }
  } catch {
    // 具体错误提示由 http 拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-visual">
      <img
        src="/images/seed/museum-2.jpg"
        alt="博物馆展厅"
      />
      <div class="visual-overlay"></div>
      <router-link to="/" class="visual-brand">
        <span class="brand-mark">观</span>
        <span>
          <strong class="serif">云观</strong>
          <small>MUSEUM RESERVATION</small>
        </span>
      </router-link>
      <div class="visual-copy">
        <span class="eyebrow light">A QUIET ENCOUNTER WITH TIME</span>
        <blockquote class="serif">在器物与光影之间，<br />与一段文明安静相遇。</blockquote>
        <p>实名预约 · 分时参观 · 电子票核销</p>
      </div>
      <div class="visual-meta">
        <span>3 座博物馆</span>
        <span>14 天可约</span>
        <span>全时段余票</span>
      </div>
    </div>

    <div class="auth-panel">
      <div class="auth-box">
        <span class="eyebrow">WELCOME BACK</span>
        <h1 class="serif">登录云观</h1>
        <p class="sub">继续你的博物馆之旅，查看预约与电子票。</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @keyup.enter="submit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              prefix-icon="Lock"
            />
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" size="large" @click="submit">
            登录
          </el-button>
        </el-form>

        <div class="register-line">
          还没有账号？
          <router-link to="/register">立即注册</router-link>
        </div>

        <div class="demo-box">
          <div class="demo-title">
            <span>演示账号</span>
            <small>点击自动填入</small>
          </div>
          <div class="demo-list">
            <button v-for="account in demoAccounts" :key="account.username" @click="fillDemo(account)">
              <strong>{{ account.label }}</strong>
              <span>{{ account.username }}</span>
            </button>
          </div>
        </div>

        <router-link to="/" class="back-home">
          <el-icon><Back /></el-icon>
          返回首页
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  min-height: 100vh;
  background: var(--paper);
}

.auth-visual {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: var(--ink);
}

.auth-visual img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  animation: slowZoom 24s ease-in-out infinite alternate;
}

@keyframes slowZoom {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(1.06);
  }
}

.visual-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(11, 16, 27, 0.5), rgba(11, 16, 27, 0.25) 45%, rgba(11, 16, 27, 0.88)),
    linear-gradient(90deg, rgba(11, 16, 27, 0.25), transparent);
}

.visual-brand {
  position: absolute;
  top: 38px;
  left: 44px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 2;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid var(--gold-light);
  border-radius: 50%;
  color: var(--gold-light);
  font-family: var(--font-serif);
  font-size: 19px;
}

.visual-brand span:last-child {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.visual-brand strong {
  color: #f7f3eb;
  font-size: 22px;
  letter-spacing: 0.2em;
}

.visual-brand small {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.42);
  font-size: 8px;
  letter-spacing: 0.18em;
}

.visual-copy {
  position: absolute;
  left: 10%;
  bottom: 18%;
  z-index: 2;
  color: #fff;
}

.eyebrow.light {
  color: var(--gold-light);
}

blockquote {
  margin: 24px 0 18px;
  font-size: clamp(32px, 3.4vw, 48px);
  font-weight: 400;
  line-height: 1.45;
  letter-spacing: 0.04em;
}

.visual-copy p {
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  letter-spacing: 0.18em;
}

.visual-meta {
  position: absolute;
  left: 10%;
  bottom: 8%;
  z-index: 2;
  display: flex;
  gap: 28px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 11px;
  letter-spacing: 0.08em;
}

.auth-panel {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 48px;
}

.auth-box {
  width: min(420px, 100%);
  animation: floatUp 0.6s ease both;
}

h1 {
  margin-top: 14px;
  color: var(--ink);
  font-size: clamp(32px, 3vw, 42px);
  font-weight: 600;
  letter-spacing: 0.04em;
}

.sub {
  margin: 12px 0 32px;
  color: var(--muted);
  font-size: 13px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 50px;
  border-radius: 10px;
  letter-spacing: 0.18em;
}

.register-line {
  margin-top: 20px;
  color: var(--muted);
  font-size: 12.5px;
  text-align: center;
}

.register-line a {
  color: var(--gold-deep);
  font-weight: 500;
}

.demo-box {
  margin-top: 28px;
  padding: 16px;
  border: 1px dashed var(--line);
  border-radius: 12px;
  background: rgba(255, 253, 249, 0.6);
}

.demo-title {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: var(--text-soft);
  font-size: 11px;
}

.demo-title small {
  color: var(--muted);
  font-size: 10px;
}

.demo-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.demo-list button {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 9px 8px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fffdf9;
  cursor: pointer;
  transition: all 0.2s ease;
}

.demo-list button:hover {
  border-color: var(--gold-light);
  background: var(--paper-light);
}

.demo-list strong {
  color: var(--text);
  font-size: 11px;
}

.demo-list span {
  color: var(--muted);
  font-size: 9px;
}

.back-home {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 26px;
  color: var(--muted);
  font-size: 11px;
}

.back-home:hover {
  color: var(--gold-deep);
}

@media (max-width: 960px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    display: none;
  }

  .auth-panel {
    padding: 32px 20px;
  }
}
</style>
