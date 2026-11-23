import { NavLink, Outlet } from 'react-router'

// 탭마다 주소를 따로 둬서 새로고침·뒤로가기에도 탭이 유지되게 한다
function MyPage() {
  return (
    <section>
      <h1>MyPage</h1>
      <nav>
        <NavLink to="/mypage/bookings">예매 내역</NavLink>
        {' | '}
        <NavLink to="/mypage/performances">내 공연</NavLink>
        {' | '}
        <NavLink to="/mypage/wishlist">위시리스트</NavLink>
      </nav>
      <Outlet />
    </section>
  )
}

export default MyPage
