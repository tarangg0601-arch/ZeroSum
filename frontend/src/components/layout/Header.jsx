import { Plus } from 'lucide-react'
import { Link, NavLink } from 'react-router-dom'
import { signOut } from '../../api/session.js'

function Header() {
  return <>
    <header className="flex min-h-20 items-center justify-between border-b border-slate-200/80 bg-[#F6F8F7] px-4 sm:px-8">
      <div className="lg:hidden"><Link to="/dashboard" className="font-bold text-emerald-900">ZeroSum</Link></div>
      <div className="hidden lg:block" aria-hidden="true" />
      <div className="flex items-center gap-2 sm:gap-3">
        <button type="button" onClick={signOut} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-600">Sign out</button>
        <Link to="/expenses/new" className="flex items-center gap-2 rounded-xl bg-emerald-500 px-3 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-600 sm:px-4"><Plus size={17} /><span className="hidden sm:inline">Add Expense</span><span className="sm:hidden">Add</span></Link>
      </div>
    </header>
    <nav aria-label="Mobile navigation" className="grid grid-cols-4 gap-1 border-b border-slate-200 bg-white px-2 py-2 text-center text-xs lg:hidden">
      {[['Home', '/dashboard'], ['Groups', '/groups'], ['Expenses', '/expenses'], ['Balances', '/balances']].map(([label, to]) => <NavLink key={to} to={to} className={({ isActive }) => `rounded-lg px-2 py-2 ${isActive ? 'bg-emerald-50 font-semibold text-emerald-700' : 'text-slate-600'}`}>{label}</NavLink>)}
    </nav>
  </>
}

export default Header
