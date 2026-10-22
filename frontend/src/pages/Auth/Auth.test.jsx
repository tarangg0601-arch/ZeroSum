// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Auth from './Auth.jsx'
import AuthAction from './AuthAction.jsx'

describe('account authentication journey', () => {
  afterEach(() => { cleanup(); sessionStorage.clear(); vi.unstubAllGlobals() })

  it('registers without creating a session before email verification', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ message: 'Check your email' }, { status: 202 })))
    render(<MemoryRouter><Auth /></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: /new to zerosum/i }))
    await user.type(screen.getByLabelText('Name'), 'Mira')
    await user.type(screen.getByLabelText('Email'), 'mira@example.test')
    await user.type(screen.getByLabelText(/Password/), 'a-long-password-123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect((await screen.findByRole('status')).textContent).toContain('Check your email')
    expect(sessionStorage.getItem('zerosum_token')).toBeNull()
    expect(sessionStorage.getItem('zerosum_user_id')).toBeNull()
    expect(fetch.mock.calls[0][0]).toContain('/auth/register')
    const sent = JSON.parse(fetch.mock.calls[0][1].body)
    expect(sent.password).toBe('a-long-password-123')
    expect(sent.email).toBe('mira@example.test')
  })

  it('stores a bearer token in session storage only after successful sign-in', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ userId: 8, token: 'signed-token' }, { status: 200 })))
    render(<MemoryRouter><Auth /></MemoryRouter>)
    await user.type(screen.getByLabelText('Email'), 'mira@example.test')
    await user.type(screen.getByLabelText(/Password/), 'a-long-password-123')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))
    await waitFor(() => expect(sessionStorage.getItem('zerosum_token')).toBe('signed-token'))
    expect(sessionStorage.getItem('zerosum_user_id')).toBeNull()
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

  it('submits the one-time verification token from the URL fragment without storing it', async () => {
    const user = userEvent.setup()
    window.history.replaceState({}, '', '/verify-email#token=one-time-value')
    vi.stubGlobal('fetch', vi.fn(async () => Response.json({ message: 'Email verified' }, { status: 200 })))
    render(<MemoryRouter initialEntries={['/verify-email#token=one-time-value']}><AuthAction /></MemoryRouter>)
    await user.click(screen.getByRole('button', { name: 'Continue' }))
    expect((await screen.findByRole('status')).textContent).toContain('Email verified')
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({ token: 'one-time-value' })
    expect(window.location.hash).toBe('')
    expect(sessionStorage.getItem('zerosum_token')).toBeNull()
  })

  it('does not accept a token in the query string', () => {
    render(<MemoryRouter initialEntries={['/verify-email?token=must-not-be-used']}><AuthAction /></MemoryRouter>)
    expect(screen.getByRole('button', { name: 'Continue' }).disabled).toBe(true)
    expect(screen.getByRole('alert').textContent).toContain('missing its one-time token')
  })
})
