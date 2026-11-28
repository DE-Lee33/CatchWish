import { useState } from 'react'
import { toApiError } from '../../api/client.ts'
import { reorderWishlist, type WishlistItem } from '../../api/wishlistApi.ts'
import WishlistDeleteButton from './WishlistDeleteButton.tsx'

type WishlistSortableListProps = {
  items: WishlistItem[]
  onChanged: () => void
  onError: (message: string) => void
}

function WishlistSortableList({ items, onChanged, onError }: WishlistSortableListProps) {
  const [busy, setBusy] = useState(false)

  // 드래그 라이브러리를 새로 넣지 않고 위·아래 버튼으로 순서를 바꾼다.
  // 바뀐 순서의 ID 목록을 서버에 한 번만 보내고, 서버가 한 트랜잭션으로 우선순위를 다시 매긴다.
  async function move(index: number, delta: -1 | 1) {
    const target = index + delta
    if (target < 0 || target >= items.length) return

    const ids = items.map((item) => item.wishlist.wishlistId)
    const moved = ids.splice(index, 1)[0]
    ids.splice(target, 0, moved)

    setBusy(true)
    try {
      await reorderWishlist(ids)
    } catch (caught) {
      onError(toApiError(caught).message)
    } finally {
      setBusy(false)
      onChanged() // 성공이든 실패든 서버 기준으로 다시 불러와 화면과 DB를 맞춘다
    }
  }

  return (
    <ol>
      {items.map((item, index) => (
        <li key={item.wishlist.wishlistId}>
          <strong>{item.performanceTitle}</strong>
          {item.sectorName && <span> · {item.sectorName}</span>}
          {item.performanceDate && <span> · {new Date(item.performanceDate).toLocaleDateString('ko-KR')}</span>}
          <button type="button" onClick={() => move(index, -1)} disabled={busy || index === 0}>
            위로
          </button>
          <button type="button" onClick={() => move(index, 1)} disabled={busy || index === items.length - 1}>
            아래로
          </button>
          <WishlistDeleteButton
            wishlistId={item.wishlist.wishlistId}
            disabled={busy}
            onDeleted={onChanged}
            onError={onError}
          />
        </li>
      ))}
    </ol>
  )
}

export default WishlistSortableList
