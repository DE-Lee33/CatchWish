import { Link, Navigate, useSearchParams } from 'react-router'
import { useAuth } from '../components/auth/AuthContext.ts'
import LoginForm from '../components/auth/LoginForm.tsx'

// 주소창 값은 누구나 바꿀 수 있어서, 우리 사이트 안 경로만 허용한다 (오픈 리다이렉트 방지)
// "//evil.com"은 /로 시작하지만 브라우저가 다른 사이트 주소로 해석하므로 막는다
function toSafePath(redirect: string | null): string {
  if (!redirect || !redirect.startsWith('/') || redirect.startsWith('//')) return '/'
  return redirect
}

function LoginPage() {
  const { member } = useAuth()
  const [searchParams] = useSearchParams()

  // 확인 중에 폼을 그리면 로그인한 사람에게 폼이 잠깐 보였다 사라진다
  if (member === undefined) return null
  // 로그인 성공 직후와 이미 로그인한 채 들어온 경우를 같은 길로 보낸다. replace라 뒤로 가기로 로그인 화면에 돌아오지 않는다
  if (member) return <Navigate to={toSafePath(searchParams.get('redirect'))} replace />

  return (
    <section>
      <h1>로그인</h1>
      <p>공연 예매와 내 정보를 이용하려면 로그인해주세요.</p>
      <LoginForm />
      <Link to="/signup">회원가입</Link>
      <Link to="/">홈으로</Link>
    </section>
  )
}

export default LoginPage
