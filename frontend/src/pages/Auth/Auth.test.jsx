// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Auth from './Auth.jsx'

describe('account authentication journey', () => {
  afterEach(() => { cleanup(); sessionStorage.clear(); vi.unstubAllGlobals() })

  it('registers and stores the short-lived bearer token for this browser session', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ userId: 8, token: 'signed-token' }, { status: 201 })))
    render(<MemoryRouter><Auth /></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: /new to zerosum/i }))
    await user.type(screen.getByLabelText('Name'), 'Mira')
    await user.type(screen.getByLabelText('Email'), 'mira@example.test')
    await user.type(screen.getByLabelText(/Password/), 'a-long-password-123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() => expect(sessionStorage.getItem('zerosum_token')).toBe('signed-token'))
    expect(sessionStorage.getItem('zerosum_user_id')).toBe('8')
    expect(fetch.mock.calls[0][0]).toContain('/auth/register')
    const sent = JSON.parse(fetch.mock.calls[0][1].body)
    expect(sent.password).toBe('a-long-password-123')
    expect(sent.email).toBe('mira@example.test')
  })

  it('does not store credentials when sign-in is rejected', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ detail: 'Invalid email or password' }, { status: 401 })))
    render(<MemoryRouter><Auth /></MemoryRouter>)
    await user.type(screen.getByLabelText('Email'), 'mira@example.test')
    await user.type(screen.getByLabelText(/Password/), 'wrong-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect((await screen.findByRole('alert')).textContent).toContain('Invalid email or password')
    expect(sessionStorage.getItem('zerosum_token')).toBeNull()
  })
})
