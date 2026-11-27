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

export default client
