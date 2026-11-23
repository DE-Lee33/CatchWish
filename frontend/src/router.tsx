import { createBrowserRouter, Navigate } from 'react-router'
import RootLayout from './components/layout/RootLayout.tsx'
import BookingHistoryList from './components/mypage/BookingHistoryList.tsx'
import MyPerformanceList from './components/mypage/MyPerformanceList.tsx'
import WishlistList from './components/mypage/WishlistList.tsx'
import AdminReviewPage from './pages/AdminReviewPage.tsx'
import BookingCompletePage from './pages/BookingCompletePage.tsx'
import BookingDetailPage from './pages/BookingDetailPage.tsx'
import LoginPage from './pages/LoginPage.tsx'
import MyInfoPage from './pages/MyInfoPage.tsx'
import MyPage from './pages/MyPage.tsx'
import NotFoundPage from './pages/NotFoundPage.tsx'
import PaymentPage from './pages/PaymentPage.tsx'
import PerformanceDetailPage from './pages/PerformanceDetailPage.tsx'
import PerformanceEditPage from './pages/PerformanceEditPage.tsx'
import PerformanceListPage from './pages/PerformanceListPage.tsx'
import PerformanceRegisterPage from './pages/PerformanceRegisterPage.tsx'
import QueuePage from './pages/QueuePage.tsx'
import SearchResultPage from './pages/SearchResultPage.tsx'
import SeatSelectPage from './pages/SeatSelectPage.tsx'
import SignupPage from './pages/SignupPage.tsx'

// 노션 자료실 "프론트 라우트 명세"와 한 줄씩 대응. 주소를 바꾸면 명세도 같이 고친다
export const router = createBrowserRouter([
  {
    element: <RootLayout />,
    children: [
      { path: '/', element: <PerformanceListPage /> },
      { path: '/search', element: <SearchResultPage /> },
      { path: '/login', element: <LoginPage /> },
      { path: '/signup', element: <SignupPage /> },
      { path: '/me', element: <MyInfoPage /> },
      {
        path: '/mypage',
        element: <MyPage />,
        children: [
          { index: true, element: <Navigate to="/mypage/bookings" replace /> },
          { path: 'bookings', element: <BookingHistoryList /> },
          { path: 'performances', element: <MyPerformanceList /> },
          { path: 'wishlist', element: <WishlistList /> },
        ],
      },
      { path: '/performances/new', element: <PerformanceRegisterPage /> },
      { path: '/performances/:performanceId', element: <PerformanceDetailPage /> },
      { path: '/performances/:performanceId/edit', element: <PerformanceEditPage /> },
      { path: '/performances/:performanceId/queue', element: <QueuePage /> },
      { path: '/performances/:performanceId/seats', element: <SeatSelectPage /> },
      { path: '/bookings/:bookingId', element: <BookingDetailPage /> },
      { path: '/bookings/:bookingId/payment', element: <PaymentPage /> },
      { path: '/bookings/:bookingId/complete', element: <BookingCompletePage /> },
      { path: '/admin/reviews', element: <AdminReviewPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
