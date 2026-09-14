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
import { Link } from 'react-router-dom'
import { useMemo, useState } from 'react'

const members = [
  {
    id: 1,
    name: 'Tarang',
    initials: 'TG',
  },
  {
    id: 2,
    name: 'Priya',
    initials: 'PR',
  },
  {
    id: 3,
    name: 'Dev',
    initials: 'DV',
  },
  {
    id: 4,
    name: 'Aria',
    initials: 'AR',
  },
  {
    id: 5,
    name: 'Karan',
    initials: 'KA',
  },
]

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

function AddExpense() {
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [paidBy, setPaidBy] = useState(1)
  const [category, setCategory] = useState('Food')
  const [date, setDate] = useState('2026-09-14')
  const [selectedMembers, setSelectedMembers] = useState(
    members.map((member) => member.id),
  )

  const numericAmount = Number(amount) || 0

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
    setSelectedMembers(members.map((member) => member.id))
  }

  function clearMembers() {
    setSelectedMembers([])
  }

  function handleSubmit(event) {
    event.preventDefault()

    console.log({
      description,
      amount: numericAmount,
      paidBy,
      category,
      date,
      selectedMembers,
    })
  }

  return (
    <div className="mx-auto max-w-[1100px]">

      {/* Back */}
      <Link
        to="/groups/1"
        className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 transition hover:text-emerald-600"
      >
        <ArrowLeft size={17} />
        Back to Bali trip
      </Link>

      {/* Page heading */}
      <section className="mb-8">
        <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
          Bali trip
        </p>

        <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
          Add an expense
        </h1>

        <p className="mt-2 text-sm text-slate-500">
          Record a shared expense and let ZeroSum calculate everyone&apos;s share.
        </p>
      </section>

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
                    onChange={(event) => setDescription(event.target.value)}
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
                        onChange={(event) => setAmount(event.target.value)}
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
                        onChange={(event) => setDate(event.target.value)}
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
                      onChange={(event) => setPaidBy(Number(event.target.value))}
                      className="h-11 w-full appearance-none rounded-xl border border-slate-200 bg-white px-4 pr-10 text-sm text-slate-700 outline-none transition focus:border-emerald-400 focus:ring-4 focus:ring-emerald-500/5"
                    >
                      {members.map((member) => (
                        <option key={member.id} value={member.id}>
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

                {categories.map((item) => {
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
                  <h2 className="font-semibold text-slate-900">
                    Split equally
                  </h2>

                  <p className="mt-1 text-xs text-slate-400">
                    Select everyone who participated in this expense.
                  </p>
                </div>

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
                  const selected = selectedMembers.includes(member.id)

                  return (
                    <button
                      key={member.id}
                      type="button"
                      onClick={() => toggleMember(member.id)}
                      className={`flex w-full items-center gap-3 rounded-xl border p-3 text-left transition ${
                        selected
                          ? 'border-emerald-200 bg-emerald-50/50'
                          : 'border-slate-200 bg-white hover:bg-slate-50'
                      }`}
                    >

                      <div className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-100 text-xs font-semibold text-slate-600">
                        {member.initials}
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
                  ₹{numericAmount.toLocaleString('en-IN', {
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
                      {members.find((member) => member.id === paidBy)?.name}
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
                    Equal share
                  </p>

                  <p className="mt-1 text-2xl font-bold text-emerald-700">
                    ₹{splitAmount.toLocaleString('en-IN', {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </p>

                  <p className="mt-1 text-xs text-emerald-600/70">
                    Per participant
                  </p>

                </div>

                <button
                  type="submit"
                  disabled={!description || numericAmount <= 0 || selectedMembers.length === 0}
                  className="mt-5 flex h-11 w-full items-center justify-center rounded-xl bg-emerald-500 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  Add expense
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