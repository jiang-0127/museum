import { get } from './http'
import type { BasicQuota, Exhibition, Museum, Notice, PageResult, SiteSummary, Slot, TicketType } from '@/types'

export const getSiteSummary = () => get<SiteSummary>('/site/summary')

export const getMuseums = (params?: Record<string, unknown>) =>
  get<PageResult<Museum>>('/museums', params)

export const getMuseum = (id: number | string) => get<Museum>(`/museums/${id}`)

export const getMuseumExhibitions = (id: number | string) =>
  get<Exhibition[]>(`/museums/${id}/exhibitions`)

export const getMuseumTicketTypes = (id: number | string) =>
  get<TicketType[]>(`/museums/${id}/ticket-types`)

export const getBasicQuota = (id: number | string, date: string) =>
  get<BasicQuota>(`/museums/${id}/basic-quota`, { date })

export const getExhibitions = (params?: Record<string, unknown>) =>
  get<PageResult<Exhibition>>('/exhibitions', params)

export const getExhibition = (id: number | string) => get<Exhibition>(`/exhibitions/${id}`)

export const getSlots = (params?: Record<string, unknown>) => get<Slot[]>('/slots', params)

export const getSlot = (id: number | string) => get<Slot>(`/slots/${id}`)

export const getNotices = (params?: Record<string, unknown>) =>
  get<PageResult<Notice>>('/notices', params)

export const getLatestNotices = (limit = 3) =>
  get<Notice[]>('/notices/latest', { limit })

export const getNotice = (id: number | string) => get<Notice>(`/notices/${id}`)
