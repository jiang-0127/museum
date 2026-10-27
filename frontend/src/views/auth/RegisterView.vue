<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { registerApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  idCard: '',
  email: '',
})

const validateConfirm = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '长度为 3-30 位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度为 6-32 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
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

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await registerApi({
      username: form.username,
      password: form.password,
      realName: form.realName,
      phone: form.phone || undefined,
      idCard: form.idCard || undefined,
      email: form.email || undefined,
    })
    await auth.login(form.username, form.password)
    ElMessage.success('注册成功，欢迎加入云观')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-visual">
      <img
        src="/images/seed/exhibition-5.jpg"
        alt="博物馆艺术"
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
        <span class="eyebrow light">BEGIN YOUR VISIT</span>
        <blockquote class="serif">预约一段时光，<br />让观看成为一次郑重其事的抵达。</blockquote>
        <p>一次注册，通行三馆 · 实名信息仅用于入馆核验</p>
      </div>
    </div>

    <div class="register-panel">
      <div class="register-box">
        <span class="eyebrow">CREATE ACCOUNT</span>
        <h1 class="serif">注册参观账号</h1>
        <p class="sub">填写实名信息，预约时可直接带入，减少重复输入。</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          class="register-form"
        >
          <div class="form-grid">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="字母、数字或下划线" prefix-icon="User" />
            </el-form-item>
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="用于入馆核验" prefix-icon="Postcard" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
              />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="选填" prefix-icon="Phone" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="选填" prefix-icon="Message" />
            </el-form-item>
          </div>
          <el-form-item label="身份证号" prop="idCard">
            <el-input v-model="form.idCard" placeholder="选填，填写后预约可自动带入" prefix-icon="CreditCard" />
          </el-form-item>

          <el-button type="primary" class="submit-btn" :loading="loading" size="large" @click="submit">
            注册并登录
          </el-button>
        </el-form>

        <div class="login-line">
          已有账号？
          <router-link to="/login">直接登录</router-link>
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
.register-page {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  min-height: 100vh;
  background: var(--paper);
}

.register-visual {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: var(--ink);
}

.register-visual img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  animation: slowZoom 26s ease-in-out infinite alternate;
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
    linear-gradient(180deg, rgba(11, 16, 27, 0.45), rgba(11, 16, 27, 0.3) 45%, rgba(11, 16, 27, 0.9)),
    linear-gradient(90deg, rgba(11, 16, 27, 0.2), transparent);
}

.visual-brand {
  position: absolute;
  top: 38px;
  left: 44px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 12px;
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
  left: 9%;
  bottom: 16%;
  z-index: 2;
  color: #fff;
}

.eyebrow.light {
  color: var(--gold-light);
}

blockquote {
  margin: 24px 0 18px;
  font-size: clamp(28px, 3vw, 42px);
  font-weight: 400;
  line-height: 1.5;
  letter-spacing: 0.04em;
}

.visual-copy p {
  max-width: 360px;
  color: rgba(255, 255, 255, 0.46);
  font-size: 11px;
  letter-spacing: 0.08em;
  line-height: 1.9;
}

.register-panel {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 48px;
}

.register-box {
  width: min(560px, 100%);
  animation: floatUp 0.6s ease both;
}

h1 {
  margin-top: 14px;
  color: var(--ink);
  font-size: clamp(30px, 3vw, 40px);
  font-weight: 600;
  letter-spacing: 0.04em;
}

.sub {
  margin: 10px 0 28px;
  color: var(--muted);
  font-size: 12.5px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.submit-btn {
  width: 100%;
  height: 50px;
  margin-top: 8px;
  border-radius: 10px;
  letter-spacing: 0.18em;
}

.login-line {
  margin-top: 20px;
  color: var(--muted);
  font-size: 12.5px;
  text-align: center;
}

.login-line a {
  color: var(--gold-deep);
  font-weight: 500;
}

.back-home {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 24px;
  color: var(--muted);
  font-size: 11px;
}

@media (max-width: 960px) {
  .register-page {
    grid-template-columns: 1fr;
  }

  .register-visual {
    display: none;
  }

  .register-panel {
    padding: 32px 20px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
