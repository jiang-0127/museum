<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)

const businessMenus = [
  { path: '/admin/dashboard', label: '数据看板', icon: 'DataAnalysis' },
  { path: '/admin/museums', label: '博物馆管理', icon: 'OfficeBuilding' },
  { path: '/admin/exhibitions', label: '展览管理', icon: 'Picture' },
  { path: '/admin/slots', label: '入馆批次与票额', icon: 'Calendar' },
  { path: '/admin/tickets', label: '票种管理', icon: 'Ticket' },
  { path: '/admin/reservations', label: '预约订单', icon: 'Tickets' },
  { path: '/admin/checkin', label: '票码核销', icon: 'Tickets' },
  { path: '/admin/notices', label: '公告管理', icon: 'Bell' },
]

const systemMenus = [
  { path: '/admin/users', label: '用户与角色', icon: 'User' },
  { path: '/admin/logs', label: '操作日志', icon: 'Document' },
  { path: '/admin/configs', label: '参数配置', icon: 'Setting' },
]

const pageTitle = computed(() => (route.meta.title as string) || '管理后台')

function go(path: string) {
  mobileOpen.value = false
  router.push(path)
}

function handleCommand(command: string) {
  if (command === 'logout') {
    auth.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
    return
  }
  router.push(command)
}
</script>

<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ collapsed }">
      <router-link to="/admin/dashboard" class="admin-brand">
        <span class="brand-mark"><span class="seal">观</span></span>
        <span v-show="!collapsed" class="brand-copy">
          <strong class="serif">云观</strong>
          <small>运营管理台</small>
        </span>
      </router-link>

      <div class="sidebar-body">
        <div v-show="!collapsed" class="menu-group-label">运营管理</div>
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="admin-menu"
        >
          <el-menu-item v-for="item in businessMenus" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.label }}</template>
          </el-menu-item>
        </el-menu>

        <template v-if="auth.isSystemAdmin">
          <div v-show="!collapsed" class="menu-group-label system-label">系统管理</div>
          <el-menu
            :default-active="route.path"
            :collapse="collapsed"
            :collapse-transition="false"
            router
            class="admin-menu"
          >
            <el-menu-item v-for="item in systemMenus" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.label }}</template>
            </el-menu-item>
          </el-menu>
        </template>
      </div>

      <div v-if="!auth.isAdmin" class="sidebar-footer">
        <button class="back-site" @click="go('/')">
          <el-icon><Back /></el-icon>
          <span v-show="!collapsed">返回参观门户</span>
        </button>
      </div>
    </aside>

    <div class="admin-main" :class="{ expanded: collapsed }">
      <header class="admin-header">
        <div class="header-left">
          <button class="icon-button desktop-collapse" @click="collapsed = !collapsed">
            <el-icon><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </button>
          <button class="icon-button mobile-trigger" @click="mobileOpen = true">
            <el-icon><Menu /></el-icon>
          </button>
          <div class="crumb">
            <span>管理后台</span>
            <el-icon><ArrowRight /></el-icon>
            <strong>{{ pageTitle }}</strong>
          </div>
        </div>
        <div class="header-right">
          <span class="role-pill">
            {{ auth.user?.roleNames?.[0] || '管理员' }}
          </span>
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="admin-user">
              <span class="avatar">
                <img v-if="auth.user?.avatar" :src="auth.user.avatar" :alt="auth.displayName" />
                <template v-else>{{ auth.displayName.slice(0, 1) }}</template>
              </span>
              <span class="user-copy">
                <strong>{{ auth.displayName }}</strong>
                <small>{{ auth.user?.username }}</small>
              </span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="/admin/profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="admin-content">
        <router-view :key="route.path" />
      </main>
    </div>

    <el-drawer v-model="mobileOpen" direction="ltr" size="82%" :with-header="false" class="mobile-admin-drawer">
      <div class="mobile-sidebar">
        <div class="admin-brand mobile-brand">
          <span class="brand-mark"><span class="seal">观</span></span>
          <span class="brand-copy">
            <strong class="serif">云观</strong>
            <small>运营管理台</small>
          </span>
        </div>
        <div class="menu-group-label">运营管理</div>
        <button
          v-for="item in businessMenus"
          :key="item.path"
          class="mobile-menu-item"
          :class="{ active: route.path === item.path }"
          @click="go(item.path)"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          {{ item.label }}
        </button>
        <template v-if="auth.isSystemAdmin">
          <div class="menu-group-label system-label">系统管理</div>
          <button
            v-for="item in systemMenus"
            :key="item.path"
            class="mobile-menu-item"
            :class="{ active: route.path === item.path }"
            @click="go(item.path)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            {{ item.label }}
          </button>
        </template>
        <button class="mobile-menu-item" @click="go('/admin/profile')">
          <el-icon><User /></el-icon>
          个人资料
        </button>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f4f1ea;
}

.sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: 40;
  display: flex;
  flex-direction: column;
  width: 240px;
  background:
    radial-gradient(circle at 20% 0%, rgba(176, 138, 79, 0.16), transparent 40%),
    linear-gradient(180deg, #172033, #101725);
  color: rgba(255, 255, 255, 0.7);
  transition: width 0.28s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 76px;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 84px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border: 1px solid var(--gold);
  border-radius: 50%;
  color: var(--gold-light);
  position: relative;
}

.brand-mark::after {
  content: '';
  position: absolute;
  inset: 4px;
  border: 1px solid rgba(213, 187, 139, 0.4);
  border-radius: 50%;
}

.seal {
  font-family: var(--font-serif);
  font-size: 17px;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-copy strong {
  color: #f7f3eb;
  font-size: 21px;
  letter-spacing: 0.18em;
}

.brand-copy small {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.38);
  font-size: 10px;
  letter-spacing: 0.12em;
}

.sidebar-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 10px;
}

.menu-group-label {
  padding: 10px 14px 8px;
  color: rgba(255, 255, 255, 0.32);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.system-label {
  margin-top: 14px;
}

.admin-menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: rgba(255, 255, 255, 0.62);
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.06);
  --el-menu-active-color: #e8d3a9;
  --el-menu-item-height: 46px;
}

.admin-menu :deep(.el-menu-item) {
  margin: 4px 0;
  border-radius: 8px;
  font-size: 13.5px;
  letter-spacing: 0.03em;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(176, 138, 79, 0.25), rgba(176, 138, 79, 0.06));
  color: #f0ddb6;
}

.admin-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  width: 3px;
  height: 16px;
  border-radius: 2px;
  background: var(--gold);
  margin-right: 10px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.back-site {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.55);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-site:hover {
  color: var(--gold-light);
  border-color: rgba(213, 187, 139, 0.4);
}

.admin-main {
  flex: 1;
  min-width: 0;
  margin-left: 240px;
  transition: margin-left 0.28s ease;
}

.admin-main.expanded {
  margin-left: 76px;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 32px;
  background: rgba(255, 253, 249, 0.92);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(16px);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-button {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: #fffdf9;
  color: var(--text-soft);
  cursor: pointer;
}

.mobile-trigger {
  display: none;
}

.crumb {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted);
  font-size: 13px;
}

.crumb strong {
  color: var(--ink);
  font-weight: 600;
}

.crumb .el-icon {
  font-size: 11px;
}

.role-pill {
  padding: 6px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  color: var(--gold-deep);
  background: var(--paper-light);
  font-size: 11px;
  letter-spacing: 0.06em;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 4px 4px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  cursor: pointer;
}

.admin-user:hover {
  border-color: var(--line);
  background: #fffdf9;
}

.avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  overflow: hidden;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--ink), var(--ink-light));
  color: var(--gold-light);
  font-family: var(--font-serif);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.user-copy strong {
  color: var(--text);
  font-size: 13px;
}

.user-copy small {
  color: var(--muted);
  font-size: 10px;
}

.admin-content {
  padding: 30px 32px 48px;
}

.mobile-sidebar {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
}

.mobile-brand {
  padding: 8px 6px 20px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 12px;
}

.mobile-brand .brand-copy strong {
  color: var(--ink);
}

.mobile-menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-soft);
  text-align: left;
  font-size: 14px;
  cursor: pointer;
}

.mobile-menu-item.active {
  background: var(--paper-deep);
  color: var(--gold-deep);
}

@media (max-width: 960px) {
  .sidebar {
    display: none;
  }

  .admin-main,
  .admin-main.expanded {
    margin-left: 0;
  }

  .mobile-trigger,
  .desktop-collapse {
    display: none;
  }

  .mobile-trigger {
    display: grid;
  }

  .admin-header {
    padding: 0 18px;
  }

  .admin-content {
    padding: 20px 16px 40px;
  }

  .role-pill,
  .user-copy {
    display: none;
  }
}
</style>
