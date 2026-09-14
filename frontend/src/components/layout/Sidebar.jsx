import {
  LayoutDashboard,
  Users,
  Receipt,
  ArrowLeftRight,
  Activity,
  Settings,
  LogOut,
} from 'lucide-react'
import { NavLink } from 'react-router-dom'

function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 flex h-screen w-64 flex-col border-r border-emerald-950/60 bg-[#071410] text-white">

      {/* Brand */}
      <div className="px-6 py-6">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-emerald-400 text-lg font-bold text-[#071410]">
            Z
          </div>

          <div>
            <h1 className="text-lg font-bold tracking-tight">
              ZeroSum
            </h1>

            <p className="text-[10px] text-slate-500">
              Stay even. Settle smarter.
            </p>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <nav className="mt-6 flex-1 px-4">

        <p className="mb-3 px-3 text-[10px] font-semibold uppercase tracking-widest text-slate-600">
          Workspace
        </p>

        <NavItem
          icon={<LayoutDashboard size={18} />}
          label="Dashboard"
          to="/dashboard"
        />

        <NavItem
          icon={<Users size={18} />}
          label="Groups"
          to="/groups"
        />

        <NavItem
          icon={<Receipt size={18} />}
          label="Expenses"
          to="/expenses"
        />

        <NavItem
          icon={<ArrowLeftRight size={18} />}
          label="Settlements"
          to="/balances"
        />

        <NavItem
          icon={<Activity size={18} />}
          label="Activity"
          to="/activity"
        />

        <div className="my-6 border-t border-white/5" />

        <NavItem
          icon={<Settings size={18} />}
          label="Settings"
          to="/settings"
        />

      </nav>

      {/* Profile */}
      <div className="border-t border-white/5 p-4">

        <div className="flex items-center gap-3 rounded-xl p-2">

          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-500/20 text-xs font-semibold text-emerald-300">
            TG
          </div>

          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium">
              Tarang Gupta
            </p>

            <p className="text-[11px] text-slate-500">
              View profile
            </p>
          </div>

          <button className="text-slate-600 transition hover:text-slate-300">
            <LogOut size={16} />
          </button>

        </div>

      </div>

    </aside>
  )
}

function NavItem({ icon, label, to }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `mb-1 flex w-full items-center gap-3 rounded-xl px-3 py-3 text-sm transition ${
          isActive
            ? 'bg-emerald-400/10 text-emerald-300'
            : 'text-slate-400 hover:bg-white/5 hover:text-white'
        }`
      }
    >
      {icon}
      <span>{label}</span>
    </NavLink>
  )
}

export default Sidebar