import { ArrowRight, Info } from 'lucide-react'

const payments = [
  ['Priya', 'Tarang', '₹120'],
  ['Dev', 'Priya', '₹85'],
  ['Aria', 'Tarang', '₹64'],
  ['Karan', 'Dev', '₹48'],
]

function SettlementOptimizer() {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-semibold text-slate-900">
            Settlement optimizer
          </h2>

          <Info
            size={15}
            className="text-slate-400"
          />
        </div>

        <button className="flex items-center gap-1 text-sm font-medium text-emerald-600 transition hover:text-emerald-700">
          View details
          <ArrowRight size={15} />
        </button>
      </div>

      {/* Summary */}
      <div className="mt-4">
        <p className="text-xl font-semibold tracking-tight text-slate-900">
          9 debts reduced to 4 payments
        </p>

        <p className="mt-1 text-sm text-slate-500">
          That's{' '}
          <span className="font-semibold text-emerald-600">
            56% fewer
          </span>{' '}
          transactions!
        </p>
      </div>

      {/* Optimization visual */}
      <div className="mt-6 grid grid-cols-[1fr_auto_1fr] items-center gap-4">

        {/* Before */}
        <div>
          <p className="text-center text-[11px] font-bold tracking-wider text-rose-400">
            BEFORE
          </p>

          <p className="mt-1 text-center text-xs text-slate-400">
            9 debts
          </p>

          <div className="mx-auto mt-4 grid w-24 grid-cols-3 gap-3">
            {Array.from({ length: 9 }).map((_, index) => (
              <span
                key={index}
                className="mx-auto h-3 w-3 rounded-full bg-slate-300"
              />
            ))}
          </div>
        </div>

        {/* Arrow */}
        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-50">
          <ArrowRight
            size={20}
            className="text-slate-400"
          />
        </div>

        {/* After */}
        <div>
          <p className="text-center text-[11px] font-bold tracking-wider text-emerald-500">
            AFTER
          </p>

          <p className="mt-1 text-center text-xs text-slate-400">
            4 payments
          </p>

          <div className="mx-auto mt-4 grid w-20 grid-cols-2 gap-4">
            {Array.from({ length: 4 }).map((_, index) => (
              <span
                key={index}
                className="mx-auto h-4 w-4 rounded-full bg-emerald-400 shadow-sm shadow-emerald-200"
              />
            ))}
          </div>
        </div>

      </div>

      {/* Payments */}
      <div className="mt-6 border-t border-slate-100 pt-4">
        <div className="space-y-2.5">
          {payments.map(([from, to, amount]) => (
            <div
              key={`${from}-${to}`}
              className="flex items-center justify-between text-sm"
            >
              <span className="font-medium text-slate-700">
                {from}{' '}
                <span className="text-slate-400">→</span>{' '}
                {to}
              </span>

              <span className="font-semibold text-slate-800">
                {amount}
              </span>
            </div>
          ))}
        </div>
      </div>

    </section>
  )
}

export default SettlementOptimizer