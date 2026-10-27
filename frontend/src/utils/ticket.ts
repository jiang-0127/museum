import type { TicketType } from '@/types'

export type TicketKind = 'BASIC' | 'SINGLE' | 'COMBO'
export type AudienceType = 'GENERAL' | 'ADULT' | 'STUDENT' | 'FAMILY'

export interface TicketGroup {
  key: string
  kind: TicketKind
  title: string
  subtitle?: string
  tagline?: string
  description?: string
  benefits?: string
  variants: TicketType[]
}

export const audienceLabels: Record<AudienceType, string> = {
  GENERAL: '通用',
  ADULT: '成人票',
  STUDENT: '学生票',
  FAMILY: '亲子票',
}

const audienceOrder: AudienceType[] = ['GENERAL', 'ADULT', 'STUDENT', 'FAMILY']

export function ticketKind(ticket: TicketType): TicketKind {
  if (ticket.ticketKind === 'SINGLE' || ticket.ticketKind === 'COMBO') return ticket.ticketKind
  return 'BASIC'
}

export function ticketAudience(ticket: TicketType): AudienceType {
  const value = ticket.audienceType
  return value === 'ADULT' || value === 'STUDENT' || value === 'FAMILY' ? value : 'GENERAL'
}

export function defaultVariant(group: TicketGroup): TicketType {
  return group.variants.find((ticket) => ticketAudience(ticket) === 'ADULT') || group.variants[0]
}

export function groupTickets(
  tickets: TicketType[],
  exhibitionNames: Record<number, string> = {},
): TicketGroup[] {
  const groups = new Map<string, TicketGroup>()

  for (const ticket of tickets) {
    const kind = ticketKind(ticket)
    const key = kind === 'SINGLE' ? `SINGLE-${ticket.exhibitionId ?? ticket.id}` : kind
    let group = groups.get(key)
    if (!group) {
      if (kind === 'SINGLE') {
        const fallback = ticket.name.includes('·') ? ticket.name.split('·').slice(1).join('·').trim() : ticket.name
        group = {
          key,
          kind,
          title: '单展票',
          subtitle: (ticket.exhibitionId && exhibitionNames[ticket.exhibitionId]) || fallback,
          tagline: '基础入馆 + 单展',
          description: ticket.description,
          benefits: ticket.benefits,
          variants: [],
        }
      } else if (kind === 'COMBO') {
        group = {
          key,
          kind,
          title: '全馆联票',
          subtitle: '基础入馆 + 馆内全部展览',
          tagline: '基础入馆 + 全馆展览',
          description: ticket.description,
          benefits: ticket.benefits,
          variants: [],
        }
      } else {
        group = {
          key,
          kind,
          title: '普通预约票',
          subtitle: '仅基础入馆',
          tagline: '基础入馆',
          description: ticket.description,
          benefits: ticket.benefits,
          variants: [],
        }
      }
      groups.set(key, group)
    }
    group.variants.push(ticket)
  }

  return Array.from(groups.values()).map((group) => ({
    ...group,
    variants: [...group.variants].sort(
      (a, b) => audienceOrder.indexOf(ticketAudience(a)) - audienceOrder.indexOf(ticketAudience(b)),
    ),
  }))
}

export function groupPriceRange(group: TicketGroup): { min: number; max: number } {
  const prices = group.variants.map((ticket) => Number(ticket.price))
  return { min: Math.min(...prices), max: Math.max(...prices) }
}
