import {
  ArrowLeft,
  ArrowRight,
  Copy,
  Plus,
  Receipt,
  Users,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { apiRequest } from '../../api/client.js'

function GroupDetails() {
  const { groupId } = useParams()
  const navigate = useNavigate()

  const [group, setGroup] = useState(null)
  const [expenses, setExpenses] = useState([])
  const [balances, setBalances] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showInvite, setShowInvite] = useState(false)
  const [inviteUsername, setInviteUsername] = useState('')
  const [inviteError, setInviteError] = useState('')
  const [inviting, setInviting] = useState(false)
  const [currentUserId, setCurrentUserId] = useState(null)
  const [copyMessage, setCopyMessage] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [showEditGroup, setShowEditGroup] = useState(false)
  const [groupForm, setGroupForm] = useState({ name: '', groupType: 'general' })

  useEffect(() => {
    async function loadGroupData() {
      try {
        setLoading(true)
        setError('')

        const [groupData, expenseData, balanceData, currentUser] = await Promise.all([
          apiRequest(`/groups/${groupId}`), apiRequest(`/groups/${groupId}/expenses`),
          apiRequest(`/groups/${groupId}/balances`), apiRequest('/users/me'),
        ])

        setGroup(groupData)
        setExpenses(expenseData)
        setBalances(balanceData)
        setCurrentUserId(currentUser.userId)
      } catch (err) {
        console.error(err)
        setError('Unable to load group details.')
      } finally {
        setLoading(false)
      }
    }

    loadGroupData()
  }, [groupId, refreshKey])

  const totalExpensesPaise = useMemo(() => {
    return expenses.reduce(
      (total, expense) =>
        total + Number(expense.amountPaise || 0),
      0,
    )
  }, [expenses])

  const currentUserBalance = useMemo(() => {
    const currentUser = balances.find(
      (balance) => balance.userId === currentUserId,
    )

    return currentUser?.balancePaise ?? 0
  }, [balances, currentUserId])

  async function handleInvite(event) {
    event.preventDefault()
    setInviting(true)
    setInviteError('')
    try {
      await apiRequest(`/groups/${groupId}/members`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: inviteUsername }),
      })
      const [groupData, balanceData] = await Promise.all([apiRequest(`/groups/${groupId}`), apiRequest(`/groups/${groupId}/balances`)] )
      setGroup(groupData)
      setBalances(balanceData)
      setInviteUsername('')
      setShowInvite(false)
    } catch (err) { setInviteError(err.message) } finally { setInviting(false) }
  }

  async function handleDeleteExpense(expense) {
    if (!window.confirm(`Delete “${expense.description}”? This cannot be undone.`)) return
    setError('')
    try {
      await apiRequest(`/groups/${groupId}/expenses/${expense.id}`, { method: 'DELETE' })
      setRefreshKey(value => value + 1)
    } catch (err) {
      setError(err.message || 'Could not delete this expense.')
    }
  }

  async function handleUpdateGroup(event) {
    event.preventDefault()
    setError('')
    try {
      await apiRequest(`/groups/${groupId}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(groupForm) })
      setShowEditGroup(false)
      setRefreshKey(value => value + 1)
    } catch (err) { setError(err.message || 'Could not update the group.') }
  }

  async function handleDeleteGroup() {
    if (!window.confirm(`Delete “${group.name}”? This removes its expenses and cannot be undone.`)) return
    setError('')
    try {
      await apiRequest(`/groups/${groupId}`, { method: 'DELETE' })
      navigate('/groups')
    } catch (err) { setError(err.message || 'Could not delete the group.') }
  }

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
      return memberId === currentUserId
        ? 'You are owed'
        : 'Owed'
    }

    if (memberBalance < 0) {
      return memberId === currentUserId
        ? 'You owe'
        : 'Owes'
    }

    return 'Settled'
  }

  const canManageMembers = group?.createdBy === currentUserId

  const copyJoinCode = async () => {
    if (!group?.joinCode) return

    try {
      await navigator.clipboard.writeText(group.joinCode)
      setCopyMessage('Join code copied.')
    } catch {
      setCopyMessage('Could not copy. Select and copy the code manually.')
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

            {canManageMembers && <>
              <button type="button" onClick={() => { setGroupForm({ name: group.name, groupType: group.groupType }); setShowEditGroup(true) }} className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-600">Edit group</button>
              <button type="button" onClick={handleDeleteGroup} className="rounded-xl border border-rose-200 px-4 py-2.5 text-sm font-semibold text-rose-700">Delete group</button>
            </>}

            {canManageMembers && <button
              type="button"
              onClick={() => { setInviteError(''); setShowInvite(true) }}
              className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-600 transition hover:bg-slate-50"
            >
              <Users size={17} />
              Add member
            </button>}

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
                  className="flex flex-wrap items-center gap-4 px-6 py-5 transition hover:bg-slate-50/70"
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

                  <div className="flex gap-2 sm:ml-2">
                    <Link aria-label={`Edit ${expense.description}`} to={`/expenses/new?groupId=${groupId}&expenseId=${expense.id}`} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-medium text-slate-600">Edit</Link>
                    <button type="button" onClick={() => handleDeleteExpense(expense)} className="rounded-lg border border-rose-200 px-3 py-2 text-xs font-medium text-rose-700">Delete</button>
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

          <Link to={`/groups/${group.id}/insights`} className="inline-flex shrink-0 items-center justify-center rounded-xl border border-emerald-200 bg-white px-5 py-3 text-sm font-semibold text-emerald-700 hover:bg-emerald-50">Insights</Link>
          <Link to={`/groups/${group.id}/ious`} className="inline-flex shrink-0 items-center justify-center rounded-xl border border-emerald-200 bg-white px-5 py-3 text-sm font-semibold text-emerald-700 hover:bg-emerald-50">IOUs</Link>

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
        {copyMessage && <p role="status" className="text-xs text-emerald-700">{copyMessage}</p>}

      </section>

      {showInvite && canManageMembers && <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setShowInvite(false) }}>
        <form onSubmit={handleInvite} className="w-full max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-xl">
          <div><h2 className="text-xl font-semibold text-slate-900">Add an existing account</h2><p className="mt-1 text-sm text-slate-500">This adds the registered account directly.</p></div>
          {inviteError && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{inviteError}</p>}
          <label className="block text-sm text-slate-600">Registered username<input required minLength={3} maxLength={32} pattern="[A-Za-z0-9_]{3,32}" autoFocus autoComplete="username" value={inviteUsername} onChange={event => setInviteUsername(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>
          <div className="flex justify-end gap-2"><button type="button" onClick={() => setShowInvite(false)} className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600">Cancel</button><button disabled={inviting} className="rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{inviting ? 'Adding…' : 'Add member'}</button></div>
        </form>
      </div>}
      {showEditGroup && canManageMembers && <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setShowEditGroup(false) }}><form onSubmit={handleUpdateGroup} className="w-full max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-xl"><h2 className="text-xl font-semibold text-slate-900">Edit group</h2><label className="block text-sm text-slate-600">Group name<input required maxLength="150" value={groupForm.name} onChange={event => setGroupForm({ ...groupForm, name: event.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label><label className="block text-sm text-slate-600">Group mode<select value={groupForm.groupType} onChange={event => setGroupForm({ ...groupForm, groupType: event.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2"><option value="general">General</option><option value="hostel">Hostel</option><option value="trip">Trip</option></select></label><div className="flex justify-end gap-2"><button type="button" onClick={() => setShowEditGroup(false)} className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600">Cancel</button><button className="rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-white">Save group</button></div></form></div>}
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
