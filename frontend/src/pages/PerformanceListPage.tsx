import { useEffect, useState } from 'react'
import { fetchPerformances, type PerformanceList } from '../api/performances.ts'
import PerformanceCard from '../components/performance/PerformanceCard.tsx'

type Loaded = PerformanceList & {
  // 카드의 "예매 중/오픈 예정" 기준 시각. 렌더링 중에 Date.now()를 부르면 그릴 때마다 값이 달라져서 응답 받은 시각으로 고정한다
  loadedAt: number
}

function PerformanceListPage() {
  // null = 불러오는 중
  const [loaded, setLoaded] = useState<Loaded | null>(null)
  const [failed, setFailed] = useState(false)
  // 값이 바뀌면 useEffect가 다시 돌아 목록을 다시 부른다 ("다시 시도" 버튼용)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    fetchPerformances('', 1).then(
      (list) => setLoaded({ ...list, loadedAt: Date.now() }),
      () => setFailed(true),
    )
  }, [retryCount])

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
    if (loaded.performanceList.length === 0) return <p>등록된 공연이 없습니다.</p>
    return (
      <>
        <p>총 {loaded.totalCount}개 공연</p>
        <ul>
          {loaded.performanceList.map((performance) => (
            <li key={performance.performanceId}>
              <PerformanceCard performance={performance} now={loaded.loadedAt} />
            </li>
          ))}
        </ul>
      </>
    )
  }

  return (
    <section>
      <h1>공연 찾기</h1>
      {renderList()}
    </section>
  )
}

export default PerformanceListPage
