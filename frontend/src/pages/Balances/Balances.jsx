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

const API_BASE_URL = 'http://localhost:8080/api'

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

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadSettlement() {
      try {
        setLoading(true)
        setError('')

        const [groupResponse, settlementResponse] =
          await Promise.all([
            fetch(`${API_BASE_URL}/groups/${groupId}`),
            fetch(`${API_BASE_URL}/groups/${groupId}/settlement`),
          ])

        if (!groupResponse.ok) {
          throw new Error('Failed to load group')
        }

        if (!settlementResponse.ok) {
          throw new Error('Failed to load settlement')
        }

        const groupData = await groupResponse.json()
        const settlementData = await settlementResponse.json()

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
  }, [groupId])

  const balances = settlement?.balances || []
  const transactions = settlement?.transactions || []
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

                  </div>

                </div>

              ),
            )}

          </div>

        )}

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

            {balances.map((balance) => {

              const positive =
                balance.balancePaise > 0

              const negative =
                balance.balancePaise < 0

              const zero =
                balance.balancePaise === 0

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