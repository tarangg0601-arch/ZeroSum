import { ArrowRight, Check } from 'lucide-react'

const settlements = [
  {
    from: 'Priya',
    to: 'Tarang',
    amount: '₹120',
  },
  {
    from: 'Dev',
    to: 'Priya',
    amount: '₹85',
  },
  {
    from: 'Aria',
    to: 'Tarang',
    amount: '₹64',
  },
  {
    from: 'Karan',
    to: 'Dev',
    amount: '₹48',
  },
]

function UpcomingSettlements() {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

      <div className="mb-3 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">
          Upcoming settlements
        </h2>

        <button className="flex items-center gap-1 text-sm font-medium text-emerald-600 transition hover:text-emerald-700">
          View all
          <ArrowRight size={15} />
        </button>
      </div>

      <div>
        {settlements.map((settlement) => (
          <div
            key={`${settlement.from}-${settlement.to}`}
            className="flex items-center gap-3 border-b border-slate-100 py-3 last:border-0"
          >

            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
              <Check size={15} />
            </div>

            <div className="min-w-0 flex-1">
              <p className="text-sm font-medium text-slate-800">
                {settlement.from}
                <span className="text-slate-400"> → </span>
                {settlement.to}
              </p>

              <p className="mt-0.5 text-xs text-slate-400">
                Pending settlement
              </p>
            </div>

            <p className="text-sm font-semibold text-slate-800">
              {settlement.amount}
            </p>

            <button className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-600 hover:shadow-sm">
              Settle
            </button>

          </div>
        ))}
      </div>

    </section>
  )
}

export default UpcomingSettlements