export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export type ReservationStatus =
  | 'PENDING_PAYMENT'
  | 'RESERVED'
  | 'CANCELED'
  | 'CHECKED_IN'
  | 'EXPIRED'

export type PaymentStatus = 'UNPAID' | 'PAID' | 'REFUNDED' | 'CLOSED'

export interface User {
  id: number
  username: string
  realName: string
  phone?: string
  idCard?: string
  email?: string
  avatar?: string
  status: number
  roles: string[]
  roleNames: string[]
  lastLoginTime?: string
  createdAt?: string
}

export interface LoginResult {
  token: string
  user: User
}

export interface Museum {
  id: number
  name: string
  address: string
  description?: string
  openTime?: string
  closeTime?: string
  phone?: string
  coverUrl?: string
  status: number
  sort: number
  createdAt?: string
}

export interface Exhibition {
  id: number
  museumId: number
  museumName?: string
  name: string
  description?: string
  coverUrl?: string
  startDate: string
  endDate: string
  status: number
  createdAt?: string
}

export interface TicketType {
  id: number
  museumId: number
  name: string
  price: number
  ticketKind?: 'BASIC' | 'SINGLE' | 'COMBO'
  exhibitionId?: number | null
  audienceType?: 'GENERAL' | 'ADULT' | 'STUDENT' | 'FAMILY'
  tagline?: string
  audience?: string
  description?: string
  benefits?: string
  rules?: string
  status: number
  sort: number
}

export interface Slot {
  id: number
  museumId: number
  museumName: string
  exhibitionId?: number
  exhibitionName?: string
  visitDate: string
  startTime: string
  endTime: string
  totalCapacity: number
  bookedCount: number
  remaining: number
  status: number
  createdAt?: string
}

export interface VisitorInfo {
  id?: number
  name: string
  idCard: string
  phone?: string
}

export interface Reservation {
  id: number
  orderNo: string
  userId: number
  museumId: number
  museumName: string
  exhibitionId?: number
  exhibitionName?: string
  slotId: number
  visitDate: string
  startTime: string
  endTime: string
  ticketTypeId: number
  ticketTypeName: string
  visitorCount: number
  unitPrice: number
  totalAmount: number
  contactName: string
  contactPhone: string
  status: ReservationStatus
  paymentNo?: string
  paymentStatus?: PaymentStatus
  qrExpireAt?: string
  cancelTime?: string
  checkinTime?: string
  remark?: string
  createdAt: string
}

export interface ReservationDetail extends Reservation {
  visitors: VisitorInfo[]
  paymentPayType?: string
  paymentPaidTime?: string
  paymentRefundTime?: string
  qrToken?: string
}

export interface Ticket {
  reservationId: number
  orderNo: string
  status: ReservationStatus
  museumName: string
  exhibitionName?: string
  visitDate: string
  startTime: string
  endTime: string
  visitorCount: number
  contactName: string
  ticketCode: string
  expireAt: string
  qrImage: string
}

export interface Notice {
  id: number
  title: string
  summary?: string
  content?: string
  coverUrl?: string
  status: number
  publishTime?: string
  createdAt?: string
}

export interface Overview {
  todayReservations: number
  todayVisitors: number
  todayCheckins: number
  todayCanceled: number
  pendingPayments: number
  totalMuseums: number
  totalUsers: number
  totalReservations: number
}

export interface TrendPoint {
  date: string
  reservations: number
  checkins: number
  visitors: number
  canceled: number
}

export interface NameValue {
  name: string
  value: number
}

export interface SlotSaturation {
  id: number
  museumName: string
  visitDate: string
  startTime: string
  endTime: string
  totalCapacity: number
  bookedCount: number
  saturation: number
}

export interface CheckinResult {
  success: boolean
  message: string
  orderNo?: string
  museumName?: string
  contactName?: string
  visitDate?: string
  startTime?: string
  endTime?: string
  visitorCount?: number
  status?: string
  checkinTime?: string
}

export interface CheckinRecord {
  id: number
  reservationId: number
  orderNo: string
  checkinTime: string
  gate: string
  operatorId?: number
  operatorName?: string
  status: string
}

export interface SysRole {
  id: number
  roleCode: string
  roleName: string
  description?: string
}

export interface OperationLog {
  id: number
  userId?: number
  username: string
  module: string
  operation: string
  method: string
  params?: string
  ip?: string
  durationMs?: number
  success: number
  errorMsg?: string
  createTime: string
}

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  description?: string
}

export interface SiteSummary {
  museumCount: number
  advanceBookingDays: number
  maxVisitorsPerOrder: number
}

export interface ReservationCreatePayload {
  slotId: number
  ticketTypeId: number
  contactName: string
  contactPhone: string
  visitors: Array<{ name: string; idCard: string; phone?: string }>
  remark?: string
}
