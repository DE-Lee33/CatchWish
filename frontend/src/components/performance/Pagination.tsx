type PaginationProps = {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

function Pagination({ page, totalPages, onChange }: PaginationProps) {
  // 공연 수가 적어 번호를 전부 그린다. 페이지가 수십 개가 되면 현재 페이지 주변만 그리도록 바꾼다
  const pages = Array.from({ length: totalPages }, (_, index) => index + 1)

  return (
    <nav>
      {/* 주소로 마지막을 넘은 페이지에 들어왔을 때도 이전이 바로 마지막 페이지로 가게 한다 */}
      <button type="button" disabled={page <= 1} onClick={() => onChange(Math.min(page - 1, totalPages))}>이전</button>
      {pages.map((number) => (
        <button key={number} type="button" disabled={number === page} onClick={() => onChange(number)}>
          {number === page ? `${number} · 현재` : number}
        </button>
      ))}
      <button type="button" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>다음</button>
    </nav>
  )
}

export default Pagination
