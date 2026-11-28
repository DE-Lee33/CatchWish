import client from './client.ts'

// 서버 model.domain.Performance와 같은 모양. 날짜는 epoch 밀리초 숫자로 온다 (docs/API.md)
export type Performance = {
  performanceId: number
  registrantId: number
  managerId: number | null
  venueTemplateId: number | null
  performanceName: string
  genre: string
  castInfo: string
  description: string
  posterUrl: string | null
  runningTime: number
  showAt: number
  bookingOpenAt: number
  cancelDeadlineAt: number
  maxTicketsPerMember: number
  venueName: string
  venueAddress: string
  approvalStatus: 'P' | 'A' | 'R'
  approved: boolean
  rejectReason: string | null
  reviewedAt: number | null
  createdAt: number
}

// 서버 model.dto.PerformanceListResponse
export type PerformanceList = {
  performanceList: Performance[]
  totalCount: number
}

export async function fetchPerformances(genre: string, page: number): Promise<PerformanceList> {
  const response = await client.get<PerformanceList>('/performances', { params: { genre, page } })
  return response.data
}
