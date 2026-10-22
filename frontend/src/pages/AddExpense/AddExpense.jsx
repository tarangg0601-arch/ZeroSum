import {
  ArrowLeft,
  CalendarDays,
  Check,
  ChevronDown,
  Receipt,
  Utensils,
  Car,
  ShoppingBag,
  Home,
  Plane,
  MoreHorizontal,
} from 'lucide-react'

import {
  Link,
  useNavigate,
  useSearchParams,
} from 'react-router-dom'

import {
  useEffect,
  useMemo,
  useState,
} from 'react'
import { createSplitPayload } from '../../utils/expenseSplits.js'
import { rupeesToPaise } from '../../utils/currency.js'
import { localDateString } from '../../utils/date.js'
import { apiRequest } from '../../api/client.js'

const categories = [
  {
    name: 'Food',
    icon: Utensils,
  },
  {
    name: 'Transport',
    icon: Car,
  },
  {
    name: 'Shopping',
    icon: ShoppingBag,
  },
  {
    name: 'Home',
    icon: Home,
  },
  {
    name: 'Travel',
    icon: Plane,
  },
  {
    name: 'Other',
    icon: MoreHorizontal,
  },
]

const modeCategories = {
  hostel: ['Rent', 'Mess', 'Electricity', 'Groceries', 'Maintenance', 'Wi-Fi', 'Supplies'],
  trip: ['Transport', 'Hotel', 'Food', 'Tickets', 'Activities', 'Other'],
}

function AddExpense() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const groupId = searchParams.get('groupId')
  const expenseId = searchParams.get('expenseId')

  const [group, setGroup] = useState(null)
  const [availableGroups, setAvailableGroups] = useState([])
  const [members, setMembers] = useState([])

  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [paidBy, setPaidBy] = useState('')
  const [category, setCategory] = useState('Food')
  const [date, setDate] = useState(
    localDateString(),
  )

  const [selectedMembers, setSelectedMembers] = useState([])
  const [splitType, setSplitType] = useState('equal')
  const [splitValues, setSplitValues] = useState({})

  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    async function loadGroup() {
      if (!groupId) {
        try {
          const groups = await apiRequest('/groups')
          setAvailableGroups(groups)
        } catch (err) {
          setError(err.message || 'Unable to load your groups.')
        } finally { setLoading(false) }
        return
      }

      try {
        setLoading(true)
        setError('')

        const data = await apiRequest(`/groups/${groupId}`)

        setGroup(data)

        const groupMembers = data.members || []

        setMembers(groupMembers)

        if (groupMembers.length > 0) {
          setPaidBy(groupMembers[0].userId)

          setSelectedMembers(
            groupMembers.map((member) => member.userId),
          )
        }
        if (expenseId) {
          const expense = await apiRequest(`/groups/${groupId}/expenses/${expenseId}`)
          setDescription(expense.description)
          setAmount((expense.amountPaise / 100).toFixed(2))
          setCategory(expense.category || 'Other')
          setDate(expense.expenseDate)
          setPaidBy(expense.paidBy)
          setSelectedMembers(expense.shares.map(share => share.userId))
          setSplitType('exact')
          setSplitValues(Object.fromEntries(expense.shares.map(share => [share.userId, (share.shareAmountPaise / 100).toFixed(2)])))
        }
      } catch (err) {
        console.error(err)
        setError('Unable to load group details.')
      } finally {
        setLoading(false)
      }
    }

    loadGroup()
  }, [groupId, expenseId])

  const numericAmount = Number(amount) || 0
  const availableCategories = (modeCategories[group?.groupType] || categories.map(item => item.name))
    .map(name => categories.find(item => item.name === name) || { name, icon: MoreHorizontal })

  const splitAmount = useMemo(() => {
    if (selectedMembers.length === 0) {
      return 0
    }

    return numericAmount / selectedMembers.length
  }, [numericAmount, selectedMembers.length])

  function toggleMember(memberId) {
    setSelectedMembers((current) => {
      if (current.includes(memberId)) {
        return current.filter((id) => id !== memberId)
      }

      return [...current, memberId]
    })
  }

  function selectAllMembers() {
    setSelectedMembers(
      members.map((member) => member.userId),
    )
  }

  function clearMembers() {
    setSelectedMembers([])
  }

  async function handleSubmit(event) {
    event.preventDefault()

    if (!groupId) {
      setError('No group selected.')
      return
    }

    if (!description.trim()) {
      setError('Please enter an expense description.')
      return
    }

    if (numericAmount <= 0) {
      setError('Please enter a valid expense amount.')
      return
    }

    if (!paidBy) {
      setError('Please select who paid for the expense.')
      return
    }

    if (selectedMembers.length === 0) {
      setError('Select at least one participant.')
      return
    }

    try {
      setSubmitting(true)
      setError('')
      setSuccess('')

      const amountPaise = rupeesToPaise(amount)

      const payload = {
        description: description.trim(),
        amountPaise,
        category,
        expenseDate: date,
        paidBy: Number(paidBy),
        participantIds: selectedMembers.map(Number),
        ...createSplitPayload(amountPaise, splitType, selectedMembers, splitValues),
      }

      await apiRequest(expenseId ? `/groups/${groupId}/expenses/${expenseId}` : `/groups/${groupId}/expenses`, {
          method: expenseId ? 'PUT' : 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(payload),
        })

      setSuccess(expenseId ? 'Expense updated successfully.' : 'Expense added successfully.')

      setTimeout(() => {
        navigate(`/groups/${groupId}`)
      }, 500)
    } catch (err) {
      console.error(err)
      setError(err.message || 'Unable to add expense.')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-[1100px]">
        <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center shadow-sm">
          <p className="text-sm text-slate-500">
            Loading group details...
          </p>
        </div>
      </div>
    )
  }

  if (!groupId) {
    return <div className="mx-auto max-w-3xl"><Link to="/groups" className="inline-flex items-center gap-2 text-sm text-slate-500"><ArrowLeft size={16} />Back to groups</Link><h1 className="mt-4 text-3xl font-bold text-slate-900">Choose a group</h1><p className="mt-2 text-sm text-slate-500">Expenses always belong to one of your groups.</p>{error && <p role="alert" className="mt-4 text-sm text-rose-600">{error}</p>}{availableGroups.length ? <ul className="mt-5 divide-y divide-slate-100 rounded-2xl border border-slate-200 bg-white px-5">{availableGroups.map(item => <li key={item.id}><Link className="block py-4 font-semibold text-slate-800 hover:text-emerald-700" to={`/expenses/new?groupId=${item.id}`}>{item.name}<span className="ml-2 text-xs font-normal text-slate-500">{item.members?.length || 0} members</span></Link></li>)}</ul> : !error && <div className="mt-5 rounded-2xl border border-dashed border-slate-300 bg-white p-8 text-center"><p className="text-sm text-slate-600">Create or join a group before adding an expense.</p><Link className="mt-3 inline-flex text-sm font-semibold text-emerald-700" to="/groups">Open groups</Link></div>}</div>
  }

  if (error && !group) {
    return (
      <div className="mx-auto max-w-[1100px]">

        <Link
          to="/groups"
          className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-emerald-600"
        >
          <ArrowLeft size={17} />
          Back to groups
        </Link>

        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-10 text-center">
          <p className="font-semibold text-rose-600">
            {error}
          </p>

          <p className="mt-2 text-sm text-rose-400">
            Open this page from a specific group before adding an expense.
          </p>
        </div>

      </div>
    )
  }

  const groupType = group?.groupType
    ? group.groupType.charAt(0).toUpperCase() +
      group.groupType.slice(1)
    : 'Group'

  const paidByMember = members.find(
    (member) => member.userId === Number(paidBy),
  )

  return (
    <div className="mx-auto max-w-[1100px]">

      {/* Back */}

      <Link
        to={`/groups/${groupId}`}
        className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-emerald-600"
      >
        <ArrowLeft size={17} />

        Back to {group?.name}
      </Link>


      {/* Page heading */}

      <section className="mb-8">

        <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
          {groupType}
        </p>

        <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
          {expenseId ? 'Edit expense' : 'Add an expense'}
        </h1>

        <p className="mt-2 text-sm text-slate-500">
          Record a shared expense and let ZeroSum calculate everyone's share.
        </p>

      </section>


      {/* Error / success messages */}

      {error && (
        <div className="mb-5 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-medium text-rose-600">
          {error}
        </div>
      )}

      {success && (
        <div className="mb-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-600">
          {success}
        </div>
      )}


      <form onSubmit={handleSubmit}>

        <div className="grid gap-6 lg:grid-cols-[1fr_360px]">


          {/* Main form */}

          <div className="space-y-6">


            {/* Basic information */}

            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">

              <div className="mb-6 flex items-center gap-3">

                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
                  <Receipt size={19} />
                </div>

                <div>

                  <h2 className="font-semibold text-slate-900">
                    Expense details
                  </h2>

                  <p className="text-xs text-slate-400">
                    Tell us what the expense was for.
                  </p>

                </div>

              </div>


              <div className="space-y-5">


                {/* Description */}

                <div>

                  <label className="mb-2 block text-sm font-medium text-slate-700">
                    Description
                  </label>

                  <input
                    type="text"
                    value={description}
                    onChange={(event) =>
                      setDescription(event.target.value)
                    }
                    placeholder="e.g. Dinner at the beach"
                    className="h-11 w-full rounded-xl border border-slate-200 bg-white px-4 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/5"
                  />

                </div>


                {/* Amount + date */}

                <div className="grid gap-5 sm:grid-cols-2">


                  <div>

                    <label className="mb-2 block text-sm font-medium text-slate-700">
                      Amount
                    </label>

                    <div className="relative">

                      <span className="absolute left-4 top-1/2 -translate-y-1/2 text-sm font-semibold text-slate-400">
                        ₹
                      </span>

                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={amount}
                        onChange={(event) =>
                          setAmount(event.target.value)
                        }
                        placeholder="0.00"
                        className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-4 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/5"
                      />

                    </div>

                  </div>


                  <div>

                    <label className="mb-2 block text-sm font-medium text-slate-700">
                      Date
                    </label>

                    <div className="relative">

                      <CalendarDays
                        size={17}
                        className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400"
                      />

                      <input
                        type="date"
                        value={date}
                        onChange={(event) =>
                          setDate(event.target.value)
                        }
                        className="h-11 w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 text-sm text-slate-700 outline-none transition focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/5"
                      />

                    </div>

                  </div>

                </div>


                {/* Paid by */}

                <div>

                  <label className="mb-2 block text-sm font-medium text-slate-700">
                    Paid by
                  </label>

                  <div className="relative">

                    <select
                      value={paidBy}
                      onChange={(event) =>
                        setPaidBy(Number(event.target.value))
                      }
                      className="h-11 w-full appearance-none rounded-xl border border-slate-200 bg-white px-4 pr-10 text-sm text-slate-700 outline-none transition focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/5"
                    >

                      {members.map((member) => (
                        <option
                          key={member.userId}
                          value={member.userId}
                        >
                          {member.name}
                        </option>
                      ))}

                    </select>

                    <ChevronDown
                      size={17}
                      className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400"
                    />

                  </div>

                </div>

              </div>

            </section>


            {/* Category */}

            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">

              <div className="mb-5">

                <h2 className="font-semibold text-slate-900">
                  Category
                </h2>

                <p className="mt-1 text-xs text-slate-400">
                  Choose a category for this expense.
                </p>

              </div>

              <div className="grid grid-cols-3 gap-3 sm:grid-cols-6">

                {availableCategories.map((item) => {

                  const Icon = item.icon
                  const isSelected = category === item.name

                  return (

                    <button
                      key={item.name}
                      type="button"
                      onClick={() => setCategory(item.name)}
                      className={`flex flex-col items-center gap-2 rounded-xl border p-3 transition ${
                        isSelected
                          ? 'border-emerald-300 bg-emerald-50 text-emerald-600'
                          : 'border-slate-200 bg-white text-slate-400 hover:border-emerald-200 hover:bg-emerald-50/40'
                      }`}
                    >

                      <Icon size={19} />

                      <span className="text-xs font-medium">
                        {item.name}
                      </span>

                    </button>

                  )
                })}

              </div>

            </section>


            {/* Split */}

            <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">

              <div className="mb-5 flex items-start justify-between gap-4">

                <div>

                  <h2 className="font-semibold text-slate-900">Split expense</h2>

                  <p className="mt-1 text-xs text-slate-400">
                    Select everyone who participated in this expense.
                  </p>

                </div>

                <label className="mb-4 block max-w-xs text-xs font-medium text-slate-600">
                  Split type
                  <select value={splitType} onChange={event => setSplitType(event.target.value)} className="mt-1 block h-10 w-full rounded-lg border border-slate-200 bg-white px-3 text-sm text-slate-700">
                    <option value="equal">Equal</option>
                    <option value="percentage">Percentage</option>
                    <option value="exact">Exact amount</option>
                    <option value="shares">Shares</option>
                  </select>
                </label>

                <div className="flex gap-2">

                  <button
                    type="button"
                    onClick={selectAllMembers}
                    className="text-xs font-semibold text-emerald-600 hover:text-emerald-700"
                  >
                    Select all
                  </button>

                  <span className="text-xs text-slate-300">
                    /
                  </span>

                  <button
                    type="button"
                    onClick={clearMembers}
                    className="text-xs font-semibold text-slate-400 hover:text-slate-600"
                  >
                    Clear
                  </button>

                </div>

              </div>


              <div className="space-y-2">

                {members.map((member) => {

                  const selected =
                    selectedMembers.includes(member.userId)

                  const initials = member.name
                    .split(' ')
                    .map((part) => part[0])
                    .join('')
                    .slice(0, 2)
                    .toUpperCase()

                  return (

                    <button
                      key={member.userId}
                      type="button"
                      onClick={() =>
                        toggleMember(member.userId)
                      }
                      className={`flex w-full items-center gap-3 rounded-xl border p-3 text-left transition ${
                        selected
                          ? 'border-emerald-200 bg-emerald-50/50'
                          : 'border-slate-200 bg-white hover:bg-slate-50'
                      }`}
                    >

                      <div className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-600">
                        {initials}
                      </div>

                      <span className="flex-1 text-sm font-medium text-slate-700">
                        {member.name}
                      </span>

                      <div
                        className={`flex h-6 w-6 items-center justify-center rounded-full border transition ${
                          selected
                            ? 'border-emerald-500 bg-emerald-500 text-white'
                            : 'border-slate-300 text-transparent'
                        }`}
                      >
                        <Check size={14} strokeWidth={3} />
                      </div>

                    </button>

                  )
                })}

              </div>

              {splitType !== 'equal' && <div className="mt-4 space-y-2 border-t border-slate-100 pt-4">
                {members.filter(member => selectedMembers.includes(member.userId)).map(member => <label key={member.userId} className="flex items-center justify-between gap-3 text-sm text-slate-600">
                  <span>{member.name} {splitType === 'percentage' ? '(%)' : splitType === 'exact' ? '(₹)' : '(units)'}</span>
                  <input type="number" min="0" step={splitType === 'shares' ? '1' : '0.01'} aria-label={`${splitType} for ${member.name}`} value={splitValues[member.userId] ?? ''} onChange={event => setSplitValues(current => ({ ...current, [member.userId]: event.target.value }))} className="w-28 rounded-lg border border-slate-200 px-3 py-2 text-right text-sm" />
                </label>)}
              </div>}

            </section>

          </div>


          {/* Summary */}

          <aside className="lg:sticky lg:top-6 lg:self-start">

            <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">

              <div className="bg-[#071410] p-6 text-white">

                <p className="text-xs font-semibold uppercase tracking-widest text-emerald-300">
                  Expense summary
                </p>

                <p className="mt-4 text-3xl font-bold tracking-tight">
                  ₹
                  {numericAmount.toLocaleString('en-IN', {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </p>

                <p className="mt-1 text-sm text-slate-400">
                  {description || 'Unnamed expense'}
                </p>

              </div>


              <div className="p-6">

                <div className="space-y-4">

                  <div className="flex items-center justify-between">

                    <span className="text-sm text-slate-500">
                      Paid by
                    </span>

                    <span className="text-sm font-semibold text-slate-800">
                      {paidByMember?.name || '—'}
                    </span>

                  </div>


                  <div className="flex items-center justify-between">

                    <span className="text-sm text-slate-500">
                      Category
                    </span>

                    <span className="text-sm font-semibold text-slate-800">
                      {category}
                    </span>

                  </div>


                  <div className="flex items-center justify-between">

                    <span className="text-sm text-slate-500">
                      Participants
                    </span>

                    <span className="text-sm font-semibold text-slate-800">
                      {selectedMembers.length}
                    </span>

                  </div>

                </div>


                <div className="my-5 border-t border-slate-100" />


                <div className="rounded-xl bg-emerald-50 p-4">

                  <p className="text-xs font-medium text-emerald-700">
                    {splitType === 'equal' ? 'Equal share' : 'Selected participants'}
                  </p>

                  <p className="mt-1 text-2xl font-bold text-emerald-700">
                  {splitType === 'equal' ? '₹' : ''}
                  {(splitType === 'equal' ? splitAmount : selectedMembers.reduce((sum, id) => sum + Number(splitValues[id] || 0), 0)).toLocaleString('en-IN', {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </p>

                  <p className="mt-1 text-xs text-emerald-600/70">
                    {splitType === 'equal' ? 'Per participant' : splitType === 'percentage' ? 'Total percentage (must equal 100)' : splitType === 'exact' ? 'Allocated rupees (must equal expense total)' : 'Total share units'}
                  </p>

                </div>


                <button
                  type="submit"
                  disabled={
                    submitting ||
                    !description.trim() ||
                    numericAmount <= 0 ||
                    selectedMembers.length === 0 ||
                    !paidBy
                  }
                  className="mt-5 flex h-11 w-full items-center justify-center rounded-xl bg-emerald-500 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {submitting
                    ? expenseId ? 'Saving changes…' : 'Adding expense…'
                    : expenseId ? 'Save changes' : 'Add expense'}
                </button>


                <p className="mt-3 text-center text-[11px] leading-5 text-slate-400">
                  ZeroSum will automatically calculate the resulting balances.
                </p>

              </div>

            </section>

          </aside>

        </div>

      </form>

    </div>
  )
}

export default AddExpense
