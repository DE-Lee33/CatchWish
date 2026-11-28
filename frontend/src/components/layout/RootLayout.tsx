import { Outlet } from 'react-router'
import { useAuth } from '../auth/AuthContext.ts'
import LogoutButton from '../auth/LogoutButton.tsx'
import NotificationPopover from './NotificationPopover.tsx'

function RootLayout() {
  const { member } = useAuth()

  return (
    <>
      {/* Header 자리 (지우 담당). LogoutButton은 Header가 생기면 그 안으로 옮기는 임시 배치 */}	  
	  <header>
	          {/* 로그인한 회원에게만 알림 버튼과 로그아웃 버튼을 표시한다. */}
	          {member && (
	            <>
	              {/* 로그인 회원이 바뀌면 알림 목록 상태를 초기화한다. */}
	              <NotificationPopover key={member.memberId} />
	              <LogoutButton />
	            </>
	          )}
	        </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}

export default RootLayout
