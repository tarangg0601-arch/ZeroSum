import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`

function Insights() {
  const { groupId } = useParams()
  const [group, setGroup] = useState(null)
  const [data, setData] = useState(null)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)

  useEffect(() => {
    let current = true
    const query = new URLSearchParams()
    if (from) query.set('from', from)
    if (to) query.set('to', to)
    Promise.all([fetch(`${API_BASE_URL}/groups/${groupId}`), fetch(`${API_BASE_URL}/groups/${groupId}/insights?${query}`)])
      .then(async ([groupResponse, insightResponse]) => {
        if (!groupResponse.ok || !insightResponse.ok) throw new Error('Unable to load group insights.')
        const [groupInfo, insights] = await Promise.all([groupResponse.json(), insightResponse.json()])
        if (current) { setGroup(groupInfo); setData(insights); setError('') }
      })
      .catch(err => { if (current) setError(err.message) })
    return () => { current = false }
  }, [groupId, from, to, refresh])

  const money = paise => `₹${(paise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
  const largest = Math.max(1, ...(data?.categories || []).map(item => item.amountPaise))

  return <div className="mx-auto max-w-[1100px]">
    <Link to={`/groups/${groupId}`} className="text-sm font-medium text-emerald-700">← Back to {group?.name || 'group'}</Link>
    <header className="mb-6 mt-5"><p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">Group insights</p><h1 className="mt-2 text-3xl font-bold text-slate-900">Spending overview</h1><p className="mt-2 text-sm text-slate-500">Totals are calculated from recorded expenses for this group.</p></header>
    <form className="mb-5 flex flex-wrap items-end gap-3 rounded-2xl border border-slate-200 bg-white p-5" onSubmit={event => { event.preventDefault(); setRefresh(value => value + 1) }}>
      <label className="text-xs font-medium text-slate-600">From<input type="date" value={from} onChange={event => setFrom(event.target.value)} className="mt-1 block rounded-lg border border-slate-200 px-3 py-2 text-sm" /></label>
      <label className="text-xs font-medium text-slate-600">To<input type="date" value={to} onChange={event => setTo(event.target.value)} className="mt-1 block rounded-lg border border-slate-200 px-3 py-2 text-sm" /></label>
      <button className="rounded-lg bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white">Apply dates</button>
    </form>
    {error && <p role="alert" className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{error}</p>}
    {data && <>
      <div className="mb-5 grid gap-4 sm:grid-cols-2"><Summary label="Total expenses" value={money(data.totalAmountPaise)} /><Summary label="Expense count" value={String(data.expenseCount)} /></div>
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"><h2 className="font-semibold text-slate-900">By category</h2>
        {data.categories.length === 0 ? <p className="mt-5 rounded-xl bg-slate-50 p-5 text-sm text-slate-500">No expenses in this date range.</p> : <div className="mt-5 space-y-5">{data.categories.map(item => <div key={item.category}>
          <div className="mb-2 flex justify-between gap-4 text-sm"><span className="font-medium text-slate-700">{item.category} <span className="font-normal text-slate-400">· {item.expenseCount}</span></span><span className="font-semibold text-slate-900">{money(item.amountPaise)}</span></div>
          <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-emerald-500" style={{ width: `${Math.max(2, item.amountPaise / largest * 100)}%` }} /></div>
        </div>)}</div>}
      </section>
    </>}
  </div>
}

function Summary({ label, value }) { return <div className="rounded-2xl border border-slate-200 bg-white p-5"><p className="text-xs text-slate-500">{label}</p><p className="mt-2 text-2xl font-bold text-slate-900">{value}</p></div> }
export default Insights
