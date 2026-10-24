// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router-dom'
import Auth from './Auth.jsx'

function CurrentPath() {
  return <output aria-label="current path">{useLocation().pathname}</output>
}

describe('username and password authentication', () => {
  afterEach(() => { cleanup(); sessionStorage.clear(); vi.unstubAllGlobals() })

  it('registers with a username and password, then starts an authenticated session without email', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ userId: 8, username: 'mira', token: 'signed-token' }, { status: 201 })))
    render(<MemoryRouter initialEntries={['/login']}><Auth /><CurrentPath /></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: /new to zerosum/i }))
    await user.type(screen.getByLabelText('Display name (optional)'), 'Mira')
    await user.type(screen.getByLabelText(/^Username/), 'Mira_1')
    await user.type(screen.getByLabelText(/^Password/), 'a-long-password-123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => expect(sessionStorage.getItem('zerosum_token')).toBe('signed-token'))
    expect(screen.getByLabelText('current path').textContent).toBe('/groups')
    expect(fetch.mock.calls[0][0]).toContain('/auth/register')
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ username: 'Mira_1', password: 'a-long-password-123', name: 'Mira' })
    expect(screen.queryByLabelText(/email/i)).toBeNull()
  })

  it('logs in using username and password and stores no client-supplied identity', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ userId: 8, username: 'mira', token: 'signed-token' })))
    render(<MemoryRouter><Auth /><CurrentPath /></MemoryRouter>)
    await user.type(screen.getByLabelText(/^Username/), 'MIRA')
    await user.type(screen.getByLabelText('Password'), 'correct-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() => expect(sessionStorage.getItem('zerosum_token')).toBe('signed-token'))
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ username: 'MIRA', password: 'correct-password' })
    expect(sessionStorage.getItem('zerosum_user_id')).toBeNull()
  })

  it('does not store a token when credentials are rejected', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ detail: 'Invalid username or password' }, { status: 401 })))
    render(<MemoryRouter><Auth /></MemoryRouter>)
    await user.type(screen.getByLabelText(/^Username/), 'mira')
    await user.type(screen.getByLabelText('Password'), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Invalid username or password')
    expect(sessionStorage.getItem('zerosum_token')).toBeNull()
  })

  it('does not offer email-based verification or password recovery', async () => {
    render(<MemoryRouter><Auth /></MemoryRouter>)
    expect(screen.queryByRole('link')).toBeNull()
    expect(screen.getByText(/Forgotten passwords cannot currently be recovered/)).toBeTruthy()
  })
})
