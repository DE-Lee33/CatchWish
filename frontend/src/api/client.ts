import axios from 'axios'

// 화면 코드는 /api 기준 경로만 쓰고, 실제 서버 주소는 vite 프록시가 결정한다
const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 서버 GlobalExceptionHandler의 오류 응답 본문 (common.error.ApiError)
export type ApiError = {
  code: string
  message: string
}

// 폼마다 code로 문구·칸을 고르므로 오류를 항상 ApiError 모양으로 맞춰 준다
export function toApiError(error: unknown): ApiError {
  const body = axios.isAxiosError<ApiError>(error) ? error.response?.data : undefined
  // Tomcat이 꺼져 있으면 Vite 프록시가 본문 없는 오류를 돌려줘 code가 없다
  if (!body?.code) {
    return { code: 'NETWORK_ERROR', message: '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.' }
  }
  return body
}

export default client
