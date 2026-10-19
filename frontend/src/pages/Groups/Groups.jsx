import {
  ArrowRight,
  Home,
  MapPin,
  Plus,
  Users,
  WalletCards,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import GroupCard from '../../components/common/GroupCard'
import { createGroup, getGroups } from '../../api/groupsApi'

function Groups() {
  const navigate = useNavigate()
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [groupName, setGroupName] = useState('')
  const [groupType, setGroupType] = useState('general')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState('')

  async function handleCreate(event) {
    event.preventDefault()
    setCreating(true)
    setCreateError('')
    try {
      const group = await createGroup({ name: groupName, groupType })
      navigate(`/groups/${group.id}`)
    } catch (err) {
      setCreateError(err.message)
    } finally {
      setCreating(false)
    }
  }

  useEffect(() => {
    async function loadGroups() {
      try {
        setLoading(true)
        setError('')

        const data = await getGroups()
        const withBalances = await Promise.all(data.map(async group => {
          const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'}/api/groups/${group.id}/balances`)
          if (!response.ok) throw new Error('Failed to load group balances')
          const balances = await response.json()
          const currentUserId = Number(sessionStorage.getItem('zerosum_user_id'))
          const creatorBalance = balances.find(balance => balance.userId === currentUserId)?.balancePaise || 0
          return { ...group, creatorBalance, hasOpenBalances: balances.some(balance => balance.balancePaise !== 0) }
        }))
        setGroups(withBalances)
      } catch (err) {
        console.error(err)
        setError('Unable to load groups.')
      } finally {
        setLoading(false)
      }
    }

    loadGroups()
  }, [])

  return (
    <div className="mx-auto max-w-[1400px]">

      {/* Page header */}
      <section className="mb-8 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">

        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
            Workspace
          </p>

          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
            Your groups
          </h1>

          <p className="mt-2 text-sm text-slate-500">
            Manage your shared expenses and settlements in one place.
          </p>
        </div>

        <button onClick={() => setShowCreate(true)} className="flex items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-600 hover:shadow-md">
          <Plus size={17} />
          Create group
        </button>

      </section>

      {/* Summary */}
      <section className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center gap-3">

            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
              <Users size={18} />
            </div>

            <div>
              <p className="text-xs text-slate-400">
                Total groups
              </p>

              <p className="text-xl font-semibold text-slate-900">
                {groups.length}
              </p>
            </div>

          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center gap-3">

            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
              <WalletCards size={18} />
            </div>

            <div>
              <p className="text-xs text-slate-400">
                Members across groups
              </p>

              <p className="text-xl font-semibold text-slate-900">
                {groups.reduce((total, group) => total + group.members.length, 0)}
              </p>
            </div>

          </div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex items-center gap-3">

            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
              <ArrowRight size={18} />
            </div>

            <div>
              <p className="text-xs text-slate-400">
                Open groups
              </p>

              <p className="text-xl font-semibold text-emerald-600">
                {groups.filter(group => group.hasOpenBalances).length}
              </p>
            </div>

          </div>
        </div>

      </section>

      {/* Loading */}
      {loading && (
        <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
          <p className="text-sm text-slate-500">
            Loading your groups...
          </p>
        </div>
      )}

      {/* Error */}
      {!loading && error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center">
          <p className="text-sm font-medium text-rose-600">
            {error}
          </p>

          <p className="mt-1 text-xs text-rose-400">
            Make sure the Spring Boot backend is running on port 8080.
          </p>
        </div>
      )}

      {/* Groups */}
      {!loading && !error && (
        <section>

          <div className="mb-4 flex items-center justify-between">

            <h2 className="text-lg font-semibold text-slate-900">
              All groups
            </h2>

            <p className="text-sm text-slate-400">
              {groups.length} {groups.length === 1 ? 'group' : 'groups'}
            </p>

          </div>

          {groups.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-slate-300 bg-white/50 p-12 text-center">

              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
                <Users size={21} />
              </div>

              <h3 className="mt-4 font-semibold text-slate-800">
                No groups yet
              </h3>

              <p className="mt-1 text-sm text-slate-400">
                Create your first group to start tracking shared expenses.
              </p>

            </div>
          ) : (
            <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">

              {groups.map((group) => (
                <GroupCard
                  key={group.id}
                  id={group.id}
                  name={group.name}
                  type={group.groupType}
                  members={group.members.length}
                  balance={formatSignedRupees(group.creatorBalance)}
                  status={group.creatorBalance === 0 ? 'Settled' : group.creatorBalance > 0 ? 'You are owed' : 'You owe'}
                  icon={getGroupIcon(group.groupType)}
                />
              ))}

              {/* Create group card */}
              <button onClick={() => setShowCreate(true)} className="flex min-h-[280px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white/50 p-5 text-center transition hover:border-emerald-400 hover:bg-emerald-50/30">

                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
                  <Plus size={21} />
                </div>

                <h3 className="mt-4 font-semibold text-slate-800">
                  Create a new group
                </h3>

                <p className="mt-1 max-w-[220px] text-xs leading-5 text-slate-400">
                  Start tracking expenses with friends, roommates or your next trip.
                </p>

              </button>

            </div>
          )}

        </section>
      )}

      {showCreate && <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/40 p-4" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setShowCreate(false) }}>
        <form onSubmit={handleCreate} className="w-full max-w-md space-y-4 rounded-2xl bg-white p-6 shadow-xl">
          <div><h2 className="text-xl font-semibold text-slate-900">Create a group</h2><p className="mt-1 text-sm text-slate-500">Your account will be added as the first participant.</p></div>
          {createError && <p role="alert" className="rounded-lg bg-rose-50 p-3 text-sm text-rose-700">{createError}</p>}
          <label className="block text-sm text-slate-600">Group name<input required maxLength="150" value={groupName} onChange={event => setGroupName(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 px-3 py-2" /></label>
          <label className="block text-sm text-slate-600">Group mode<select value={groupType} onChange={event => setGroupType(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-200 bg-white px-3 py-2"><option value="general">General</option><option value="hostel">Hostel</option><option value="trip">Trip</option></select></label>
          <div className="flex justify-end gap-2"><button type="button" onClick={() => setShowCreate(false)} className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600">Cancel</button><button disabled={creating} className="rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{creating ? 'Creating…' : 'Create group'}</button></div>
        </form>
      </div>}
    </div>
  )
}

function getGroupIcon(groupType) {
  const type = groupType?.toLowerCase()

  if (type === 'trip') {
    return MapPin
  }

  if (type === 'hostel') {
    return Home
  }

  return Users
}

function formatSignedRupees(paise) {
  const amount = Math.abs(paise) / 100
  const formatted = `₹${amount.toLocaleString('en-IN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
  return paise > 0 ? `+${formatted}` : paise < 0 ? `-${formatted}` : '₹0'
}

export default Groups
