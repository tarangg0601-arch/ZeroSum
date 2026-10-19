import {
  ArrowLeft,
  ArrowRight,
  Copy,
  Plus,
  Receipt,
  Users,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`

// Temporary current user for MVP.
// Authentication will replace this later.
const CURRENT_USER_ID = 2

function GroupDetails() {
  const { groupId } = useParams()

  const [group, setGroup] = useState(null)
  const [expenses, setExpenses] = useState([])
  const [balances, setBalances] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadGroupData() {
      try {
        setLoading(true)
        setError('')

        const [groupResponse, expensesResponse, balancesResponse] =
          await Promise.all([
            fetch(`${API_BASE_URL}/groups/${groupId}`),
            fetch(`${API_BASE_URL}/groups/${groupId}/expenses`),
            fetch(`${API_BASE_URL}/groups/${groupId}/balances`),
          ])

        if (!groupResponse.ok) {
          throw new Error('Failed to load group')
        }

        if (!expensesResponse.ok) {
          throw new Error('Failed to load expenses')
        }

        if (!balancesResponse.ok) {
          throw new Error('Failed to load balances')
        }

        const [groupData, expenseData, balanceData] =
          await Promise.all([
            groupResponse.json(),
            expensesResponse.json(),
            balancesResponse.json(),
          ])

        setGroup(groupData)
        setExpenses(expenseData)
        setBalances(balanceData)
      } catch (err) {
        console.error(err)
        setError('Unable to load group details.')
      } finally {
        setLoading(false)
      }
    }

    loadGroupData()
  }, [groupId])

  const totalExpensesPaise = useMemo(() => {
    return expenses.reduce(
      (total, expense) =>
        total + Number(expense.amountPaise || 0),
      0,
    )
  }, [expenses])

  const currentUserBalance = useMemo(() => {
    const currentUser = balances.find(
      (balance) => balance.userId === CURRENT_USER_ID,
    )

    return currentUser?.balancePaise ?? 0
  }, [balances])

  const formatRupees = (paise) => {
    const amount = Number(paise || 0) / 100

    return `₹${amount.toLocaleString('en-IN', {
      maximumFractionDigits: 2,
    })}`
  }

  const formatSignedRupees = (paise) => {
    const amount = Number(paise || 0)

    if (amount > 0) {
      return `+${formatRupees(amount)}`
    }

    if (amount < 0) {
      return `-${formatRupees(Math.abs(amount))}`
    }

    return '₹0'
  }

  const formatDate = (date) => {
    if (!date) return ''

    const parsedDate = new Date(date)

    if (Number.isNaN(parsedDate.getTime())) {
      return date
    }

    return parsedDate.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
    })
  }

  const getInitials = (name) => {
    if (!name) return '?'

    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .slice(0, 2)
      .toUpperCase()
  }

  const getBalanceStatus = (memberBalance, memberId) => {
    if (memberBalance > 0) {
      return memberId === CURRENT_USER_ID
        ? 'You are owed'
        : 'Owed'
    }

    if (memberBalance < 0) {
      return memberId === CURRENT_USER_ID
        ? 'You owe'
        : 'Owes'
    }

    return 'Settled'
  }

  const copyJoinCode = async () => {
    if (!group?.joinCode) return

    try {
      await navigator.clipboard.writeText(group.joinCode)
    } catch (err) {
      console.error('Unable to copy join code', err)
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-[1400px]">
        <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
          <p className="text-sm text-slate-500">
            Loading group details...
          </p>
        </div>
      </div>
    )
  }

  if (error || !group) {
    return (
      <div className="mx-auto max-w-[1400px]">
        <Link
          to="/groups"
          className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-emerald-600"
        >
          <ArrowLeft size={17} />
          Back to groups
        </Link>

        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-10 text-center">
          <p className="font-semibold text-rose-600">
            {error || 'Group not found.'}
          </p>

          <p className="mt-2 text-sm text-rose-400">
            Make sure the Spring Boot backend is running on port 8080.
          </p>
        </div>
      </div>
    )
  }

  const groupType = group.groupType
    ? group.groupType.charAt(0).toUpperCase() +
      group.groupType.slice(1)
    : 'Group'

  return (
    <div className="mx-auto max-w-[1400px]">

      {/* Back */}
      <Link
        to="/groups"
        className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-emerald-600"
      >
        <ArrowLeft size={17} />
        Back to groups
      </Link>

      {/* Group header */}
      <section className="mb-7 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">

        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">

          <div className="flex items-center gap-4">

            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-600">
              <Users size={25} />
            </div>

            <div>

              <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
                {groupType}
              </p>

              <h1 className="mt-1 text-2xl font-bold tracking-tight text-slate-900">
                {group.name}
              </h1>

              <p className="mt-1 text-sm text-slate-400">
                {group.members.length}{' '}
                {group.members.length === 1
                  ? 'member'
                  : 'members'}{' '}
                · Active group
              </p>

            </div>

          </div>

          <div className="flex flex-wrap gap-3">

            <button
              type="button"
              className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
            >
              <Users size={17} />
              Invite
            </button>

            <Link
              to={`/expenses/new?groupId=${group.id}`}
              className="flex items-center gap-2 rounded-xl bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-600"
            >
              <Plus size={17} />
              Add expense
            </Link>

          </div>

        </div>

      </section>

      {/* Summary cards */}
      <section className="mb-6 grid gap-4 md:grid-cols-3">

        <SummaryCard
          label="Total expenses"
          value={formatRupees(totalExpensesPaise)}
          icon={<Receipt size={19} />}
        />

        <SummaryCard
          label="Members"
          value={group.members.length}
          icon={<Users size={19} />}
        />

        <SummaryCard
          label="Your balance"
          value={formatSignedRupees(currentUserBalance)}
          icon={<ArrowRight size={19} />}
          positive={currentUserBalance > 0}
        />

      </section>

      {/* Main content */}
      <div className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">

        {/* Expenses */}
        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">

          <div className="flex items-center justify-between border-b border-slate-100 px-6 py-5">

            <div>

              <h2 className="font-semibold text-slate-900">
                Expenses
              </h2>

              <p className="mt-1 text-xs text-slate-400">
                Recent shared expenses
              </p>

            </div>

            <span className="text-sm text-slate-400">
              {expenses.length}{' '}
              {expenses.length === 1
                ? 'expense'
                : 'expenses'}
            </span>

          </div>

          {expenses.length === 0 ? (

            <div className="p-10 text-center">

              <Receipt
                size={28}
                className="mx-auto text-slate-300"
              />

              <p className="mt-3 text-sm font-medium text-slate-600">
                No expenses yet
              </p>

              <p className="mt-1 text-xs text-slate-400">
                Add an expense to start tracking this group.
              </p>

            </div>

          ) : (

            <div className="divide-y divide-slate-100">

              {expenses.map((expense) => (

                <div
                  key={expense.id}
                  className="flex items-center gap-4 px-6 py-5 transition hover:bg-slate-50/70"
                >

                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
                    <Receipt size={18} />
                  </div>

                  <div className="min-w-0 flex-1">

                    <p className="truncate text-sm font-semibold text-slate-800">
                      {expense.description}
                    </p>

                    <div className="mt-1 flex items-center gap-2 text-xs text-slate-400">

                      <span>
                        {expense.category || 'General'}
                      </span>

                      <span>•</span>

                      <span>
                        Paid by {expense.paidByName || 'Unknown'}
                      </span>

                    </div>

                  </div>

                  <div className="text-right">

                    <p className="text-sm font-semibold text-slate-800">
                      {formatRupees(expense.amountPaise)}
                    </p>

                    <p className="mt-1 text-xs text-slate-400">
                      {formatDate(expense.expenseDate)}
                    </p>

                  </div>

                </div>

              ))}

            </div>

          )}

        </section>

        {/* Members */}
        <section className="rounded-2xl border border-slate-200 bg-white shadow-sm">

          <div className="border-b border-slate-100 px-6 py-5">

            <h2 className="font-semibold text-slate-900">
              Members
            </h2>

            <p className="mt-1 text-xs text-slate-400">
              Current balances
            </p>

          </div>

          <div className="divide-y divide-slate-100">

            {group.members.map((member) => {

              const memberBalance =
                balances.find(
                  (balance) =>
                    balance.userId === member.userId,
                )?.balancePaise ?? 0

              const positive = memberBalance > 0
              const negative = memberBalance < 0

              return (

                <div
                  key={member.userId}
                  className="flex items-center gap-3 px-6 py-4"
                >

                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-50 text-xs font-semibold text-emerald-700">
                    {getInitials(member.name)}
                  </div>

                  <div className="min-w-0 flex-1">

                    <p className="text-sm font-medium text-slate-800">
                      {member.name}
                    </p>

                    <p className="mt-0.5 text-[11px] text-slate-400">
                      {getBalanceStatus(
                        memberBalance,
                        member.userId,
                      )}
                    </p>

                  </div>

                  <p
                    className={`text-sm font-semibold ${
                      positive
                        ? 'text-emerald-600'
                        : negative
                          ? 'text-rose-500'
                          : 'text-slate-400'
                    }`}
                  >
                    {formatSignedRupees(memberBalance)}
                  </p>

                </div>

              )
            })}

          </div>

        </section>

      </div>

      {/* Settlement section */}
      <section className="mt-5 overflow-hidden rounded-2xl border border-emerald-200 bg-emerald-50/50">

        <div className="flex flex-col gap-5 p-6 sm:flex-row sm:items-center sm:justify-between">

          <div>

            <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
              Ready to settle
            </p>

            <h2 className="mt-2 text-lg font-semibold text-slate-900">
              ZeroSum has optimized your group balances.
            </h2>

            <p className="mt-1 text-sm text-slate-500">
              See who should pay whom and settle with fewer transactions.
            </p>

          </div>

          <Link
            to={`/balances?groupId=${group.id}`}
            className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-[#071410] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#0d211a]"
          >
            View settlement
            <ArrowRight size={17} />
          </Link>

          <Link to={`/groups/${group.id}/recurring`} className="inline-flex shrink-0 items-center justify-center rounded-xl border border-emerald-200 bg-white px-5 py-3 text-sm font-semibold text-emerald-700 hover:bg-emerald-50">
            Recurring expenses
          </Link>

        </div>

      </section>

      {/* Join code */}
      <section className="mt-5 flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:flex-row sm:items-center sm:justify-between">

        <div className="flex items-center gap-3">

          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-100 text-slate-500">
            <Users size={18} />
          </div>

          <div>

            <p className="text-xs text-slate-400">
              Group join code
            </p>

            <p className="mt-0.5 font-mono text-sm font-semibold tracking-wider text-slate-800">
              {group.joinCode}
            </p>

          </div>

        </div>

        <button
          type="button"
          onClick={copyJoinCode}
          className="flex items-center justify-center gap-2 rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
        >
          <Copy size={16} />
          Copy code
        </button>

      </section>

    </div>
  )
}

function SummaryCard({
  label,
  value,
  icon,
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

        </div>

      </div>

    </div>
  )
}

export default GroupDetails
