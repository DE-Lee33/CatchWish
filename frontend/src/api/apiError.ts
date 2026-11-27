
import axios from 'axios'

type ApiErrorResponse = {
  code?: string
  message?: string
}

export function getApiError(error: unknown): ApiErrorResponse | null {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) {
    return null
  }

  return error.response?.data ?? null
}
