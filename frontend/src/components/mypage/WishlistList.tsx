import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { toApiError } from '../../api/client.ts'
import { fetchWishlist, type WishlistItem } from '../../api/wishlistApi.ts'
import WishlistSortableList from './WishlistSortableList.tsx'

function WishlistList() {
  const navigate = useNavigate()
  const [items, setItems] = useState<WishlistItem[] | null>(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setItems(await fetchWishlist())
      setError('')
    } catch (caught) {
      const apiError = toApiError(caught)
      if (apiError.code === 'AUTH_REQUIRED') {
        navigate('/login?redirect=/mypage/wishlist')
        return
      }
      setError(apiError.message)
    }
  }, [navigate])

  useEffect(() => {
    void load()
  }, [load])

  if (items === null && !error) return <p>불러오는 중...</p>

  return (
    <div>
      {error && <p role="alert">{error}</p>}
      {items !== null && items.length === 0 && <p>등록된 위시리스트가 없습니다.</p>}
      {items !== null && items.length > 0 && (
        <WishlistSortableList items={items} onChanged={() => void load()} onError={setError} />
      )}
    </div>
  )
}

export default WishlistList
