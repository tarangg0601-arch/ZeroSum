import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiRequest } from '../../api/client.js'

export default function Auth() {
  const navigate = useNavigate()
  const [register, setRegister] = useState(false)
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      const result = await apiRequest(`/auth/${register ? 'register' : 'login'}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(form),
      })
      if (register) { setNotice(result.message || 'Check your email for a verification link.'); return }
      sessionStorage.setItem('zerosum_token', result.token)
      navigate('/groups', { replace: true })
    } catch (submitError) { setError(submitError.message) }
    finally { setBusy(false) }
  }

  return <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
    <form onSubmit={submit} className="w-full max-w-md space-y-4 rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
      <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">ZeroSum account</p>
      <h1 className="text-2xl font-bold text-slate-900">{register ? 'Create your account' : 'Sign in'}</h1>
      {error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
      {notice && <p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{notice}</p>}
      {register && <label className="block text-sm text-slate-600">Name<input required maxLength={100} autoComplete="name" value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>}
      <label className="block text-sm text-slate-600">Email<input required type="email" autoComplete="email" value={form.email} onChange={event => setForm({ ...form, email: event.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>
      <label className="block text-sm text-slate-600">Password<input required type="password" minLength={register ? 12 : 1} maxLength={128} autoComplete={register ? 'new-password' : 'current-password'} value={form.password} onChange={event => setForm({ ...form, password: event.target.value })} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" />{register && <span className="mt-1 block text-xs text-slate-500">Use at least 12 characters.</span>}</label>
      <button disabled={busy} className="w-full rounded-lg bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{busy ? 'Please wait…' : register ? 'Create account' : 'Sign in'}</button>
      {!register && <Link to="/forgot-password" className="block text-center text-sm text-emerald-700">Forgot password?</Link>}
      {!register && <Link to="/claim-legacy-account" className="block text-center text-xs text-slate-500">Secure an existing account</Link>}
      <button type="button" onClick={() => { setRegister(!register); setError(''); setNotice('') }} className="w-full text-sm font-medium text-emerald-700">{register ? 'Already have an account? Sign in' : 'New to ZeroSum? Create an account'}</button>
    </form>
  </main>
}
