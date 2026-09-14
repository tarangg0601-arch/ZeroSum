function BalanceOverview() {
  const balances = [
    {
      label: 'You are owed',
      amount: '₹1,248',
      description: 'Across 3 groups',
      type: 'positive',
    },
    {
      label: 'You owe',
      amount: '₹342',
      description: 'To 2 people',
      type: 'negative',
    },
    {
      label: 'Net balance',
      amount: '+₹906',
      description: "You're ahead overall",
      type: 'positive',
    },
    {
      label: 'Active groups',
      amount: '3',
      description: '2 with open balances',
      type: 'neutral',
    },
  ]

  return (
    <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {balances.map((balance) => (
        <div
          key={balance.label}
          className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
        >
          <p className="text-sm font-medium text-slate-500">
            {balance.label}
          </p>

          <p
            className={`mt-3 text-3xl font-semibold tracking-tight ${
              balance.type === 'positive'
                ? 'text-emerald-600'
                : balance.type === 'negative'
                  ? 'text-rose-500'
                  : 'text-slate-900'
            }`}
          >
            {balance.amount}
          </p>

          <p className="mt-1 text-xs text-slate-400">
            {balance.description}
          </p>
        </div>
      ))}
    </section>
  )
}

export default BalanceOverview