import { useEffect, useState, type ReactNode } from 'react'
import { fetchMe } from '../../api/auth.ts'
import { AuthContext, type AuthState } from './AuthContext.ts'

type AuthProviderProps = {
  children: ReactNode
}

function AuthProvider({ children }: AuthProviderProps) {
  const [member, setMember] = useState<AuthState>(undefined)

  // 새로고침하면 React 상태는 사라지지만 세션 쿠키는 남아 있으므로, 켜질 때 서버에 한 번 물어 복원한다
  // 401뿐 아니라 서버가 꺼져 있어도 비로그인으로 둔다. 화면이 "확인 중"에 멈추지 않게
  useEffect(() => {
    fetchMe().then(setMember, () => setMember(null))
  }, [])

  return <AuthContext value={{ member, setMember }}>{children}</AuthContext>
}

export default AuthProvider
