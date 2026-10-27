<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import dayjs from 'dayjs'
import PageHeader from '@/components/PageHeader.vue'
import {
  adminUsers,
  assignUserRoles,
  createUser,
  getRoles,
  resetUserPassword,
  updateUserStatus,
} from '@/api/admin'
import type { SysRole, User } from '@/types'

const rows = ref<User[]>([])
const roles = ref<SysRole[]>([])
const total = ref(0)
const page = ref(1)
const size = 10
const keyword = ref('')
const status = ref<number | undefined>(undefined)
const roleCode = ref('')
const loading = ref(false)
const createVisible = ref(false)
const roleVisible = ref(false)
const saving = ref(false)
const currentUser = ref<User | null>(null)
const createFormRef = ref<FormInstance>()

const createForm = reactive({
  username: '',
  password: 'Museum@123',
  realName: '',
  phone: '',
  idCard: '',
  email: '',
  roles: ['VISITOR'] as string[],
})

const roleSelection = ref<string[]>([])

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '长度为 3-30 位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度为 6-32 位', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roles: [{ required: true, type: 'array', min: 1, message: '请选择角色', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const data = await adminUsers({
      page: page.value,
      size,
      keyword: keyword.value || undefined,
      status: status.value,
      roleCode: roleCode.value || undefined,
    })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function openCreate() {
  Object.assign(createForm, {
    username: '',
    password: 'Museum@123',
    realName: '',
    phone: '',
    idCard: '',
    email: '',
    roles: ['VISITOR'],
  })
  createVisible.value = true
}

async function saveCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createUser({ ...createForm })
    ElMessage.success('用户已创建')
    createVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function openRoles(row: User) {
  currentUser.value = row
  roleSelection.value = [...row.roles]
  roleVisible.value = true
}

async function saveRoles() {
  if (!currentUser.value) return
  if (!roleSelection.value.length) {
    ElMessage.warning('请至少选择一个角色')
    return
  }
  saving.value = true
  try {
    await assignUserRoles(currentUser.value.id, roleSelection.value)
    ElMessage.success('角色已更新')
    roleVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: User) {
  const next = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      next === 1 ? `确认启用账号「${row.username}」？` : `确认禁用账号「${row.username}」？禁用后无法登录。`,
      '修改账号状态',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
    await updateUserStatus(row.id, next)
    ElMessage.success(next === 1 ? '账号已启用' : '账号已禁用')
    load()
  } catch {
    // 取消
  }
}

async function resetPassword(row: User) {
  try {
    const { value } = await ElMessageBox.prompt(
      `为账号「${row.username}」设置新的登录密码（6-32 位）`,
      '重置密码',
      {
        confirmButtonText: '确认重置',
        cancelButtonText: '取消',
        inputPattern: /^.{6,32}$/,
        inputErrorMessage: '密码长度需为 6-32 位',
        inputValue: 'Museum@123',
      },
    )
    await resetUserPassword(row.id, value)
    ElMessage.success('密码已重置')
  } catch {
    // 取消
  }
}

function roleLabel(code: string) {
  return roles.value.find((role) => role.roleCode === code)?.roleName || code
}

function changePage(value: number) {
  page.value = value
  load()
}

onMounted(async () => {
  roles.value = await getRoles()
  load()
})
</script>

<template>
  <div class="admin-page">
    <PageHeader
      eyebrow="USERS & ROLES"
      title="用户与角色"
      desc="管理系统用户账号与角色。系统管理员拥有全部权限，预约管理员负责运营业务，游客仅可预约。"
    >
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
    </PageHeader>

    <div class="admin-toolbar">
      <div class="toolbar-filters">
        <el-input v-model="keyword" placeholder="用户名/姓名/手机号" clearable @keyup.enter="search" @clear="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="roleCode" placeholder="全部角色" clearable @change="search">
          <el-option v-for="role in roles" :key="role.roleCode" :label="role.roleName" :value="role.roleCode" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable @change="search">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button @click="search">查询</el-button>
      </div>
      <span class="total-note">共 {{ total }} 名用户</span>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="avatar">{{ (row.realName || row.username).slice(0, 1) }}</span>
              <div>
                <strong>{{ row.realName }}</strong>
                <small>@{{ row.username }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" min-width="170">
          <template #default="{ row }">
            <el-tag v-for="code in row.roles" :key="code" size="small" effect="plain" class="role-tag">
              {{ roleLabel(code) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="160">
          <template #default="{ row }">
            {{ row.lastLoginTime ? dayjs(row.lastLoginTime).format('YYYY-MM-DD HH:mm') : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain" round>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openRoles(row)">分配角色</el-button>
            <el-button text size="small" @click="resetPassword(row)">重置密码</el-button>
            <el-button text size="small" :type="row.status === 1 ? 'danger' : 'primary'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          layout="total, prev, pager, next"
          :total="total"
          :page-size="size"
          :current-page="page"
          background
          @current-change="changePage"
        />
      </div>
    </div>

    <el-dialog v-model="createVisible" title="新增用户" width="620px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="rules" label-position="top">
        <div class="form-cols">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="createForm.username" placeholder="字母、数字或下划线" />
          </el-form-item>
          <el-form-item label="初始密码" prop="password">
            <el-input v-model="createForm.password" placeholder="至少 6 位" />
          </el-form-item>
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="createForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="createForm.phone" placeholder="选填" maxlength="11" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="createForm.email" placeholder="选填" />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="createForm.idCard" placeholder="选填" maxlength="18" />
          </el-form-item>
        </div>
        <el-form-item label="角色" prop="roles">
          <el-checkbox-group v-model="createForm.roles">
            <el-checkbox v-for="role in roles" :key="role.roleCode" :value="role.roleCode">
              {{ role.roleName }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCreate">创建用户</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="分配角色" width="480px">
      <div v-if="currentUser" class="role-dialog">
        <p>为用户 <strong>{{ currentUser.realName }}（@{{ currentUser.username }}）</strong> 分配角色：</p>
        <el-checkbox-group v-model="roleSelection" class="role-options">
          <label v-for="role in roles" :key="role.roleCode" class="role-option">
            <el-checkbox :value="role.roleCode">
              <span>
                <strong>{{ role.roleName }}</strong>
                <small>{{ role.description }}</small>
              </span>
            </el-checkbox>
          </label>
        </el-checkbox-group>
        <el-alert type="warning" :closable="false" show-icon title="不能移除自己的系统管理员角色，否则将无法进入系统管理。" />
      </div>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoles">保存角色</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.total-note {
  color: var(--muted);
  font-size: 11px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--ink);
  color: var(--gold-light);
  font-family: var(--font-serif);
}

.user-cell strong {
  display: block;
  color: var(--ink);
  font-size: 12.5px;
  font-weight: 500;
}

.user-cell small {
  display: block;
  margin-top: 3px;
  color: var(--muted);
  font-size: 9.5px;
}

.role-tag {
  margin: 2px 4px 2px 0;
}

.form-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.role-dialog > p {
  margin-bottom: 14px;
  color: var(--text-soft);
  font-size: 12px;
}

.role-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.role-option {
  display: block;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 9px;
  background: #fff;
}

.role-option strong {
  display: block;
  color: var(--text);
  font-size: 12px;
  font-weight: 500;
}

.role-option small {
  display: block;
  margin-top: 3px;
  color: var(--muted);
  font-size: 10px;
}

@media (max-width: 600px) {
  .form-cols {
    grid-template-columns: 1fr;
  }
}
</style>
