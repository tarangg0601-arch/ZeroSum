import { useEffect, useState } from 'react'
import { apiRequest } from '../../api/client.js'
import { signOut } from '../../api/session.js'

export default function Settings() {
  const [user, setUser] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => { apiRequest('/users/me').then(setUser).catch(err => setError(err.message || 'Could not load account details.')) }, [])
  return <div className="mx-auto max-w-3xl"><p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">Account</p><h1 className="mt-2 text-3xl font-bold text-slate-900">Settings</h1><section className="mt-6 rounded-2xl border border-slate-200 bg-white p-6"><h2 className="font-semibold text-slate-900">Signed-in account</h2>{error ? <p role="alert" className="mt-3 text-sm text-rose-600">{error}</p> : user ? <dl className="mt-4 grid gap-4 sm:grid-cols-2"><div><dt className="text-xs text-slate-500">Name</dt><dd className="mt-1 font-medium">{user.name}</dd></div><div><dt className="text-xs text-slate-500">Account ID</dt><dd className="mt-1 font-medium">{user.userId}</dd></div></dl> : <p role="status" className="mt-3 text-sm text-slate-500">Loading account…</p>}<button type="button" onClick={signOut} className="mt-6 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700">Sign out</button></section></div>
}
