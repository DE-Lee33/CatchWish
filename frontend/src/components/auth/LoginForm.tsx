import axios from 'axios'
import { useState, type FormEvent } from 'react'
import { login } from '../../api/auth.ts'
import type { ApiError } from '../../api/client.ts'
import { useAuth } from './AuthContext.ts'

// 화면 문구를 서버 수정 없이 바꾸려고 code별로 둔다. 표에 없는 code는 서버 message를 쓴다
const ERROR_MESSAGES: Record<string, string> = {
  LOGIN_FAILED: '아이디 또는 비밀번호가 올바르지 않습니다.',
  BAD_REQUEST: '아이디와 비밀번호를 입력해주세요.',
}

function toErrorMessage(error: unknown): string {
  const body = axios.isAxiosError<ApiError>(error) ? error.response?.data : undefined
  // Tomcat이 꺼져 있으면 Vite 프록시가 본문 없는 오류를 돌려줘 code가 없다
  if (!body?.code) return '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'
  return ERROR_MESSAGES[body.code] ?? body.message
}

function LoginForm() {
  const { setMember } = useAuth()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      // 이동은 여기서 하지 않는다. member가 바뀌면 LoginPage가 redirect 주소로 보낸다
      setMember(await login(loginId, password))
    } catch (caught) {
      setError(toErrorMessage(caught))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <label>
        아이디
        <input
          value={loginId}
          onChange={(event) => setLoginId(event.target.value)}
          placeholder="아이디를 입력하세요"
          autoComplete="username"
          required
        />
      </label>
      <label>
        비밀번호
        <input
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="비밀번호를 입력하세요"
          autoComplete="current-password"
          required
        />
      </label>
      {error && <p role="alert">{error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? '로그인 중...' : '로그인'}
      </button>
    </form>
  )
}

export default LoginForm
