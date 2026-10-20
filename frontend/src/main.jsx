import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'

const originalFetch = window.fetch.bind(window)
const apiOrigin = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
window.fetch = (input, init = {}) => {
  const url = typeof input === 'string' ? input : input.url
  const token = sessionStorage.getItem('zerosum_token')
  const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined))
  const logout = url.includes('/api/auth/logout')
  const publicAuthRoute = url.includes('/api/auth/') && !logout
  if (token && url.startsWith(apiOrigin) && !publicAuthRoute) headers.set('Authorization', `Bearer ${token}`)
  return originalFetch(input, { ...init, headers }).then(response => {
    if (response.status === 401 && token && !publicAuthRoute) {
      sessionStorage.removeItem('zerosum_token')
      sessionStorage.removeItem('zerosum_user_id')
      window.location.assign('/login')
    }
    return response
  })
}

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
