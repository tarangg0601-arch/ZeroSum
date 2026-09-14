import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'

function MainLayout() {
  return (
    <div className="min-h-screen bg-[#F6F8F7]">

      <Sidebar />

      <div className="ml-64 min-h-screen">

        <Header />

        <main className="px-8 py-8">
          <Outlet />
        </main>

      </div>

    </div>
  )
}

export default MainLayout