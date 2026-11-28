import { GENRES } from '../../api/performances.ts'

type GenreTabsProps = {
  genre: string
  onChange: (genre: string) => void
}

function GenreTabs({ genre, onChange }: GenreTabsProps) {
  return (
    <div>
      {GENRES.map(({ code, label }) => (
        // 고른 탭은 다시 눌러도 바뀔 게 없어서 비활성화한다. 스타일이 없는 지금은 이게 선택 표시도 된다
        <button key={code} type="button" disabled={code === genre} onClick={() => onChange(code)}>
          {label}
        </button>
      ))}
    </div>
  )
}

export default GenreTabs
