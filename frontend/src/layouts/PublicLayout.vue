<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menuOpen = ref(false)

const navs = [
  { label: '首页', path: '/' },
  { label: '博物馆', path: '/museums' },
  { label: '展览', path: '/exhibitions' },
  { label: '参观公告', path: '/notices' },
]

const activePath = computed(() => {
  if (route.path === '/') return '/'
  return navs.find((item) => item.path !== '/' && route.path.startsWith(item.path))?.path || ''
})

function go(path: string) {
  menuOpen.value = false
  router.push(path)
}

function handleCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    ElMessage.success('已退出登录')
    router.push('/')
    return
  }
  router.push(command)
}

const year = new Date().getFullYear()
</script>

<template>
  <div class="public-layout">
    <header class="site-header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark">
            <span class="seal">观</span>
          </span>
          <span class="brand-text">
            <strong class="serif">云观</strong>
            <small>MUSEUM RESERVATION</small>
          </span>
        </router-link>

        <nav class="desktop-nav">
          <router-link
            v-for="item in navs"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: activePath === item.path }"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="header-actions">
          <template v-if="auth.isLoggedIn">
            <el-button class="desktop-only" text @click="go('/orders')">我的预约</el-button>
            <el-dropdown trigger="click" @command="handleCommand">
              <button class="user-chip">
                <span class="avatar">
                  <img v-if="auth.user?.avatar" :src="auth.user.avatar" :alt="auth.displayName" />
                  <template v-else>{{ auth.displayName.slice(0, 1) }}</template>
                </span>
                <span class="desktop-only">{{ auth.displayName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="/orders">我的预约</el-dropdown-item>
                  <el-dropdown-item command="/profile">个人资料</el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="/admin/dashboard" divided>
                    管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text class="desktop-only" @click="go('/login')">登录</el-button>
            <el-button type="primary" round @click="go('/register')">预约参观</el-button>
          </template>
          <button class="menu-toggle" aria-label="打开菜单" @click="menuOpen = true">
            <el-icon><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main>
      <router-view />
    </main>

    <footer class="site-footer">
      <div class="container footer-grid">
        <div class="footer-brand">
          <div class="brand brand-light">
            <span class="brand-mark"><span class="seal">观</span></span>
            <span class="brand-text">
              <strong class="serif">云观</strong>
              <small>MUSEUM RESERVATION</small>
            </span>
          </div>
          <p>让每一次进入展厅，都从容、有序、值得期待。</p>
        </div>
        <div class="footer-col">
          <h4>参观</h4>
          <router-link to="/museums">博物馆</router-link>
          <router-link to="/exhibitions">正在展出</router-link>
          <router-link to="/notices">参观公告</router-link>
        </div>
        <div class="footer-col">
          <h4>服务</h4>
          <router-link to="/orders">我的预约</router-link>
          <router-link to="/login">登录账号</router-link>
          <router-link to="/register">注册账号</router-link>
        </div>
        <div class="footer-col">
          <h4>预约提示</h4>
          <p>各馆开放时间以场馆详情为准</p>
          <p>每周闭馆日以场馆详情为准</p>
          <p class="footer-note">建议提前 1-7 天预约</p>
        </div>
      </div>
      <div class="container footer-bottom">
        <span>© {{ year }} 云观博物馆预约平台 · 课程设计演示系统</span>
        <span>实名预约 · 分时参观 · 电子票核销</span>
      </div>
    </footer>

    <el-drawer v-model="menuOpen" direction="rtl" size="78%" :with-header="false">
      <div class="mobile-menu">
        <div class="brand">
          <span class="brand-mark"><span class="seal">观</span></span>
          <span class="brand-text">
            <strong class="serif">云观</strong>
            <small>MUSEUM RESERVATION</small>
          </span>
        </div>
        <nav>
          <button
            v-for="item in navs"
            :key="item.path"
            class="mobile-nav-link"
            :class="{ active: activePath === item.path }"
            @click="go(item.path)"
          >
            {{ item.label }}
            <el-icon><ArrowRight /></el-icon>
          </button>
          <button v-if="auth.isLoggedIn" class="mobile-nav-link" @click="go('/orders')">
            我的预约
            <el-icon><ArrowRight /></el-icon>
          </button>
          <button v-if="auth.isAdmin" class="mobile-nav-link" @click="go('/admin/dashboard')">
            管理后台
            <el-icon><ArrowRight /></el-icon>
          </button>
        </nav>
        <div class="mobile-menu-actions">
          <el-button v-if="auth.isLoggedIn" plain @click="handleCommand('logout')">退出登录</el-button>
          <template v-else>
            <el-button plain @click="go('/login')">登录</el-button>
            <el-button type="primary" @click="go('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.public-layout {
  min-height: 100vh;
  background: var(--paper);
}

.site-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: rgba(247, 243, 235, 0.9);
  border-bottom: 1px solid rgba(229, 221, 207, 0.7);
  backdrop-filter: blur(18px);
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 76px;
  gap: 28px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid var(--gold);
  border-radius: 50%;
  background: var(--ink);
  color: var(--paper);
  position: relative;
}

.brand-mark::after {
  content: '';
  position: absolute;
  inset: 4px;
  border: 1px solid rgba(213, 187, 139, 0.55);
  border-radius: 50%;
}

.seal {
  font-family: var(--font-serif);
  font-size: 19px;
  line-height: 1;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-text strong {
  font-size: 22px;
  letter-spacing: 0.18em;
  color: var(--ink);
}

.brand-text small {
  margin-top: 3px;
  font-size: 8px;
  letter-spacing: 0.16em;
  color: var(--muted);
}

.desktop-nav {
  display: flex;
  align-items: center;
  gap: 34px;
  margin-left: auto;
  margin-right: 12px;
}

.nav-link {
  position: relative;
  padding: 8px 0;
  font-size: 14px;
  color: var(--text-soft);
  letter-spacing: 0.04em;
  transition: color 0.2s ease;
}

.nav-link::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 0;
  height: 1px;
  background: var(--gold);
  transition: all 0.3s ease;
  transform: translateX(-50%);
}

.nav-link:hover,
.nav-link.active {
  color: var(--ink);
}

.nav-link.active::after,
.nav-link:hover::after {
  width: 100%;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px 5px 5px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fffdf9;
  color: var(--text-soft);
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.user-chip:hover {
  border-color: var(--gold-light);
}

.avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  overflow: hidden;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--ink), var(--ink-light));
  color: var(--gold-light);
  font-family: var(--font-serif);
  font-size: 14px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.menu-toggle {
  display: none;
  width: 40px;
  height: 40px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: transparent;
  color: var(--ink);
  cursor: pointer;
}

.site-footer {
  padding: 64px 0 24px;
  background: var(--ink);
  color: rgba(255, 255, 255, 0.58);
}

.footer-grid {
  display: grid;
  grid-template-columns: 1.6fr 0.7fr 0.7fr 1fr;
  gap: 48px;
  padding-bottom: 48px;
  border-bottom: 1px solid var(--line-dark);
}

.footer-brand p {
  max-width: 300px;
  margin-top: 18px;
  line-height: 1.9;
  font-size: 13px;
}

.brand-light .brand-mark {
  background: transparent;
}

.brand-light .brand-text strong {
  color: var(--paper);
}

.brand-light .brand-text small {
  color: rgba(255, 255, 255, 0.45);
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  font-size: 13px;
}

.footer-col h4 {
  margin-bottom: 8px;
  color: var(--gold-light);
  font-family: var(--font-serif);
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.08em;
}

.footer-col a {
  transition: color 0.2s ease;
}

.footer-col a:hover {
  color: var(--paper);
}

.footer-note {
  color: var(--gold-light);
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-top: 24px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.38);
}

.mobile-menu {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px 4px;
}

.mobile-menu nav {
  display: flex;
  flex-direction: column;
  margin-top: 36px;
}

.mobile-nav-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 4px;
  border: none;
  border-bottom: 1px solid var(--line);
  background: transparent;
  color: var(--text);
  font-family: var(--font-serif);
  font-size: 22px;
  text-align: left;
  cursor: pointer;
}

.mobile-nav-link.active {
  color: var(--gold-deep);
}

.mobile-menu-actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 24px;
}

.mobile-menu-actions .el-button {
  flex: 1;
}

@media (max-width: 960px) {
  .desktop-nav,
  .desktop-only {
    display: none !important;
  }

  .menu-toggle {
    display: grid;
    place-items: center;
  }

  .footer-grid {
    grid-template-columns: 1fr 1fr;
    gap: 32px;
  }
}

@media (max-width: 600px) {
  .header-inner {
    height: 66px;
  }

  .brand-text strong {
    font-size: 20px;
  }

  .footer-grid {
    grid-template-columns: 1fr;
  }

  .footer-bottom {
    flex-direction: column;
  }
}
</style>
