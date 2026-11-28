import { Link } from 'react-router'
import type { Performance } from '../../api/performances.ts'

const WEEKDAYS = '일월화수목금토'

// 화면 01 시안 형식: 2026.12.20 (일) 18:00
function formatDateTime(epochMillis: number): string {
  const date = new Date(epochMillis)
  const twoDigits = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}.${twoDigits(date.getMonth() + 1)}.${twoDigits(date.getDate())}`
    + ` (${WEEKDAYS[date.getDay()]}) ${twoDigits(date.getHours())}:${twoDigits(date.getMinutes())}`
}

type PerformanceCardProps = {
  performance: Performance
  now: number
}

function PerformanceCard({ performance, now }: PerformanceCardProps) {
  // 목록에는 공연 전인 공연만 오므로 예매 오픈 시각만 보면 상태가 정해진다
  const bookingOpened = performance.bookingOpenAt <= now

  return (
    <article>
      <p>{bookingOpened ? '예매 중' : '오픈 예정'}</p>
      <h2>{performance.performanceName}</h2>
      <p>{formatDateTime(performance.showAt)} · {performance.venueName}</p>
      {!bookingOpened && <p>예매 오픈 {formatDateTime(performance.bookingOpenAt)}</p>}
      <Link to={`/performances/${performance.performanceId}`}>공연 상세</Link>
    </article>
  )
}

export default PerformanceCard
