import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiRequest } from '../../api/client.js'

const amount = value => `₹${(Math.abs(value) / 100).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

export default function Activity({ expensesOnly = false }) {
  const [state, setState] = useState({ loading: true, error: '', entries: [] })
  useEffect(() => {
    let active = true
    Promise.all([apiRequest('/groups'), apiRequest('/users/me')]).then(async ([groups]) => {
      const rows = await Promise.all(groups.map(async group => {
        const requests = [apiRequest(`/groups/${group.id}/expenses`)]
        if (!expensesOnly) requests.push(apiRequest(`/groups/${group.id}/settlement/history`))
        const [expenses, payments = []] = await Promise.all(requests)
        return [
          ...expenses.map(item => ({ id: `expense-${group.id}-${item.id}`, title: item.description, detail: `${group.name} · expense · ${item.expenseDate}`, value: item.amountPaise, date: item.expenseDate, groupId: group.id })),
          ...payments.map(item => ({ id: `payment-${group.id}-${item.id}`, title: `${item.fromUserName} paid ${item.toUserName}`, detail: `${group.name} · recorded payment`, value: item.amountPaise, date: item.settledAt, groupId: group.id })),
        ]
      }))
      if (active) setState({ loading: false, error: '', entries: rows.flat().sort((a, b) => String(b.date || '').localeCompare(String(a.date || ''))) })
    }).catch(error => { if (active) setState({ loading: false, error: error.message || 'Could not load activity.', entries: [] }) })
    return () => { active = false }
  }, [expensesOnly])
  const title = expensesOnly ? 'Expenses' : 'Activity'
  return <div className="mx-auto max-w-5xl"><p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">Workspace</p><h1 className="mt-2 text-3xl font-bold text-slate-900">{title}</h1><p className="mt-2 text-sm text-slate-500">{expensesOnly ? 'Expenses across groups you belong to.' : 'Recorded expenses and payments from your groups.'}</p>
    {state.loading ? <p role="status" className="mt-6 rounded-2xl bg-white p-6 text-slate-500">Loading {title.toLowerCase()}…</p> : state.error ? <p role="alert" className="mt-6 rounded-2xl bg-rose-50 p-6 text-rose-700">{state.error}</p> : state.entries.length === 0 ? <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center"><h2 className="font-semibold">No {title.toLowerCase()} yet</h2><p className="mt-2 text-sm text-slate-500">Add an expense inside one of your groups to get started.</p><Link className="mt-4 inline-flex text-sm font-semibold text-emerald-700" to="/expenses/new">Add expense</Link></div> : <ul className="mt-6 divide-y divide-slate-100 rounded-2xl border border-slate-200 bg-white px-5">{state.entries.map(item => <li key={item.id} className="flex flex-wrap items-center gap-3 py-4"><div className="min-w-0 flex-1"><p className="truncate font-medium text-slate-800">{item.title}</p><p className="text-xs text-slate-500">{item.detail}</p></div><span className="font-semibold text-slate-900">{amount(item.value)}</span><Link className="text-xs text-emerald-700" to={`/groups/${item.groupId}`}>Open group</Link></li>)}</ul>}
  </div>
}
