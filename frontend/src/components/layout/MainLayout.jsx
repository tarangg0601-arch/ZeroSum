import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'

function MainLayout() {
  return (
    <div className="min-h-screen bg-[#F6F8F7]">

      <Sidebar />

      <div className="min-h-screen lg:ml-64">

        <Header />

        <main className="px-4 py-6 sm:px-8 sm:py-8">
          <Outlet />
        </main>

      </div>

    </div>
  )
}

export default MainLayout
