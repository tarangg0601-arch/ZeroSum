import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  ChevronRight,
  CircleDollarSign,
  Lightbulb,
  Users,
  WalletCards,
} from 'lucide-react'
import { Link } from 'react-router-dom'

const balances = [
  {
    name: 'Tarang',
    initials: 'TG',
    amount: '+₹210',
    status: 'You are owed',
    positive: true,
  },
  {
    name: 'Priya',
    initials: 'PR',
    amount: '+₹70',
    status: 'Gets back',
    positive: true,
  },
  {
    name: 'Dev',
    initials: 'DV',
    amount: '-₹130',
    status: 'Needs to pay',
    positive: false,
  },
  {
    name: 'Aria',
    initials: 'AR',
    amount: '-₹80',
    status: 'Needs to pay',
    positive: false,
  },
  {
    name: 'Karan',
    initials: 'KA',
    amount: '-₹70',
    status: 'Needs to pay',
    positive: false,
  },
]

const settlements = [
  {
    from: 'Dev',
    fromInitials: 'DV',
    to: 'Tarang',
    toInitials: 'TG',
    amount: '₹130',
  },
  {
    from: 'Aria',
    fromInitials: 'AR',
    to: 'Tarang',
    toInitials: 'TG',
    amount: '₹80',
  },
  {
    from: 'Karan',
    fromInitials: 'KA',
    to: 'Priya',
    toInitials: 'PR',
    amount: '₹70',
  },
]

const explanationSteps = [
  'ZeroSum calculates the net balance of every member.',
  'Members who owe money are matched with members who should receive money.',
  'The largest compatible balances are settled first.',
  'The process continues until every balance reaches zero.',
]

function Balances() {
  return (
    <div className="mx-auto max-w-[1400px]">

      {/* Back */}
      <Link
        to="/groups/1"
        className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-emerald-600"
      >
        <ArrowLeft size={17} />
        Back to Bali trip
      </Link>

      {/* Header */}
      <section className="mb-8 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">

        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
            Settlement optimizer
          </p>

          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
            Balances & settlement
          </h1>

          <p className="mt-2 max-w-2xl text-sm text-slate-500">
            ZeroSum has optimized the group balances into the fewest practical payments.
          </p>
        </div>

        <div className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2.5">
          <CheckCircle2 size={17} className="text-emerald-600" />

          <span className="text-sm font-semibold text-emerald-700">
            Ready to settle
          </span>
        </div>

      </section>

      {/* Summary */}
      <section className="mb-6 grid gap-4 md:grid-cols-3">

        <SummaryCard
          icon={<WalletCards size={19} />}
          label="Your net balance"
          value="+₹210"
          positive
          description="You should receive"
        />

        <SummaryCard
          icon={<Users size={19} />}
          label="Members with balances"
          value="5"
          description="All members included"
        />

        <SummaryCard
          icon={<CircleDollarSign size={19} />}
          label="Optimized payments"
          value="3"
          description="Payments to settle group"
        />

      </section>

      {/* Main settlement card */}
      <section className="mb-6 overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">

        <div className="flex flex-col gap-4 border-b border-slate-100 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">

          <div>
            <h2 className="font-semibold text-slate-900">
              Optimized settlement plan
            </h2>

            <p className="mt-1 text-xs text-slate-400">
              Follow these payments to settle the group.
            </p>
          </div>

          <div className="rounded-lg bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-600">
            3 transactions
          </div>

        </div>

        <div className="divide-y divide-slate-100">

          {settlements.map((settlement, index) => (
            <div
              key={`${settlement.from}-${settlement.to}`}
              className="flex flex-col gap-4 px-6 py-5 sm:flex-row sm:items-center"
            >

              {/* Step number */}
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-50 text-xs font-bold text-emerald-600">
                {index + 1}
              </div>

              {/* From */}
              <div className="flex min-w-[150px] items-center gap-3">

                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-600">
                  {settlement.fromInitials}
                </div>

                <div>
                  <p className="text-sm font-semibold text-slate-800">
                    {settlement.from}
                  </p>

                  <p className="text-xs text-rose-500">
                    Pays
                  </p>
                </div>

              </div>

              {/* Arrow */}
              <div className="hidden flex-1 items-center justify-center sm:flex">
                <div className="flex w-full max-w-[180px] items-center">

                  <div className="h-px flex-1 bg-slate-200" />

                  <div className="mx-2 flex h-8 w-8 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
                    <ArrowRight size={15} />
                  </div>

                  <div className="h-px flex-1 bg-slate-200" />

                </div>
              </div>

              {/* To */}
              <div className="flex min-w-[150px] items-center gap-3">

                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-50 text-xs font-semibold text-emerald-700">
                  {settlement.toInitials}
                </div>

                <div>
                  <p className="text-sm font-semibold text-slate-800">
                    {settlement.to}
                  </p>

                  <p className="text-xs text-emerald-500">
                    Receives
                  </p>
                </div>

              </div>

              {/* Amount */}
              <div className="sm:ml-auto sm:text-right">
                <p className="text-lg font-bold text-slate-900">
                  {settlement.amount}
                </p>

                <p className="text-xs text-slate-400">
                  payment
                </p>
              </div>

            </div>
          ))}

        </div>

      </section>

      {/* Balances + explanation */}
      <div className="grid gap-5 xl:grid-cols-[1fr_0.8fr]">

        {/* Individual balances */}
        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">

          <div className="border-b border-slate-100 px-6 py-5">

            <h2 className="font-semibold text-slate-900">
              Net balances
            </h2>

            <p className="mt-1 text-xs text-slate-400">
              Each member&apos;s position after all expenses.
            </p>

          </div>

          <div className="divide-y divide-slate-100">

            {balances.map((balance) => (
              <div
                key={balance.name}
                className="flex items-center gap-3 px-6 py-4"
              >

                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-600">
                  {balance.initials}
                </div>

                <div className="min-w-0 flex-1">

                  <p className="text-sm font-semibold text-slate-800">
                    {balance.name}
                  </p>

                  <p
                    className={`mt-0.5 text-xs ${
                      balance.positive
                        ? 'text-emerald-500'
                        : 'text-rose-400'
                    }`}
                  >
                    {balance.status}
                  </p>

                </div>

                <div
                  className={`text-sm font-bold ${
                    balance.positive
                      ? 'text-emerald-600'
                      : 'text-rose-500'
                  }`}
                >
                  {balance.amount}
                </div>

              </div>
            ))}

          </div>

        </section>

        {/* How it works */}
        <section className="overflow-hidden rounded-2xl border border-emerald-200 bg-emerald-50/50">

          <div className="border-b border-emerald-100 px-6 py-5">

            <div className="flex items-center gap-3">

              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white text-emerald-600 shadow-sm">
                <Lightbulb size={19} />
              </div>

              <div>
                <h2 className="font-semibold text-slate-900">
                  How ZeroSum calculated this
                </h2>

                <p className="mt-1 text-xs text-slate-500">
                  Greedy balance optimization
                </p>
              </div>

            </div>

          </div>

          <div className="p-6">

            <div className="space-y-5">

              {explanationSteps.map((step, index) => (
                <div
                  key={step}
                  className="flex gap-3"
                >

                  <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-[11px] font-bold text-emerald-700">
                    {index + 1}
                  </div>

                  <p className="pt-0.5 text-sm leading-6 text-slate-600">
                    {step}
                  </p>

                </div>
              ))}

            </div>

            <div className="mt-6 rounded-xl border border-emerald-100 bg-white p-4">

              <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
                Result
              </p>

              <p className="mt-2 text-sm font-medium leading-6 text-slate-700">
                The group can be settled using only{' '}
                <span className="font-bold text-slate-900">
                  3 payments
                </span>
                .
              </p>

              <Link
                to="/groups/1"
                className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-emerald-600 transition hover:text-emerald-700"
              >
                Return to group
                <ChevronRight size={14} />
              </Link>

            </div>

          </div>

        </section>

      </div>

    </div>
  )
}

function SummaryCard({
  icon,
  label,
  value,
  description,
  positive = false,
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">

      <div className="flex items-center gap-3">

        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
          {icon}
        </div>

        <div>
          <p className="text-xs text-slate-400">
            {label}
          </p>

          <p
            className={`mt-1 text-xl font-semibold ${
              positive
                ? 'text-emerald-600'
                : 'text-slate-900'
            }`}
          >
            {value}
          </p>

          <p className="mt-0.5 text-[11px] text-slate-400">
            {description}
          </p>
        </div>

      </div>

    </div>
  )
}

export default Balances