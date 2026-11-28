import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { fetchWishlist } from '../../api/wishlistApi.ts'
import { useAuth } from '../auth/AuthContext.ts'
import WishlistForm from './WishlistForm.tsx'

type WishlistButtonProps = {
  performanceId: number
}

function WishlistButton({ performanceId }: WishlistButtonProps) {
  const { member } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [formOpen, setFormOpen] = useState(false)
  const [registeredCount, setRegisteredCount] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)

  // 새로고침해도 유지되도록, 서버의 내 위시리스트에서 이 공연 항목 수를 가져온다
  useEffect(() => {
    if (!member) return
    let cancelled = false
    fetchWishlist()
      .then((items) => {
        if (!cancelled) {
          setRegisteredCount(items.filter((item) => item.wishlist.performanceId === performanceId).length)
        }
      })
      .catch(() => {
        // 조회에 실패해도 등록 버튼은 그대로 쓸 수 있게 둔다 (등록 상태 문구만 안 보임)
      })
    return () => {
      cancelled = true
    }
  }, [member, performanceId, reloadKey])

  function handleClick() {
    if (!member) {
      navigate(`/login?redirect=${encodeURIComponent(location.pathname)}`)
      return
    }
    setFormOpen((open) => !open)
  }

  const count = member ? registeredCount : 0

  return (
    <div>
      {count > 0 && <p>이 공연이 위시리스트에 {count}건 등록되어 있습니다.</p>}
      <button type="button" onClick={handleClick}>
        {count > 0 ? '다른 구역도 담기' : '위시리스트에 담기'}
      </button>
      {formOpen && (
        <WishlistForm
          performanceId={performanceId}
          onRegistered={() => {
            setFormOpen(false)
            setReloadKey((key) => key + 1)
          }}
        />
      )}
    </div>
  )
}

export default WishlistButton
