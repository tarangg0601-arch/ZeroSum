const configuredOrigin = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
export const API_ORIGIN = new URL(configuredOrigin, window.location.origin).origin

export function apiUrl(path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return new URL(`/api${normalizedPath}`, API_ORIGIN).toString()
}

export async function apiRequest(path, options = {}) {
  const url = new URL(path.startsWith('http') ? path : apiUrl(path), window.location.origin)
  const headers = new Headers(options.headers || {})
  const token = sessionStorage.getItem('zerosum_token')
  const isAuthEndpoint = url.pathname.startsWith('/api/auth/')
  const isLogout = url.pathname === '/api/auth/logout'

  if (token && url.origin === API_ORIGIN && (!isAuthEndpoint || isLogout)) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(url.toString(), { ...options, headers })
  if (response.status === 401 && token && (!isAuthEndpoint || isLogout)) {
    sessionStorage.removeItem('zerosum_token')
    sessionStorage.removeItem('zerosum_user_id')
    window.location.assign('/login')
  }

  let data = null
  if (response.status !== 204) {
    try { data = await response.json() } catch { data = null }
  }
  if (!response.ok) {
    const message = data?.error || data?.message || data?.detail || `Request failed (${response.status})`
    const error = new Error(message)
    error.status = response.status
    error.data = data
    throw error
  }
  return data
}
