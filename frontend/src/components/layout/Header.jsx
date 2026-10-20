import { Bell, Plus, Search } from 'lucide-react'
import { Link } from 'react-router-dom'

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`

async function signOut() {
  try { await fetch(`${API_BASE_URL}/auth/logout`, { method: 'POST' }) } catch { /* clear the local bearer regardless */ }
  sessionStorage.removeItem('zerosum_token')
  sessionStorage.removeItem('zerosum_user_id')
  window.location.assign('/login')
}

function Header() {
  return (
    <header className="flex h-20 items-center justify-between border-b border-slate-200/80 bg-[#F6F8F7] px-8">

      {/* Search */}
      <div className="relative w-80">
        <Search
          size={17}
          className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
        />

        <input
          type="text"
          placeholder="Search groups, expenses, or people..."
          className="h-10 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 text-sm text-slate-700 outline-none transition placeholder:text-slate-400 focus:border-emerald-400"
        />
      </div>

      {/* Actions */}
      <div className="flex items-center gap-3">

        <button type="button" onClick={signOut} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-600">Sign out</button>

        {/* Notifications */}
        <button
          type="button"
          className="relative flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-500 transition hover:bg-slate-50"
        >
          <Bell size={18} />

          <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-red-400" />
        </button>

        {/* Add Expense */}
        <Link
          to="/expenses/new"
          className="flex items-center gap-2 rounded-xl bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-600"
        >
          <Plus size={17} />
          Add Expense
        </Link>

      </div>

    </header>
  )
}

export default Header
