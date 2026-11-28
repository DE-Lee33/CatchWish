import axios from 'axios'

type ApiErrorResponse = {
  code?: string
  message?: string
}

export const NETWORK_ERROR_MESSAGE = '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'

export function getApiError(error: unknown): ApiErrorResponse | null {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return null
  }
  return error.response?.data ?? null
}

// 화면에 보여줄 오류 문구: 서버 message가 있으면 그것, 없으면 연결 오류 문구
export function getErrorMessage(error: unknown): string {
  return getApiError(error)?.message ?? NETWORK_ERROR_MESSAGE
}
