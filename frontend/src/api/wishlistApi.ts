import client from './client.ts'

// 서버 WishlistResponseDTO와 같은 모양 (wishlist 안에 위시 본체가 중첩)
export type WishlistItem = {
  wishlist: {
    wishlistId: number
    memberId: number
    performanceId: number
    sectorId: number | null
    priority: number | null
    createdAt: number | string
  }
  performanceTitle: string
  performanceDate: number | string | null
  sectorName: string | null
}

export async function fetchWishlist(): Promise<WishlistItem[]> {
  const response = await client.get<WishlistItem[]>('/wishlist')
  return response.data
}

export async function addWishlist(
  performanceId: number,
  sectorId: number | null,
  priority: number | null,
): Promise<WishlistItem> {
  const response = await client.post<WishlistItem>('/wishlist', { performanceId, sectorId, priority })
  return response.data
}

export async function deleteWishlist(wishlistId: number): Promise<void> {
  await client.delete(`/wishlist/${wishlistId}`)
}

export async function updateWishlistPriority(wishlistId: number, priority: number): Promise<WishlistItem[]> {
  const response = await client.put<WishlistItem[]>(`/wishlist/${wishlistId}/priority`, { priority })
  return response.data
}

// 화면에 보이는 순서대로 위시리스트 ID 전체를 보내면 서버가 한 번에 1, 2, 3... 으로 다시 매긴다
export async function reorderWishlist(wishlistIds: number[]): Promise<WishlistItem[]> {
  const response = await client.put<WishlistItem[]>('/wishlist/order', { wishlistIds })
  return response.data
}
