import { useState } from 'react'
import { getApiError } from '../.././api/apiError.ts'
import { updateWishlistPriority, type WishlistItem } from '../../api/wishlistApi.ts'
import WishlistDeleteButton from './WishlistDeleteButton.tsx'

type WishlistSortableListProps = {
  items: WishlistItem[]
  onChanged: () => void
  onError: (message: string) => void
}

function WishlistSortableList({ items, onChanged, onError }: WishlistSortableListProps) {
  const [busy, setBusy] = useState(false)

  // 드래그 라이브러리를 새로 넣지 않고 위·아래 버튼으로 순서를 바꾼다.
  // 바뀐 순서대로 우선순위를 1, 2, 3...으로 다시 매기고, 값이 달라진 항목만 서버에 보낸다.
  async function move(index: number, delta: -1 | 1) {
    const target = index + delta
    if (target < 0 || target >= items.length) return

    const reordered = [...items]
    ;[reordered[index], reordered[target]] = [reordered[target], reordered[index]]

    setBusy(true)
    try {
      for (const [position, item] of reordered.entries()) {
        const priority = position + 1
        if (item.wishlist.priority !== priority) {
          await updateWishlistPriority(item.wishlist.wishlistId, priority)
        }
      }
    } catch (caught) {
      onError(getApiError(caught)?.message ?? '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.')
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
