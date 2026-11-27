import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
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
  const [registered, setRegistered] = useState(false)

  function handleClick() {
    if (!member) {
      navigate(`/login?redirect=${encodeURIComponent(location.pathname)}`)
      return
    }
    setFormOpen((open) => !open)
  }

  if (registered) {
    return <p>위시리스트에 등록되었습니다.</p>
  }

  return (
    <div>
      <button type="button" onClick={handleClick}>
        위시리스트에 담기
      </button>
      {formOpen && (
        <WishlistForm
          performanceId={performanceId}
          onRegistered={() => {
            setRegistered(true)
            setFormOpen(false)
          }}
        />
      )}
    </div>
  )
}

export default WishlistButton
