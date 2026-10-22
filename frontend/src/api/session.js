import { apiRequest } from './client.js'

export async function signOut() {
  try { await apiRequest('/auth/logout', { method: 'POST' }) } catch { /* Local session must still be cleared. */ }
  sessionStorage.removeItem('zerosum_token')
  sessionStorage.removeItem('zerosum_user_id')
  window.location.assign('/login')
}
