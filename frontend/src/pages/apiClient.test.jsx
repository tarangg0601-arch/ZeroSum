// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../api/client.js'

describe('API client', () => {
  afterEach(() => { vi.unstubAllGlobals(); sessionStorage.clear() })

  it('sends bearer tokens only to the configured API origin', async () => {
    sessionStorage.setItem('zerosum_token', 'session-token')
    const fetchMock = vi.fn(async () => Response.json({ ok: true }))
    vi.stubGlobal('fetch', fetchMock)
    await apiRequest('/groups')
    await apiRequest('http://localhost.attacker.invalid:8080/api/groups')
    expect(fetchMock.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer session-token')
    expect(fetchMock.mock.calls[1][1].headers.get('Authorization')).toBeNull()
  })

  it('surfaces backend validation messages consistently', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ error: 'Join code is invalid' }, { status: 404 })))
    await expect(apiRequest('/groups/join', { method: 'POST' })).rejects.toMatchObject({ message: 'Join code is invalid', status: 404 })
  })
})
