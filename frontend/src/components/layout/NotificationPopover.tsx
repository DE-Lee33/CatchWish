import { useRef, useState } from 'react'
import client from '../../api/client.ts'

type NotificationItem = {
  notificationId: number
  notificationType: string
}

type NotificationState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'ready'; items: NotificationItem[] }

export default function NotificationPopover() {
  const buttonRef = useRef<HTMLButtonElement>(null)
  const [open, setOpen] = useState(false)
  const [state, setState] = useState<NotificationState>({ status: 'loading' })

  // M32-1: 알림 목록을 열 때 로그인한 회원의 최신 알림을 조회한다.
  async function toggleNotifications() {
    if (open) {
      setOpen(false)
      return
    }

    setState({ status: 'loading' })
    setOpen(true)

    try {
      // client의 기본 경로가 /api이므로 GET /api/notifications로 요청한다.
      const { data } = await client.get<NotificationItem[]>('/notifications')
      setState({ status: 'ready', items: data })
    } catch {
      setState({ status: 'error' })
    }
  }

  return (
    <div
      onKeyDown={(event) => {
        if (event.key === 'Escape' && open) {
          setOpen(false)
          buttonRef.current?.focus()
        }
      }}
    >
      <button
        ref={buttonRef}
        type="button"
        aria-label="알림 목록"
        aria-expanded={open}
        onClick={toggleNotifications}
      >
        <span aria-hidden="true">🔔</span>
      </button>

      {open && (
        <section aria-label="알림">
          <h2>알림</h2>

          {state.status === 'loading' && (
            <p role="status">알림을 불러오는 중이에요.</p>
          )}

          {state.status === 'error' && (
            <p role="alert">알림을 불러오지 못했어요.</p>
          )}

          {state.status === 'ready' && state.items.length === 0 && (
            <p>아직 알림이 없어요.</p>
          )}

          {state.status === 'ready' && state.items.length > 0 && (
            <ul>
              {state.items.map((item) => (
                <li key={item.notificationId}>
                  {item.notificationType === 'CANCEL' ? '취소표 재오픈 안내' : '공연 알림'}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  )
}