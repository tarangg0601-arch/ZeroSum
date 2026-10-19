import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { rupeesToPaise } from '../../utils/currency.js'

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`
const now = new Date()
const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

function Recurring() {
  const { groupId } = useParams()
  const [group, setGroup] = useState(null)
  const [rules, setRules] = useState([])
  const [form, setForm] = useState({ description: '', amount: '', category: '', frequency: 'MONTHLY', nextRunDate: today, paidBy: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    const [groupResponse, ruleResponse] = await Promise.all([
      fetch(`${API_BASE_URL}/groups/${groupId}`),
      fetch(`${API_BASE_URL}/groups/${groupId}/recurring`),
    ])
    if (!groupResponse.ok || !ruleResponse.ok) throw new Error('Unable to load recurring expenses.')
    const groupData = await groupResponse.json()
    setGroup(groupData)
    setRules(await ruleResponse.json())
    setForm(current => ({ ...current, paidBy: current.paidBy || groupData.members?.[0]?.userId || '' }))
  }, [groupId])

  useEffect(() => {
    // This async request updates the view only after the API response arrives.
    // oxlint-disable-next-line react/set-state-in-effect
    load().catch(err => setError(err.message))
  }, [load])

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const response = await fetch(`${API_BASE_URL}/groups/${groupId}/recurring`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          description: form.description,
          amountPaise: rupeesToPaise(form.amount),
          category: form.category,
          frequency: form.frequency,
          nextRunDate: form.nextRunDate,
          paidBy: Number(form.paidBy),
        }),
      })
      const data = await response.json()
      if (!response.ok) throw new Error(data.error || data.message || 'Unable to create recurring expense.')
      setForm(current => ({ ...current, description: '', amount: '', category: '' }))
      await load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const toggle = async (rule) => {
    setError('')
    try {
      const response = await fetch(`${API_BASE_URL}/groups/${groupId}/recurring/${rule.id}`, {
        method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ active: !rule.active }),
      })
      if (!response.ok) throw new Error('Unable to update recurring rule.')
      await load()
    } catch (err) { setError(err.message) }
  }

  const money = paise => `₹${(paise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

  if (!group && !error) return <div className="rounded-2xl bg-white p-10 text-center text-sm text-slate-500">Loading recurring rules…</div>

  const categoryOptions = group?.groupType === 'hostel'
    ? ['Rent', 'Mess', 'Electricity', 'Wi-Fi', 'Groceries', 'Maintenance', 'Supplies']
    : group?.groupType === 'trip'
      ? ['Transport', 'Hotel', 'Food', 'Tickets', 'Activities', 'Other']
      : ['Home', 'Food', 'Utilities', 'Other']

  return <div className="mx-auto max-w-[1200px]">
    <Link to={`/groups/${groupId}`} className="text-sm font-medium text-emerald-700 hover:text-emerald-800">← Back to {group?.name || 'group'}</Link>
    <div className="mb-6 mt-5">
      <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">{group?.groupType} group</p>
      <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">Recurring expenses</h1>
      <p className="mt-2 text-sm text-slate-500">Rules create equal-split expenses on their due date. Repeated scheduler runs do not duplicate an occurrence.</p>
    </div>

    {error && <p role="alert" className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{error}</p>}

    <div className="grid gap-5 lg:grid-cols-[0.9fr_1.1fr]">
      <form onSubmit={submit} className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="font-semibold text-slate-900">Add a rule</h2>
        <label className="block text-sm text-slate-600">Description<input required maxLength="255" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" placeholder="Monthly rent" /></label>
        <div className="grid grid-cols-2 gap-3">
          <label className="block text-sm text-slate-600">Amount (₹)<input required min="0.01" step="0.01" type="number" value={form.amount} onChange={e => setForm({ ...form, amount: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>
          <label className="block text-sm text-slate-600">Category<select value={form.category} onChange={e => setForm({ ...form, category: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2"><option value="">Uncategorized</option>{categoryOptions.map(item => <option key={item} value={item}>{item}</option>)}</select></label>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <label className="block text-sm text-slate-600">Frequency<select value={form.frequency} onChange={e => setForm({ ...form, frequency: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2"><option value="DAILY">Daily</option><option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option><option value="YEARLY">Yearly</option></select></label>
          <label className="block text-sm text-slate-600">Next due<input required type="date" min={today} value={form.nextRunDate} onChange={e => setForm({ ...form, nextRunDate: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>
        </div>
        <label className="block text-sm text-slate-600">Paid by<select required value={form.paidBy} onChange={e => setForm({ ...form, paidBy: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2">{group?.members?.map(member => <option key={member.userId} value={member.userId}>{member.name}</option>)}</select></label>
        <button disabled={busy || !form.paidBy} className="w-full rounded-lg bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{busy ? 'Saving…' : 'Create recurring rule'}</button>
      </form>

      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="font-semibold text-slate-900">Rules <span className="font-normal text-slate-400">({rules.length})</span></h2>
        {rules.length === 0 ? <p className="mt-6 rounded-xl bg-slate-50 p-5 text-sm text-slate-500">No recurring expenses yet.</p> : <ul className="mt-4 divide-y divide-slate-100">{rules.map(rule => <li key={rule.id} className="flex items-center gap-4 py-4">
          <div className="min-w-0 flex-1"><p className="font-medium text-slate-800">{rule.description}</p><p className="mt-1 text-xs text-slate-500">{money(rule.amountPaise)} · {rule.frequency.toLowerCase()} · next due {rule.nextRunDate}</p><p className="mt-1 text-xs text-slate-400">{rule.category || 'Uncategorized'} · paid by {group?.members?.find(member => member.userId === rule.paidBy)?.name || 'group member'}</p></div>
          <button onClick={() => toggle(rule)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-50">{rule.active ? 'Pause' : 'Resume'}</button>
        </li>)}</ul>}
      </section>
    </div>
  </div>
}

export default Recurring
