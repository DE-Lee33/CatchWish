import { Outlet } from 'react-router'

function RootLayout() {
  return (
    <>
      {/* Header 자리 (지우 담당) */}
      <main>
        <Outlet />
      </main>
    </>
  )
}

export default RootLayout
