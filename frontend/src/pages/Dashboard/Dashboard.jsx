import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../../api/client.js'

const rupees = paise => `₹${(Math.abs(paise) / 100).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

export default function Dashboard() {
  const [state, setState] = useState({ loading: true, error: '', user: null, groups: [], expenses: [], expenseCount: 0, transactions: [], net: 0 })

  useEffect(() => {
    let active = true
    async function load() {
      try {
        const [user, groups] = await Promise.all([apiRequest('/users/me'), apiRequest('/groups')])
        const details = await Promise.all(groups.map(async group => {
          const [balances, expenses, settlement] = await Promise.all([
            apiRequest(`/groups/${group.id}/balances`), apiRequest(`/groups/${group.id}/expenses`), apiRequest(`/groups/${group.id}/settlement`),
          ])
          return { group, balances, expenses, transactions: settlement.transactions || [] }
        }))
        if (!active) return
        const net = details.reduce((sum, item) => sum + (item.balances.find(balance => balance.userId === user.userId)?.balancePaise || 0), 0)
        const expensesWithGroup = details.flatMap(item => item.expenses.map(expense => ({ ...expense, groupName: item.group.name })))
          .sort((a, b) => (b.expenseDate || '').localeCompare(a.expenseDate || ''))
        setState({ loading: false, error: '', user, groups: details.map(item => item.group), expenses: expensesWithGroup.slice(0, 6), expenseCount: expensesWithGroup.length, transactions: details.flatMap(item => item.transactions.map(transaction => ({ ...transaction, groupId: item.group.id, groupName: item.group.name }))).slice(0, 5), net })
      } catch (error) {
        if (active) setState(current => ({ ...current, loading: false, error: error.message || 'Could not load your dashboard.' }))
      }
    }
    load()
    return () => { active = false }
  }, [])

  if (state.loading) return <p role="status" className="rounded-2xl bg-white p-8 text-slate-500">Loading your dashboard…</p>
  if (state.error) return <div role="alert" className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-rose-700"><p>{state.error}</p><button className="mt-3 underline" onClick={() => window.location.reload()}>Try again</button></div>

  const firstName = state.user?.name?.trim().split(/\s+/)[0] || 'there'
  return <div className="mx-auto max-w-[1400px]">
    <section className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div><p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">Overview</p><h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">Welcome, {firstName}</h1><p className="mt-2 text-sm text-slate-500">Your shared expenses and settlement plans.</p></div>
      <Link to="/expenses/new" className="rounded-xl bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white">Add expense</Link>
    </section>
    <section className="mb-5 grid gap-4 sm:grid-cols-3">
      <Metric label="Your net balance" value={`${state.net > 0 ? '+' : state.net < 0 ? '−' : ''}${rupees(state.net)}`} detail={state.net > 0 ? 'You are owed overall' : state.net < 0 ? 'You owe overall' : 'You are settled'} />
      <Metric label="Your groups" value={state.groups.length} detail="Groups you belong to" />
      <Metric label="Recent expenses" value={state.expenseCount} detail="Across your groups" />
    </section>
    {state.groups.length === 0 ? <section className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center"><h2 className="font-semibold text-slate-900">Start with a group</h2><p className="mt-2 text-sm text-slate-500">Create a group or join one with a code to track shared expenses.</p><Link to="/groups" className="mt-4 inline-flex rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-white">Open groups</Link></section> : <div className="grid gap-5 xl:grid-cols-2">
      <section className="rounded-2xl border border-slate-200 bg-white p-5"><div className="mb-4 flex items-center justify-between"><h2 className="font-semibold text-slate-900">Your groups</h2><Link className="text-sm text-emerald-700" to="/groups">View all</Link></div><ul className="divide-y divide-slate-100">{state.groups.map(group => <li key={group.id} className="py-3"><Link to={`/groups/${group.id}`} className="font-medium text-slate-800 hover:text-emerald-700">{group.name}</Link><p className="text-xs text-slate-500">{group.members?.length || 0} members · {group.groupType}</p></li>)}</ul></section>
      <section className="rounded-2xl border border-slate-200 bg-white p-5"><h2 className="mb-4 font-semibold text-slate-900">Recent expenses</h2>{state.expenses.length ? <ul className="divide-y divide-slate-100">{state.expenses.map(expense => <li key={expense.id} className="flex justify-between gap-4 py-3"><div className="min-w-0"><p className="truncate font-medium text-slate-800">{expense.description}</p><p className="text-xs text-slate-500">{expense.groupName} · {expense.expenseDate}</p></div><span className="shrink-0 text-sm font-semibold">{rupees(expense.amountPaise)}</span></li>)}</ul> : <p className="text-sm text-slate-500">No expenses recorded yet.</p>}</section>
      <section className="rounded-2xl border border-slate-200 bg-white p-5 xl:col-span-2"><h2 className="mb-4 font-semibold text-slate-900">Suggested settlements</h2>{state.transactions.length ? <ul className="grid gap-3 md:grid-cols-2">{state.transactions.map((item, index) => <li key={`${item.groupId}-${item.fromUserId}-${item.toUserId}-${index}`} className="rounded-xl bg-slate-50 p-4 text-sm"><Link to={`/balances?groupId=${item.groupId}`} className="font-medium text-slate-700">{item.fromUserName} pays {item.toUserName} {rupees(item.amountPaise)}</Link><p className="mt-1 text-xs text-slate-500">{item.groupName}</p></li>)}</ul> : <p className="text-sm text-slate-500">No payments are currently needed.</p>}</section>
    </div>}
  </div>
}

function Metric({ label, value, detail }) {
  return <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><p className="text-xs text-slate-500">{label}</p><p className="mt-2 text-2xl font-semibold text-slate-900">{value}</p><p className="mt-1 text-xs text-slate-400">{detail}</p></div>
}
