import { get, post, put } from './http'
import type {
  PageResult,
  Reservation,
  ReservationCreatePayload,
  ReservationDetail,
  Ticket,
} from '@/types'

export const createReservation = (data: ReservationCreatePayload) =>
  post<ReservationDetail>('/reservations', data)

export const getMyReservations = (params?: Record<string, unknown>) =>
  get<PageResult<Reservation>>('/reservations/my', params)

export const getReservation = (id: number | string) =>
  get<ReservationDetail>(`/reservations/${id}`)

export const cancelReservation = (id: number | string) =>
  put<void>(`/reservations/${id}/cancel`)

export const getTicket = (id: number | string) => get<Ticket>(`/reservations/${id}/ticket`)

export const mockPay = (orderNo: string) =>
  post<ReservationDetail>(`/payments/${orderNo}/mock-pay`)
