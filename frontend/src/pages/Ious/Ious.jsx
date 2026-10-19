import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { rupeesToPaise } from '../../utils/currency.js'

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`

function Ious() {
  const { groupId } = useParams()
  const [group, setGroup] = useState(null)
  const [ious, setIous] = useState([])
  const [form, setForm] = useState({ fromUserId: '', toUserId: '', amount: '', note: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const reload = useCallback(async () => {
    const [groupResponse, iouResponse] = await Promise.all([fetch(`${API_BASE_URL}/groups/${groupId}`), fetch(`${API_BASE_URL}/groups/${groupId}/ious`)])
    if (!groupResponse.ok || !iouResponse.ok) throw new Error('Unable to load IOUs.')
    const groupData = await groupResponse.json()
    setGroup(groupData)
    setIous(await iouResponse.json())
    setForm(current => ({ ...current, fromUserId: current.fromUserId || String(groupData.members?.[0]?.userId || ''), toUserId: current.toUserId || String(groupData.members?.[1]?.userId || '') }))
  }, [groupId])

  useEffect(() => {
    // This async request synchronizes group and IOU records into the view.
    // oxlint-disable-next-line react/set-state-in-effect
    reload().catch(err => setError(err.message))
  }, [reload])

  const create = async event => {
    event.preventDefault(); setBusy(true); setError('')
    try {
      const response = await fetch(`${API_BASE_URL}/ious`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ fromUserId: Number(form.fromUserId), toUserId: Number(form.toUserId), amountPaise: rupeesToPaise(form.amount), note: form.note, groupId: Number(groupId) }) })
      const data = await response.json()
      if (!response.ok) throw new Error(data.error || 'Unable to add IOU.')
      setForm(current => ({ ...current, amount: '', note: '' })); await reload()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  const settle = async id => {
    try {
      const response = await fetch(`${API_BASE_URL}/ious/${id}`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ settled: true }) })
      if (!response.ok) throw new Error('Unable to update IOU.')
      await reload()
    } catch (err) { setError(err.message) }
  }
  const money = paise => `₹${(paise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

  return <div className="mx-auto max-w-[1100px]">
    <Link to={`/groups/${groupId}`} className="text-sm font-medium text-emerald-700">← Back to {group?.name || 'group'}</Link>
    <header className="mb-6 mt-5"><p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">Direct loans</p><h1 className="mt-2 text-3xl font-bold text-slate-900">IOUs</h1><p className="mt-2 text-sm text-slate-500">Track a direct loan without changing shared expense balances.</p></header>
    {error && <p role="alert" className="mb-4 rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{error}</p>}
    <div className="grid gap-5 lg:grid-cols-[0.8fr_1.2fr]">
      <form onSubmit={create} className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="font-semibold text-slate-900">Record IOU</h2>
        <label className="block text-sm text-slate-600">Lender<select required value={form.fromUserId} onChange={e => setForm({ ...form, fromUserId: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2">{group?.members?.map(member => <option key={member.userId} value={member.userId}>{member.name}</option>)}</select></label>
        <label className="block text-sm text-slate-600">Borrower<select required value={form.toUserId} onChange={e => setForm({ ...form, toUserId: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2">{group?.members?.map(member => <option key={member.userId} value={member.userId}>{member.name}</option>)}</select></label>
        <label className="block text-sm text-slate-600">Amount (₹)<input required min="0.01" step="0.01" type="number" value={form.amount} onChange={e => setForm({ ...form, amount: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>
        <label className="block text-sm text-slate-600">Note<input maxLength="255" value={form.note} onChange={e => setForm({ ...form, note: e.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>
        <button disabled={busy || !form.fromUserId || !form.toUserId || form.fromUserId === form.toUserId} className="w-full rounded-lg bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{busy ? 'Saving…' : 'Save IOU'}</button>
      </form>
      <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"><h2 className="font-semibold text-slate-900">History <span className="font-normal text-slate-400">({ious.length})</span></h2>
        {ious.length === 0 ? <p className="mt-5 rounded-xl bg-slate-50 p-5 text-sm text-slate-500">No IOUs recorded.</p> : <ul className="mt-3 divide-y divide-slate-100">{ious.map(iou => <li key={iou.id} className="flex items-center gap-4 py-4"><div className="min-w-0 flex-1"><p className="font-medium text-slate-800">{iou.fromUserName} lent to {iou.toUserName}</p><p className="mt-1 text-sm text-slate-500">{iou.note || 'No note'} · {new Date(iou.createdAt).toLocaleDateString()}</p></div><span className="font-semibold text-slate-900">{money(iou.amountPaise)}</span>{iou.settled ? <span className="text-xs font-semibold text-emerald-700">Settled</span> : <button onClick={() => settle(iou.id)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-600">Mark settled</button>}</li>)}</ul>}
      </section>
    </div>
  </div>
}

export default Ious
