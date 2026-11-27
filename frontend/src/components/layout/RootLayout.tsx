import { Outlet } from 'react-router'
import { useAuth } from '../auth/AuthContext.ts'
import LogoutButton from '../auth/LogoutButton.tsx'

function RootLayout() {
  const { member } = useAuth()

  return (
    <>
      {/* Header 자리 (지우 담당). LogoutButton은 Header가 생기면 그 안으로 옮기는 임시 배치 */}
      <header>{member && <LogoutButton />}</header>
      <main>
        <Outlet />
      </main>
    </>
  )
}

export default RootLayout
