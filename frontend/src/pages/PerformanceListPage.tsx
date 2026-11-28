import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { fetchPerformances, GENRES, PAGE_SIZE, type PerformanceList } from '../api/performances.ts'
import GenreTabs from '../components/performance/GenreTabs.tsx'
import Pagination from '../components/performance/Pagination.tsx'
import PerformanceCard from '../components/performance/PerformanceCard.tsx'

type Loaded = PerformanceList & {
  // 카드의 "예매 중/오픈 예정" 기준 시각. 렌더링 중에 Date.now()를 부르면 그릴 때마다 값이 달라져서 응답 받은 시각으로 고정한다
  loadedAt: number
}

function PerformanceListPage() {
  // 장르·페이지를 state가 아니라 주소에 두어 새로고침·뒤로 가기·링크 공유에도 같은 목록이 나오게 한다
  const [searchParams, setSearchParams] = useSearchParams()
  const genre = searchParams.get('genre') ?? ''
  // 주소창은 누구나 고칠 수 있어서 숫자가 아니거나 1보다 작으면 1페이지로 본다
  const page = Math.max(1, Number.parseInt(searchParams.get('page') ?? '', 10) || 1)

  // null = 처음 불러오는 중
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [failed, setFailed] = useState(false)
  // 값이 바뀌면 useEffect가 다시 돌아 목록을 다시 부른다 ("다시 시도" 버튼용)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    // 탭을 빠르게 바꾸면 앞 요청의 응답이 늦게 와서 새 목록을 덮을 수 있어, 바뀐 뒤 도착한 응답은 버린다
    let ignore = false
    fetchPerformances(genre, page).then(
      (list) => {
        if (ignore) return
        setFailed(false)
        setLoaded({ ...list, loadedAt: Date.now() })
      },
      () => {
        if (!ignore) setFailed(true)
      },
    )
    return () => {
      ignore = true
    }
  }, [genre, page, retryCount])

  function moveTo(nextGenre: string, nextPage: number) {
    // 기본값(전체, 1페이지)은 주소에서 빼서 첫 화면 주소가 /로 남게 한다
    const params = new URLSearchParams()
    if (nextGenre) params.set('genre', nextGenre)
    if (nextPage > 1) params.set('page', String(nextPage))
    setSearchParams(params)
  }

  function retry() {
    setFailed(false)
    setRetryCount((count) => count + 1)
  }

  function renderList() {
    if (failed) {
      return (
        <>
          <p role="alert">공연 목록을 불러오지 못했습니다.</p>
          <button type="button" onClick={retry}>다시 시도</button>
        </>
      )
    }
    if (!loaded) return <p>불러오는 중...</p>
    // 목록이 비어도 공연이 있으면 주소의 page가 마지막 페이지를 넘은 것이라 문구를 나누고 페이지 버튼은 남긴다
    if (loaded.totalCount === 0) return <p>등록된 공연이 없습니다.</p>
    const genreLabel = GENRES.find(({ code }) => code === genre)?.label ?? genre
    return (
      <>
        <p>{genreLabel} · 총 {loaded.totalCount}개 공연</p>
        {loaded.performanceList.length === 0 ? (
          <p>없는 페이지입니다. 아래 번호를 눌러 이동해주세요.</p>
        ) : (
          <ul>
            {loaded.performanceList.map((performance) => (
              <li key={performance.performanceId}>
                <PerformanceCard performance={performance} now={loaded.loadedAt} />
              </li>
            ))}
          </ul>
        )}
        <Pagination
          page={page}
          totalPages={Math.ceil(loaded.totalCount / PAGE_SIZE)}
          onChange={(nextPage) => moveTo(genre, nextPage)}
        />
      </>
    )
  }

  return (
    <section>
      <h1>공연 찾기</h1>
      {/* 장르를 바꾸면 그 장르의 페이지 수가 달라서 1페이지로 돌아간다 */}
      <GenreTabs genre={genre} onChange={(nextGenre) => moveTo(nextGenre, 1)} />
      {renderList()}
    </section>
  )
}

export default PerformanceListPage
