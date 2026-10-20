import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'

const API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api`

export default function AuthAction() {
  const { pathname, hash, search } = useLocation()
  const token = new URLSearchParams(hash.slice(1)).get('token') || ''
  useEffect(() => {
    if (token) window.history.replaceState(null, '', `${pathname}${search}`)
  }, [pathname, search, token])
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const verify = pathname === '/verify-email'
  const forgot = pathname === '/forgot-password'
  const claimRequest = pathname === '/claim-legacy-account'
  const reset = pathname === '/reset-password'
  const endpoint = verify ? 'verify-email' : forgot ? 'forgot-password' : claimRequest ? 'claim-legacy-account' : reset ? 'reset-password' : 'complete-legacy-claim'
  const title = verify ? 'Verify your email' : forgot ? 'Reset your password' : claimRequest ? 'Secure an existing account' : reset ? 'Choose a new password' : 'Secure your existing account'
  const needsPassword = reset || pathname === '/claim-account'

  async function submit(event) {
    event.preventDefault(); setBusy(true); setError(''); setMessage('')
    const body = verify ? { token } : forgot || claimRequest ? { email } : { token, password }
    try {
      const response = await fetch(`${API_BASE_URL}/auth/${endpoint}`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
      })
      const result = await response.json()
      if (!response.ok) throw new Error(result.detail || result.message || 'Unable to complete this request.')
      setMessage(result.message || 'Request completed.')
    } catch (requestError) { setError(requestError.message) }
    finally { setBusy(false) }
  }

  return <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
    <form onSubmit={submit} className="w-full max-w-md space-y-4 rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
      <h1 className="text-2xl font-bold text-slate-900">{title}</h1>
      {error && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
      {message && <p role="status" className="rounded-lg bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
      {(forgot || claimRequest) && <label className="block text-sm text-slate-600">Email<input required type="email" autoComplete="email" value={email} onChange={event => setEmail(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>}
      {needsPassword && <label className="block text-sm text-slate-600">New password<input required type="password" minLength={12} maxLength={128} autoComplete="new-password" value={password} onChange={event => setPassword(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>}
      {!forgot && !claimRequest && !token && <p role="alert" className="text-sm text-rose-700">This link is missing its one-time token. Request a new link.</p>}
      <button disabled={busy || (!forgot && !claimRequest && !token)} className="w-full rounded-lg bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{busy ? 'Please wait…' : 'Continue'}</button>
      <Link to="/login" className="block text-center text-sm text-emerald-700">Return to sign in</Link>
    </form>
  </main>
}
