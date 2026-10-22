import { Activity, ArrowLeftRight, LayoutDashboard, Receipt, Settings, Users } from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { apiRequest } from '../../api/client.js'
import { signOut } from '../../api/session.js'

function Sidebar() {
  const [user, setUser] = useState(null)
  useEffect(() => { apiRequest('/users/me').then(setUser).catch(() => {}) }, [])
  const initials = user?.name?.split(/\s+/).map(part => part[0]).join('').slice(0, 2).toUpperCase() || '…'
  return <aside className="fixed left-0 top-0 hidden h-screen w-64 flex-col border-r border-emerald-950/60 bg-[#071410] text-white lg:flex">
    <div className="px-6 py-6"><div className="flex items-center gap-3"><div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-400 text-lg font-bold text-[#071410]">Z</div><div><h1 className="text-lg font-bold tracking-tight">ZeroSum</h1><p className="text-[10px] text-slate-500">Stay even. Settle smarter.</p></div></div></div>
    <nav aria-label="Workspace" className="mt-6 flex-1 px-4"><p className="mb-3 px-3 text-[10px] font-semibold uppercase tracking-widest text-slate-600">Workspace</p>
      <NavItem icon={<LayoutDashboard size={18} />} label="Dashboard" to="/dashboard" />
      <NavItem icon={<Users size={18} />} label="Groups" to="/groups" />
      <NavItem icon={<Receipt size={18} />} label="Expenses" to="/expenses" />
      <NavItem icon={<ArrowLeftRight size={18} />} label="Settlements" to="/balances" />
      <NavItem icon={<Activity size={18} />} label="Activity" to="/activity" />
      <div className="my-6 border-t border-white/5" />
      <NavItem icon={<Settings size={18} />} label="Settings" to="/settings" />
    </nav>
    <div className="border-t border-white/5 p-4"><div className="flex items-center gap-3 rounded-xl p-2"><div className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-500/20 text-xs font-semibold text-emerald-300">{initials}</div><div className="min-w-0 flex-1"><p className="truncate text-sm font-medium">{user?.name || 'Loading account…'}</p><p className="text-[11px] text-slate-500">Signed-in account</p></div><button type="button" aria-label="Sign out" onClick={signOut} className="rounded p-1 text-slate-500 hover:text-white">↪</button></div></div>
  </aside>
}

function NavItem({ icon, label, to }) {
  return <NavLink to={to} className={({ isActive }) => `mb-1 flex w-full items-center gap-3 rounded-xl px-3 py-3 text-sm transition ${isActive ? 'bg-emerald-400/10 text-emerald-300' : 'text-slate-400 hover:bg-white/5 hover:text-white'}`}>{icon}<span>{label}</span></NavLink>
}

export default Sidebar
