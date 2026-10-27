import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/PublicLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('@/views/public/HomeView.vue') },
        {
          path: 'museums',
          name: 'museums',
          component: () => import('@/views/public/MuseumListView.vue'),
        },
        {
          path: 'museums/:id',
          name: 'museum-detail',
          component: () => import('@/views/public/MuseumDetailView.vue'),
        },
        {
          path: 'exhibitions',
          name: 'exhibitions',
          component: () => import('@/views/public/ExhibitionListView.vue'),
        },
        {
          path: 'notices',
          name: 'notices',
          component: () => import('@/views/public/NoticeListView.vue'),
        },
        {
          path: 'notices/:id',
          name: 'notice-detail',
          component: () => import('@/views/public/NoticeDetailView.vue'),
        },
        {
          path: 'booking',
          name: 'booking',
          component: () => import('@/views/visitor/BookingView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'orders',
          name: 'orders',
          component: () => import('@/views/visitor/MyReservationsView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'orders/:id',
          name: 'order-detail',
          component: () => import('@/views/visitor/ReservationDetailView.vue'),
          meta: { requiresAuth: true },
        },
        {
          path: 'profile',
          name: 'profile',
          component: () => import('@/views/visitor/ProfileView.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, roles: ['RESERVATION_ADMIN', 'SYSTEM_ADMIN'] },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/views/admin/DashboardView.vue'),
          meta: { title: '数据看板' },
        },
        {
          path: 'museums',
          name: 'admin-museums',
          component: () => import('@/views/admin/MuseumManageView.vue'),
          meta: { title: '博物馆管理' },
        },
        {
          path: 'exhibitions',
          name: 'admin-exhibitions',
          component: () => import('@/views/admin/ExhibitionManageView.vue'),
          meta: { title: '展览管理' },
        },
        {
          path: 'slots',
          name: 'admin-slots',
          component: () => import('@/views/admin/SlotManageView.vue'),
          meta: { title: '时段与票额' },
        },
        {
          path: 'tickets',
          name: 'admin-tickets',
          component: () => import('@/views/admin/TicketTypeManageView.vue'),
          meta: { title: '票种管理' },
        },
        {
          path: 'reservations',
          name: 'admin-reservations',
          component: () => import('@/views/admin/ReservationManageView.vue'),
          meta: { title: '预约订单' },
        },
        {
          path: 'checkin',
          name: 'admin-checkin',
          component: () => import('@/views/admin/CheckinView.vue'),
          meta: { title: '票码核销' },
        },
        {
          path: 'notices',
          name: 'admin-notices',
          component: () => import('@/views/admin/NoticeManageView.vue'),
          meta: { title: '公告管理' },
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/views/admin/UserManageView.vue'),
          meta: { title: '用户与角色', roles: ['SYSTEM_ADMIN'] },
        },
        {
          path: 'logs',
          name: 'admin-logs',
          component: () => import('@/views/admin/LogView.vue'),
          meta: { title: '操作日志', roles: ['SYSTEM_ADMIN'] },
        },
        {
          path: 'profile',
          name: 'admin-profile',
          component: () => import('@/views/admin/AdminProfileView.vue'),
          meta: { title: '个人资料' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 云观预约` : '云观 · 博物馆参观预约'
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (auth.token && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout(false)
      if (to.meta.requiresAuth) {
        return { path: '/login', query: { redirect: to.fullPath } }
      }
    }
  }
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const allowed = requiredRoles.some((role) => auth.roles.includes(role))
    if (!allowed) {
      ElMessage.error('没有权限访问该页面')
      return '/'
    }
  }
  if (
    auth.isAdmin &&
    !to.path.startsWith('/admin') &&
    to.path !== '/login' &&
    to.path !== '/register'
  ) {
    return '/admin/dashboard'
  }
  return true
})

export default router
