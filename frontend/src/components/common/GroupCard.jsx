import {
  ArrowRight,
  Users,
} from 'lucide-react'
import { Link } from 'react-router-dom'

function GroupCard({
  id,
  name,
  type,
  members,
  balance,
  status,
  icon: Icon = Users,
}) {
  const isPositive = balance.startsWith('+')
  const isNegative = balance.startsWith('-')
  const isSettled = status === 'Settled'

  return (
    <Link
      to={`/groups/${id}`}
      className="group block w-full rounded-2xl border border-slate-200 bg-white p-5 text-left shadow-sm transition duration-200 hover:-translate-y-1 hover:border-emerald-200 hover:shadow-md"
    >
      {/* Top */}
      <div className="flex items-start justify-between">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
          <Icon size={20} />
        </div>

        <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-50 text-slate-400 transition group-hover:bg-emerald-50 group-hover:text-emerald-600">
          <ArrowRight size={16} />
        </div>
      </div>

      {/* Group information */}
      <div className="mt-5">
        <h3 className="text-lg font-semibold text-slate-900">
          {name}
        </h3>

        <p className="mt-1 text-xs text-slate-400">
          {type}
        </p>

        <div className="mt-4 flex items-center gap-1.5 text-sm text-slate-500">
          <Users size={15} />
          {members} members
        </div>
      </div>

      {/* Balance */}
      <div className="mt-6 border-t border-slate-100 pt-4">
        <p
          className={`text-xl font-semibold ${
            isPositive
              ? 'text-emerald-600'
              : isNegative
                ? 'text-rose-500'
                : 'text-slate-600'
          }`}
        >
          {balance}
        </p>

        <p
          className={`mt-1 text-xs ${
            isSettled
              ? 'text-slate-400'
              : isPositive
                ? 'text-emerald-500'
                : 'text-rose-400'
          }`}
        >
          {status}
        </p>
      </div>
    </Link>
  )
}

export default GroupCard