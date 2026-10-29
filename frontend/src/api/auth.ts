 import { get, post, put } from './http'
import type { LoginResult, User } from '@/types'

export const loginApi = (data: { username: string; password: string }) =>
  post<LoginResult>('/auth/login', data)

export const registerApi = (data: {
  username: string
  password: string
  realName: string
  phone?: string
  idCard?: string
  email?: string
}) => post<User>('/auth/register', data)

export const meApi = () => get<User>('/auth/me')

export const updateProfileApi = (data: {
  realName: string
  phone?: string
  idCard?: string
  email?: string
  avatar?: string
}) => put<User>('/auth/profile', data)

export const logoutApi = () => post<void>('/auth/logout')
