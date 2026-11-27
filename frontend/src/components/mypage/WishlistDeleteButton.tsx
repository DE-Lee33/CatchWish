import { useState } from 'react'
import { getApiError } from '../../api/apiError.ts'
import { deleteWishlist } from '../../api/wishlistApi.ts'

type WishlistDeleteButtonProps = {
  wishlistId: number
  disabled?: boolean
  onDeleted: () => void
  onError: (message: string) => void
}

function WishlistDeleteButton({ wishlistId, disabled = false, onDeleted, onError }: WishlistDeleteButtonProps) {
  const [deleting, setDeleting] = useState(false)

  async function handleClick() {
    if (!window.confirm('위시리스트에서 삭제할까요?')) return
    setDeleting(true)
    try {
      await deleteWishlist(wishlistId)
      onDeleted()
    } catch (caught) {
      onError(getApiError(caught)?.message ?? '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setDeleting(false)
    }
  }

  return (
    <button type="button" onClick={handleClick} disabled={disabled || deleting}>
      삭제
    </button>
  )
}

export default WishlistDeleteButton
