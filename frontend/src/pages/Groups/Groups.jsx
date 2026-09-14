import {
  ArrowRight,
  Home,
  MapPin,
  Plus,
  Users,
  WalletCards,
} from 'lucide-react'

import GroupCard from '../../components/common/GroupCard'

const groups = [
  {
    id: 1,
    name: 'Bali trip',
    type: 'Travel',
    members: 5,
    balance: '+₹210',
    status: 'You are owed',
    icon: MapPin,
  },
  {
    id: 2,
    name: 'Hostel 4B',
    type: 'Home',
    members: 4,
    balance: '-₹60',
    status: 'You owe',
    icon: Home,
  },
  {
    id: 3,
    name: 'Friday poker',
    type: 'Friends',
    members: 6,
    balance: '₹0',
    status: 'Settled',
    icon: Users,
  },
  {
    id: 4,
    name: 'Goa weekend',
    type: 'Travel',
    members: 7,
    balance: '+₹480',
    status: 'You are owed',
    icon: MapPin,
  },
  {
    id: 5,
    name: 'College project',
    type: 'Friends',
    members: 5,
    balance: '-₹125',
    status: 'You owe',
    icon: Users,
  },
]

function Groups() {
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

        <button className="flex items-center justify-center gap-2 rounded-xl bg-emerald-500 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-600 hover:shadow-md">
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
                5
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
                Open balances
              </p>

              <p className="text-xl font-semibold text-slate-900">
                4
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
                Your net position
              </p>

              <p className="text-xl font-semibold text-emerald-600">
                +₹505
              </p>
            </div>

          </div>
        </div>

      </section>

      {/* Groups */}
      <section>

        <div className="mb-4 flex items-center justify-between">

          <h2 className="text-lg font-semibold text-slate-900">
            All groups
          </h2>

          <p className="text-sm text-slate-400">
            5 groups
          </p>

        </div>

        <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">

          {groups.map((group) => (
            <GroupCard
              key={group.id}
              {...group}
            />
          ))}

          {/* Create group card */}
          <button className="flex min-h-[280px] flex-col items-center justify-center rounded-2xl border border-dashed border-slate-300 bg-white/50 p-5 text-center transition hover:border-emerald-400 hover:bg-emerald-50/30">

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

      </section>

    </div>
  )
}

export default Groups