import dayjs from 'dayjs'

const WEEK_LABELS = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']

export function parseClosedDays(value?: string | null): number[] {
  if (!value) return []
  return Array.from(
    new Set(
      value
        .split(',')
        .map((part) => Number(part.trim()))
        .filter((day) => Number.isInteger(day) && day >= 1 && day <= 7),
    ),
  ).sort((a, b) => a - b)
}

export function formatClosedDays(value?: string | null): string {
  const days = parseClosedDays(value)
  return days.length ? days.map((day) => WEEK_LABELS[day]).join('、') : '无'
}

export function isClosedOn(value: string | null | undefined, date: string | Date): boolean {
  const days = parseClosedDays(value)
  if (!days.length) return false
  return days.includes(dayjs(date).day() || 7)
}