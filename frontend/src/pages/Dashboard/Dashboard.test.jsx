// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Dashboard from './Dashboard.jsx'

const { apiRequest } = vi.hoisted(() => ({ apiRequest: vi.fn() }))
vi.mock('../../api/client.js', () => ({ apiRequest: (...args) => apiRequest(...args) }))

describe('dashboard account data', () => {
  afterEach(() => { cleanup(); apiRequest.mockReset() })

  it('shows a useful empty state when the signed-in user has no groups', async () => {
    apiRequest.mockImplementation(path => path === '/users/me'
      ? Promise.resolve({ userId: 3, name: 'Samira' })
      : Promise.resolve([]))
    render(<MemoryRouter><Dashboard /></MemoryRouter>)
    expect(await screen.findByText('Welcome, Samira')).toBeTruthy()
    expect(screen.getByText('Start with a group')).toBeTruthy()
    expect(screen.queryByText(/Tarang|9 debts/)).toBeNull()
  })

  it('loads real group, balance, expense, and settlement values', async () => {
    apiRequest.mockImplementation(path => {
      if (path === '/users/me') return Promise.resolve({ userId: 3, name: 'Samira Patel' })
      if (path === '/groups') return Promise.resolve([{ id: 8, name: 'Flatmates', groupType: 'general', members: [{ userId: 3 }, { userId: 4 }] }])
      if (path.endsWith('/balances')) return Promise.resolve([{ userId: 3, balancePaise: 550 }, { userId: 4, balancePaise: -550 }])
      if (path.endsWith('/expenses')) return Promise.resolve([{ id: 10, description: 'Groceries', amountPaise: 1050, expenseDate: '2026-10-20' }])
      if (path.endsWith('/settlement')) return Promise.resolve({ transactions: [{ fromUserId: 4, fromUserName: 'Lee', toUserId: 3, toUserName: 'Samira Patel', amountPaise: 550 }] })
      throw new Error(`Unexpected API request: ${path}`)
    })
    render(<MemoryRouter><Dashboard /></MemoryRouter>)
    expect(await screen.findByText('Groceries')).toBeTruthy()
    expect(screen.getAllByText('Flatmates').length).toBeGreaterThan(0)
    expect(screen.getByText('You are owed overall')).toBeTruthy()
    expect(screen.getByText(/Lee pays Samira Patel/)).toBeTruthy()
  })
})
