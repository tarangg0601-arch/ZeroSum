// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Balances from './Balances.jsx'

const plan = { balances: [
  { userId: 1, userName: 'Asha', balancePaise: 500 },
  { userId: 2, userName: 'Ravi', balancePaise: -500 },
], transactions: [{ fromUserId: 2, fromUserName: 'Ravi', toUserId: 1, toUserName: 'Asha', amountPaise: 500 }], explanation: { steps: [] } }

describe('balances user journey', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(async (url) => {
      if (String(url).endsWith('/groups/2')) return Response.json({ id: 2, name: 'Trip', createdBy: 1 })
      if (String(url).endsWith('/settlement/history')) return Response.json([])
      if (String(url).endsWith('/expenses')) return Response.json([])
      if (String(url).endsWith('/settlement/whatif')) return Response.json(plan)
      return Response.json(plan)
    }))
  })
  afterEach(() => { cleanup(); vi.unstubAllGlobals() })

  it('previews a hypothetical expense through the what-if API without recording it', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter initialEntries={['/balances?groupId=2']}><Balances /></MemoryRouter>)
    await screen.findByText('Preview a change')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Simulation type' }), 'expense')
    await user.type(screen.getByRole('textbox', { name: 'Expense description' }), 'Hotel')
    await user.selectOptions(screen.getByRole('combobox', { name: 'Expense payer' }), '1')
    await user.type(screen.getByRole('spinbutton', { name: 'Expense amount in rupees' }), '12.34')
    await user.click(screen.getByRole('checkbox', { name: /Asha/ }))
    await user.click(screen.getByRole('checkbox', { name: /Ravi/ }))
    await user.click(screen.getByRole('button', { name: 'Simulate' }))

    await screen.findByText(/Preview: 1 payment would remain/)
    await waitFor(() => expect(fetch.mock.calls.some(([url]) => String(url).endsWith('/settlement/whatif'))).toBe(true))
    const payload = JSON.parse(fetch.mock.calls.find(([url]) => String(url).endsWith('/settlement/whatif'))[1].body)
    expect(payload.expense).toMatchObject({ description: 'Hotel', amountPaise: 1234, paidBy: 1, participantIds: [1, 2], splitType: 'equal' })
    expect(fetch.mock.calls.some(([url]) => String(url).endsWith('/settlement/payments'))).toBe(false)
  })

  it('asks the user to select an accessible group instead of guessing an ID', async () => {
    const user = userEvent.setup()
    vi.stubGlobal('fetch', vi.fn(async input => {
      const url = String(input)
      if (url.endsWith('/api/groups')) return Response.json([{ id: 8, name: 'Lake trip', members: [] }])
      if (url.endsWith('/api/users/me')) return Response.json({ userId: 1, name: 'Asha' })
      if (url.endsWith('/api/groups/8')) return Response.json({ id: 8, name: 'Lake trip' })
      if (url.endsWith('/api/groups/8/settlement/history') || url.endsWith('/api/groups/8/expenses')) return Response.json([])
      return Response.json({ balances: [], transactions: [], explanation: { steps: [] } })
    }))
    render(<MemoryRouter initialEntries={['/balances']}><Balances /></MemoryRouter>)
    await user.click(await screen.findByRole('button', { name: /Lake trip/ }))
    await screen.findByText('Suggested settlement plan')
    expect(fetch.mock.calls.some(([url]) => String(url).endsWith('/groups/2'))).toBe(false)
  })
})
