import { ArrowRight, Receipt } from 'lucide-react'

const activities = [
  {
    title: 'Groceries',
    group: 'Hostel 4B',
    payer: 'Dev',
    amount: '₹48',
    date: 'Today, 6:42 PM',
  },
  {
    title: 'Scooter rental',
    group: 'Bali trip',
    payer: 'Tarang',
    amount: '₹36',
    date: 'Yesterday',
  },
  {
    title: 'Dinner',
    group: 'Friday poker',
    payer: 'Aria',
    amount: '₹92',
    date: '12 Mar 2025',
  },
  {
    title: 'Flights',
    group: 'Bali trip',
    payer: 'Priya',
    amount: '₹320',
    date: '10 Mar 2025',
  },
]

function RecentActivity() {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">
          Recent activity
        </h2>

        <button className="flex items-center gap-1 text-sm font-medium text-emerald-600 transition hover:text-emerald-700">
          View all
          <ArrowRight size={15} />
        </button>
      </div>

      <div>
        {activities.map((activity) => (
          <div
            key={`${activity.title}-${activity.group}`}
            className="group flex items-center gap-3 border-b border-slate-100 py-3 last:border-0"
          >
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 transition group-hover:bg-emerald-100">
              <Receipt size={16} />
            </div>

            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-slate-800">
                {activity.title}
                <span className="text-slate-400"> · </span>
                {activity.group}
              </p>

              <p className="mt-0.5 text-xs text-slate-400">
                Paid by {activity.payer} · {activity.date}
              </p>
            </div>

            <p className="text-sm font-semibold text-slate-800">
              {activity.amount}
            </p>
          </div>
        ))}
      </div>

    </section>
  )
}

export default RecentActivity