// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useParams } from 'react-router-dom'
import Groups from './Groups.jsx'

const { apiRequest, getGroups, createGroup, joinGroup } = vi.hoisted(() => ({
  apiRequest: vi.fn(), getGroups: vi.fn(), createGroup: vi.fn(), joinGroup: vi.fn(),
}))
vi.mock('../../api/client.js', () => ({ apiRequest }))
vi.mock('../../api/groupsApi.js', () => ({ getGroups, createGroup, joinGroup }))

function GroupDestination() {
  const { groupId } = useParams()
  return <p>Opened group {groupId}</p>
}

describe('group collaboration journey', () => {
  afterEach(() => { cleanup(); apiRequest.mockReset(); getGroups.mockReset(); createGroup.mockReset(); joinGroup.mockReset() })

  it('joins a group by code and opens the joined group', async () => {
    const user = userEvent.setup()
    getGroups.mockResolvedValue([])
    apiRequest.mockResolvedValue({ userId: 5, name: 'Ari' })
    joinGroup.mockResolvedValue({ id: 9, name: 'Weekend trip' })
    render(<MemoryRouter initialEntries={['/groups']}><Routes><Route path="/groups" element={<Groups />} /><Route path="/groups/:groupId" element={<GroupDestination />} /></Routes></MemoryRouter>)
    await user.type(await screen.findByRole('textbox', { name: 'Group join code' }), 'a1b2c3d4e5f6g7h8')
    await user.click(screen.getByRole('button', { name: 'Join group' }))
    expect(await screen.findByText('Opened group 9')).toBeTruthy()
    expect(joinGroup).toHaveBeenCalledWith('A1B2C3D4E5F6G7H8')
  })

  it('shows the backend error for an invalid join code', async () => {
    const user = userEvent.setup()
    getGroups.mockResolvedValue([])
    apiRequest.mockResolvedValue({ userId: 5, name: 'Ari' })
    joinGroup.mockRejectedValue(new Error('Join code is invalid'))
    render(<MemoryRouter><Groups /></MemoryRouter>)
    await user.type(await screen.findByRole('textbox', { name: 'Group join code' }), 'bad-code')
    await user.click(screen.getByRole('button', { name: 'Join group' }))
    expect((await screen.findByRole('alert')).textContent).toContain('Join code is invalid')
  })
})
