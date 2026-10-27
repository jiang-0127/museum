import { del, get, post, put } from './http'
import type {
  CheckinRecord,
  CheckinResult,
  Exhibition,
  Museum,
  NameValue,
  Notice,
  OperationLog,
  Overview,
  PageResult,
  Reservation,
  ReservationDetail,
  Slot,
  SlotSaturation,
  SysRole,
  SystemConfig,
  TicketType,
  TrendPoint,
  User,
} from '@/types'

export const adminMuseums = (params?: Record<string, unknown>) =>
  get<PageResult<Museum>>('/admin/museums', params)
export const createMuseum = (data: Partial<Museum>) => post<Museum>('/admin/museums', data)
export const updateMuseum = (id: number, data: Partial<Museum>) =>
  put<Museum>(`/admin/museums/${id}`, data)
export const deleteMuseum = (id: number) => del<void>(`/admin/museums/${id}`)

export const adminExhibitions = (params?: Record<string, unknown>) =>
  get<PageResult<Exhibition>>('/admin/exhibitions', params)
export const createExhibition = (data: Partial<Exhibition>) =>
  post<Exhibition>('/admin/exhibitions', data)
export const updateExhibition = (id: number, data: Partial<Exhibition>) =>
  put<Exhibition>(`/admin/exhibitions/${id}`, data)
export const deleteExhibition = (id: number) => del<void>(`/admin/exhibitions/${id}`)

export const adminSlots = (params?: Record<string, unknown>) =>
  get<PageResult<Slot>>('/admin/slots', params)
export const createSlot = (data: Record<string, unknown>) => post<Slot>('/admin/slots', data)
export const updateSlot = (id: number, data: Record<string, unknown>) =>
  put<Slot>(`/admin/slots/${id}`, data)
export const deleteSlot = (id: number) => del<void>(`/admin/slots/${id}`)

export const adminTicketTypes = (params?: Record<string, unknown>) =>
  get<PageResult<TicketType>>('/admin/ticket-types', params)
export const createTicketType = (data: Partial<TicketType>) =>
  post<TicketType>('/admin/ticket-types', data)
export const updateTicketType = (id: number, data: Partial<TicketType>) =>
  put<TicketType>(`/admin/ticket-types/${id}`, data)
export const deleteTicketType = (id: number) => del<void>(`/admin/ticket-types/${id}`)

export const adminNotices = (params?: Record<string, unknown>) =>
  get<PageResult<Notice>>('/admin/notices', params)
export const createNotice = (data: Partial<Notice>) => post<Notice>('/admin/notices', data)
export const updateNotice = (id: number, data: Partial<Notice>) =>
  put<Notice>(`/admin/notices/${id}`, data)
export const deleteNotice = (id: number) => del<void>(`/admin/notices/${id}`)

export const adminReservations = (params?: Record<string, unknown>) =>
  get<PageResult<Reservation>>('/admin/reservations', params)
export const adminReservationDetail = (id: number | string) =>
  get<ReservationDetail>(`/admin/reservations/${id}`)
export const adminCancelReservation = (id: number) =>
  put<void>(`/admin/reservations/${id}/cancel`)

export const checkinVerify = (data: { ticketCode: string; gate?: string }) =>
  post<CheckinResult>('/checkin/verify', data)
export const checkinRecords = (params?: Record<string, unknown>) =>
  get<PageResult<CheckinRecord>>('/checkin/records', params)

export const statisticsOverview = () => get<Overview>('/admin/statistics/overview')
export const statisticsTrend = (days = 7) =>
  get<TrendPoint[]>('/admin/statistics/trend', { days })
export const statisticsMuseumHeat = (days = 30) =>
  get<NameValue[]>('/admin/statistics/museum-heat', { days })
export const statisticsTicketDistribution = () =>
  get<NameValue[]>('/admin/statistics/ticket-distribution')
export const statisticsSlotSaturation = () =>
  get<SlotSaturation[]>('/admin/statistics/slot-saturation')

export const adminUsers = (params?: Record<string, unknown>) =>
  get<PageResult<User>>('/admin/users', params)
export const createUser = (data: Record<string, unknown>) => post<User>('/admin/users', data)
export const updateUserStatus = (id: number, status: number) =>
  put<void>(`/admin/users/${id}/status`, { status })
export const assignUserRoles = (id: number, roles: string[]) =>
  put<void>(`/admin/users/${id}/roles`, { roles })
export const resetUserPassword = (id: number, password: string) =>
  put<void>(`/admin/users/${id}/password`, { password })
export const getRoles = () => get<SysRole[]>('/admin/roles')

export const adminLogs = (params?: Record<string, unknown>) =>
  get<PageResult<OperationLog>>('/admin/logs', params)

export const adminConfigs = () => get<SystemConfig[]>('/admin/configs')
export const updateConfig = (key: string, configValue: string) =>
  put<void>(`/admin/configs/${key}`, { configValue })
