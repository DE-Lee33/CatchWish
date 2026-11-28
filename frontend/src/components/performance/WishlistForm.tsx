import { useState, type FormEvent } from 'react'
import { toApiError } from '../../api/client.ts' 
import { addWishlist } from '../../api/wishlistApi.ts'

type WishlistFormProps = {
  performanceId: number
  onRegistered: () => void
}

function toNumberOrNull(value: string): number | null {
  return value.trim() === '' ? null : Number(value)
}

function WishlistForm({ performanceId, onRegistered }: WishlistFormProps) {
  const [sectorId, setSectorId] = useState('')
  const [priority, setPriority] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      await addWishlist(performanceId, toNumberOrNull(sectorId), toNumberOrNull(priority))
      onRegistered()
    } catch (caught) {
      setError(toApiError(caught).message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      {/* 구역 선택은 좌석 구조 API(M24)가 나오면 드롭다운으로 바꾼다. 지금은 구역 번호를 직접 입력 */}
      <label>
        구역 번호 (선택)
        <input type="number" min="1" value={sectorId} onChange={(event) => setSectorId(event.target.value)} />
      </label>
      <label>
        우선순위 (선택)
        <input type="number" min="1" value={priority} onChange={(event) => setPriority(event.target.value)} />
      </label>
      {error && <p role="alert">{error}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? '등록 중...' : '위시 등록'}
      </button>
    </form>
  )
}

export default WishlistForm
