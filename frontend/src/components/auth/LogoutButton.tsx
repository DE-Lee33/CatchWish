import { useNavigate } from 'react-router'
import { logout } from '../../api/auth.ts'
import { useAuth } from './AuthContext.ts'

function LogoutButton() {
  const { setMember } = useAuth()
  const navigate = useNavigate()

  async function handleClick() {
    try {
      await logout()
    } catch {
      // 서버 세션이 남아 있는데 화면만 비로그인으로 바꾸면 사용자는 로그아웃된 줄 안다 (공용 PC에서 위험)
      alert('로그아웃에 실패했습니다. 잠시 후 다시 시도해주세요.')
      return
    }
    setMember(null)
    navigate('/')
  }

  return (
    <button type="button" onClick={handleClick}>
      로그아웃
    </button>
  )
}

export default LogoutButton
