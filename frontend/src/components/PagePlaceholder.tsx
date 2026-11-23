// 초기세팅용 빈 화면. 담당자가 페이지를 구현하면 이 컴포넌트 사용을 지운다
type PagePlaceholderProps = {
  pageName: string
  featureIds: string
  owner: string
}

function PagePlaceholder({ pageName, featureIds, owner }: PagePlaceholderProps) {
  return (
    <section>
      <h1>{pageName}</h1>
      <p>
        기능 {featureIds} · 담당 {owner}
      </p>
    </section>
  )
}

export default PagePlaceholder
