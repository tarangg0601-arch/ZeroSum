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

import {
  Link,
  useSearchParams,
} from 'react-router-dom'

import {
  useEffect,
  useMemo,
  useState,
} from 'react'

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`

function Balances() {
  const [searchParams] = useSearchParams()

  /*
   * The current Group Details page opens /balances.
   * We support ?groupId=2 immediately, while keeping /balances
   * working for the current test group.
   *
   * Later, we will make Group Details pass its actual group ID.
   */
  const groupId = searchParams.get('groupId') || '2'

  const [group, setGroup] = useState(null)
  const [settlement, setSettlement] = useState(null)
  const [rawPaymentCount, setRawPaymentCount] = useState(0)
  const [paymentHistory, setPaymentHistory] = useState([])
  const [recordingPayment, setRecordingPayment] = useState(false)
  const [paymentError, setPaymentError] = useState('')
  const [refresh, setRefresh] = useState(0)

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [simulation, setSimulation] = useState(null)
  const [simulationError, setSimulationError] = useState('')
  const [payment, setPayment] = useState({ fromUserId: '', toUserId: '', amount: '' })
  const [simulating, setSimulating] = useState(false)

  useEffect(() => {
    async function loadSettlement() {
      try {
        setLoading(true)
        setError('')

        const [groupResponse, settlementResponse, expensesResponse, historyResponse] =
          await Promise.all([
            fetch(`${API_BASE_URL}/groups/${groupId}`),
            fetch(`${API_BASE_URL}/groups/${groupId}/settlement`),
            fetch(`${API_BASE_URL}/groups/${groupId}/expenses`),
            fetch(`${API_BASE_URL}/groups/${groupId}/settlement/history`),
          ])

        if (!groupResponse.ok) {
          throw new Error('Failed to load group')
        }

        if (!settlementResponse.ok) {
          throw new Error('Failed to load settlement')
        }
        if (!expensesResponse.ok) throw new Error('Failed to load expenses')
        if (!historyResponse.ok) throw new Error('Failed to load payment history')

        const groupData = await groupResponse.json()
        const settlementData = await settlementResponse.json()
        const expenses = await expensesResponse.json()
        const history = await historyResponse.json()
        setRawPaymentCount(expenses.reduce((count, expense) => count + expense.shares.filter(share => share.userId !== expense.paidBy).length, 0))
        setPaymentHistory(history)

        setGroup(groupData)
        setSettlement(settlementData)
      } catch (err) {
        console.error(err)
        setError(
          err.message || 'Unable to load settlement data.',
        )
      } finally {
        setLoading(false)
      }
    }

    loadSettlement()
  }, [groupId, refresh])

  const balances = useMemo(() => settlement?.balances || [], [settlement])
  const transactions = useMemo(() => settlement?.transactions || [], [settlement])
  const explanationSteps =
    settlement?.explanation?.steps || []

  /*
   * For the current MVP there is no authentication yet.
   * The group creator is therefore treated as the current user.
   * This matches our current backend model.
   */
  const currentUserId = group?.createdBy

  const currentUserBalance = useMemo(() => {
    return (
      balances.find(
        (balance) =>
          balance.userId === currentUserId,
      )?.balancePaise || 0
    )
  }, [balances, currentUserId])

  const activeBalances = balances.filter(
    (balance) => balance.balancePaise !== 0,
  )
  const debtors = balances.filter(balance => balance.balancePaise < 0)
  const creditors = balances.filter(balance => balance.balancePaise > 0)
  const graphHeight = Math.max(140, Math.max(debtors.length, creditors.length) * 76 + 36)
  const debtorY = new Map(debtors.map((balance, index) => [balance.userId, 34 + index * 76]))
  const creditorY = new Map(creditors.map((balance, index) => [balance.userId, 34 + index * 76]))

  const runSimulation = async (event) => {
    event.preventDefault()
    setSimulating(true)
    setSimulationError('')
    try {
      const response = await fetch(`${API_BASE_URL}/groups/${groupId}/settlement/whatif`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fromUserId: Number(payment.fromUserId),
          toUserId: Number(payment.toUserId),
          amountPaise: Math.round(Number(payment.amount) * 100),
        }),
      })
      const data = await response.json()
      if (!response.ok) throw new Error(data.error || 'Unable to simulate payment')
      setSimulation(data)
    } catch (err) {
      setSimulationError(err.message)
      setSimulation(null)
    } finally {
      setSimulating(false)
    }
  }

  const recordPayment = async transaction => {
    setRecordingPayment(true)
    setPaymentError('')
    try {
      const response = await fetch(`${API_BASE_URL}/groups/${groupId}/settlement/payments`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fromUserId: transaction.fromUserId, toUserId: transaction.toUserId, amountPaise: transaction.amountPaise }),
      })
      const data = await response.json()
      if (!response.ok) throw new Error(data.error || 'Unable to record payment.')
      setRefresh(value => value + 1)
    } catch (err) { setPaymentError(err.message) } finally { setRecordingPayment(false) }
  }

  const formatRupees = (paise) => {
    const rupees = Math.abs(paise) / 100

    return `₹${rupees.toLocaleString('en-IN', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    })}`
  }

  const formatSignedRupees = (paise) => {
    if (paise > 0) {
      return `+${formatRupees(paise)}`
    }

    if (paise < 0) {
      return `-${formatRupees(paise)}`
    }

    return '₹0'
  }

  const getInitials = (name) => {
    if (!name) {
      return '?'
    }

    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase()
  }

  const currentUserIsOwed =
    currentUserBalance > 0

  const currentUserOwes =
    currentUserBalance < 0

  if (loading) {
    return (
      <div className="mx-auto max-w-[1400px]">

        <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">

          <p className="text-sm text-slate-500">
            Loading balances and settlement...
          </p>

        </div>

      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-[1400px]">

        <Link
          to={`/groups/${groupId}`}
          className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-emerald-600"
        >
          <ArrowLeft size={17} />
          Back to group
        </Link>

        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-10 text-center">

          <p className="font-semibold text-rose-600">
            Unable to load settlement.
          </p>

          <p className="mt-2 text-sm text-rose-400">
            {error}
          </p>

        </div>

      </div>
    )
  }

  return (
    <div className="mx-auto max-w-[1400px]">

      {/* Back */}

      <Link
        to={`/groups/${groupId}`}
        className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-emerald-600"
      >
        <ArrowLeft size={17} />

        Back to {group?.name || 'group'}
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

          <CheckCircle2
            size={17}
            className="text-emerald-600"
          />

          <span className="text-sm font-semibold text-emerald-700">
            {transactions.length > 0
              ? 'Ready to settle'
              : 'Already settled'}
          </span>

        </div>

      </section>


      {/* Summary */}

      <section className="mb-6 grid gap-4 md:grid-cols-3">

        <SummaryCard
          icon={<WalletCards size={19} />}
          label="Your net balance"
          value={formatSignedRupees(
            currentUserBalance,
          )}
          positive={currentUserBalance > 0}
          negative={currentUserBalance < 0}
          description={
            currentUserIsOwed
              ? 'You should receive'
              : currentUserOwes
                ? 'You need to pay'
                : 'You are settled'
          }
        />


        <SummaryCard
          icon={<Users size={19} />}
          label="Members with balances"
          value={String(activeBalances.length)}
          description={
            activeBalances.length === balances.length
              ? 'All members included'
              : `${balances.length - activeBalances.length} settled`
          }
        />


        <SummaryCard
          icon={<CircleDollarSign size={19} />}
          label="Optimized payments"
          value={String(transactions.length)}
          description={
            transactions.length === 1
              ? 'Payment to settle group'
              : 'Payments to settle group'
          }
        />

      </section>

      <section className="mb-6 grid gap-5 xl:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">Debt graph</p>
          <h2 className="mt-2 font-semibold text-slate-900">Who pays whom</h2>
          <p className="mt-1 text-sm text-slate-500">Raw expense debts reduced from {rawPaymentCount} to {transactions.length} planned payments.</p>
          {transactions.length === 0 ? <p className="mt-4 rounded-xl bg-slate-50 p-4 text-sm text-slate-500">No debt edges. Everyone is settled.</p> : <div className="mt-4 overflow-x-auto">
            <svg role="img" aria-labelledby="debt-graph-title debt-graph-description" viewBox={`0 0 600 ${graphHeight}`} className="min-w-[500px] w-full">
              <title id="debt-graph-title">Optimized settlement debt graph</title>
              <desc id="debt-graph-description">Arrows show who pays whom and the amount of each planned payment.</desc>
              <defs><marker id="debt-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 z" fill="#10b981" /></marker></defs>
              {transactions.map((item, index) => {
                const y1 = debtorY.get(item.fromUserId) + 15
                const y2 = creditorY.get(item.toUserId) + 15
                return <g key={`${item.fromUserId}-${item.toUserId}-${index}`}>
                  <path d={`M 188 ${y1} C 270 ${y1}, 330 ${y2}, 412 ${y2}`} fill="none" stroke="#10b981" strokeWidth="2" markerEnd="url(#debt-arrow)" />
                  <text x="300" y={(y1 + y2) / 2 - 5} textAnchor="middle" className="fill-slate-700 text-[12px] font-semibold">{formatRupees(item.amountPaise)}</text>
                </g>
              })}
              {debtors.map((balance, index) => <g key={balance.userId}>
                <circle cx="172" cy={49 + index * 76} r="17" fill="#f1f5f9" />
                <text x="172" y={53 + index * 76} textAnchor="middle" className="fill-slate-600 text-[10px] font-bold">{getInitials(balance.userName)}</text>
                <text x="20" y={53 + index * 76} className="fill-slate-700 text-[12px]">{balance.userName}</text>
              </g>)}
              {creditors.map((balance, index) => <g key={balance.userId}>
                <circle cx="428" cy={49 + index * 76} r="17" fill="#d1fae5" />
                <text x="428" y={53 + index * 76} textAnchor="middle" className="fill-emerald-700 text-[10px] font-bold">{getInitials(balance.userName)}</text>
                <text x="454" y={53 + index * 76} className="fill-slate-700 text-[12px]">{balance.userName}</text>
              </g>)}
            </svg>
            <div className="flex justify-between px-4 text-[11px] font-medium uppercase tracking-wide text-slate-400"><span>Owes</span><span>Receives</span></div>
          </div>}
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">What-if simulator</p>
          <h2 className="mt-2 font-semibold text-slate-900">Preview a payment</h2>
          <p className="mt-1 text-sm text-slate-500">Recalculates the plan in memory. It does not record a payment.</p>
          <form onSubmit={runSimulation} className="mt-4 grid gap-3 sm:grid-cols-3">
            <select required aria-label="Payer" value={payment.fromUserId} onChange={e => setPayment({ ...payment, fromUserId: e.target.value })} className="rounded-lg border border-slate-200 px-3 py-2 text-sm">
              <option value="">Payer</option>{balances.filter(b => b.balancePaise < 0).map(b => <option key={b.userId} value={b.userId}>{b.userName}</option>)}
            </select>
            <select required aria-label="Recipient" value={payment.toUserId} onChange={e => setPayment({ ...payment, toUserId: e.target.value })} className="rounded-lg border border-slate-200 px-3 py-2 text-sm">
              <option value="">Recipient</option>{balances.filter(b => b.balancePaise > 0).map(b => <option key={b.userId} value={b.userId}>{b.userName}</option>)}
            </select>
            <input required aria-label="Amount in rupees" type="number" min="0.01" step="0.01" placeholder="Amount ₹" value={payment.amount} onChange={e => setPayment({ ...payment, amount: e.target.value })} className="rounded-lg border border-slate-200 px-3 py-2 text-sm" />
            <button disabled={simulating} className="rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{simulating ? 'Calculating…' : 'Simulate'}</button>
          </form>
          {simulationError && <p role="alert" className="mt-3 text-sm text-rose-600">{simulationError}</p>}
          {simulation && <div className="mt-4 rounded-xl border border-emerald-100 bg-emerald-50 p-4">
            <p className="text-sm font-semibold text-emerald-800">Preview: {simulation.transactions.length} payment{simulation.transactions.length === 1 ? '' : 's'} would remain</p>
            <ul className="mt-2 space-y-1 text-sm text-emerald-900">{simulation.transactions.map((item, index) => <li key={`${item.fromUserId}-${item.toUserId}-${index}`}>{item.fromUserName} pays {item.toUserName} {formatRupees(item.amountPaise)}</li>)}</ul>
          </div>}
        </div>
      </section>


      {/* Main settlement card */}

      {paymentError && <p role="alert" className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{paymentError}</p>}

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
            {transactions.length}{' '}
            {transactions.length === 1
              ? 'transaction'
              : 'transactions'}
          </div>

        </div>


        {transactions.length === 0 ? (

          <div className="px-6 py-12 text-center">

            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
              <CheckCircle2 size={22} />
            </div>

            <h3 className="mt-4 font-semibold text-slate-900">
              Everyone is settled
            </h3>

            <p className="mt-1 text-sm text-slate-400">
              No payments are required.
            </p>

          </div>

        ) : (

          <div className="divide-y divide-slate-100">

            {transactions.map(
              (transaction, index) => (

                <div
                  key={`${transaction.fromUserId}-${transaction.toUserId}-${index}`}
                  className="flex flex-col gap-4 px-6 py-5 sm:flex-row sm:items-center"
                >

                  {/* Step number */}

                  <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-emerald-50 text-xs font-bold text-emerald-600">
                    {index + 1}
                  </div>


                  {/* From */}

                  <div className="flex min-w-[150px] items-center gap-3">

                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-600">
                      {getInitials(
                        transaction.fromUserName,
                      )}
                    </div>

                    <div>

                      <p className="text-sm font-semibold text-slate-800">
                        {transaction.fromUserName}
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
                      {getInitials(
                        transaction.toUserName,
                      )}
                    </div>

                    <div>

                      <p className="text-sm font-semibold text-slate-800">
                        {transaction.toUserName}
                      </p>

                      <p className="text-xs text-emerald-500">
                        Receives
                      </p>

                    </div>

                  </div>


                  {/* Amount */}

                  <div className="sm:ml-auto sm:text-right">

                    <p className="text-lg font-bold text-slate-900">
                      {formatRupees(
                        transaction.amountPaise,
                      )}
                    </p>

                    <p className="text-xs text-slate-400">
                      payment
                    </p>
                    <button type="button" disabled={recordingPayment} onClick={() => recordPayment(transaction)} className="mt-2 rounded-lg border border-emerald-200 px-3 py-1.5 text-xs font-semibold text-emerald-700 hover:bg-emerald-50 disabled:opacity-50">Record paid</button>

                  </div>

                </div>

              ),
            )}

          </div>

        )}

      </section>


      {/* Balances + explanation */}

      <section className="mb-6 rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 px-6 py-5"><h2 className="font-semibold text-slate-900">Payment history</h2><p className="mt-1 text-xs text-slate-400">Recorded transfers reduce current balances.</p></div>
        {paymentHistory.length === 0 ? <p className="px-6 py-6 text-sm text-slate-500">No payments recorded yet.</p> : <ul className="divide-y divide-slate-100">{paymentHistory.map(item => <li key={item.id} className="flex flex-wrap items-center gap-3 px-6 py-4 text-sm"><span className="font-medium text-slate-700">{item.fromUserName} paid {item.toUserName}</span><span className="ml-auto font-semibold text-slate-900">{formatRupees(item.amountPaise)}</span><span className="text-xs text-slate-400">{item.settledAt ? new Date(item.settledAt).toLocaleDateString() : item.status}</span></li>)}</ul>}
      </section>

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

            {balances.map((balance) => {

              const positive =
                balance.balancePaise > 0

              const negative =
                balance.balancePaise < 0

              let status = 'Settled'

              if (positive) {
                status =
                  balance.userId === currentUserId
                    ? 'You are owed'
                    : 'Gets back'
              }

              if (negative) {
                status =
                  balance.userId === currentUserId
                    ? 'You owe'
                    : 'Needs to pay'
              }

              return (

                <div
                  key={balance.userId}
                  className="flex items-center gap-3 px-6 py-4"
                >

                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-full text-xs font-semibold ${
                      positive
                        ? 'bg-emerald-50 text-emerald-700'
                        : negative
                          ? 'bg-slate-100 text-slate-600'
                          : 'bg-slate-100 text-slate-500'
                    }`}
                  >
                    {getInitials(balance.userName)}
                  </div>


                  <div className="min-w-0 flex-1">

                    <p className="text-sm font-semibold text-slate-800">
                      {balance.userName}
                    </p>

                    <p
                      className={`mt-0.5 text-xs ${
                        positive
                          ? 'text-emerald-500'
                          : negative
                            ? 'text-rose-400'
                            : 'text-slate-400'
                      }`}
                    >
                      {status}
                    </p>

                  </div>


                  <div
                    className={`text-sm font-bold ${
                      positive
                        ? 'text-emerald-600'
                        : negative
                          ? 'text-rose-500'
                          : 'text-slate-500'
                    }`}
                  >
                    {formatSignedRupees(
                      balance.balancePaise,
                    )}
                  </div>

                </div>

              )
            })}

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

            {explanationSteps.length > 0 ? (

              <div className="space-y-5">

                {explanationSteps.map(
                  (step, index) => (

                    <div
                      key={`${step}-${index}`}
                      className="flex gap-3"
                    >

                      <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-emerald-100 text-[11px] font-bold text-emerald-700">
                        {index + 1}
                      </div>

                      <p className="pt-0.5 text-sm leading-6 text-slate-600">
                        {step}
                      </p>

                    </div>

                  ),
                )}

              </div>

            ) : (

              <p className="text-sm leading-6 text-slate-500">
                ZeroSum has no additional settlement explanation for this group.
              </p>

            )}


            <div className="mt-6 rounded-xl border border-emerald-100 bg-white p-4">

              <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
                Result
              </p>

              <p className="mt-2 text-sm font-medium leading-6 text-slate-700">

                The group can be settled using only{' '}

                <span className="font-bold text-slate-900">
                  {transactions.length}{' '}
                  {transactions.length === 1
                    ? 'payment'
                    : 'payments'}
                </span>
                .

              </p>


              <Link
                to={`/groups/${groupId}`}
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
  negative = false,
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
                : negative
                  ? 'text-rose-500'
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
