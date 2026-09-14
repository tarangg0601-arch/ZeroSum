import { ArrowRight, Home, MapPin, Users } from 'lucide-react'

const groups = [
  {
    name: 'Bali trip',
    members: '5 members',
    balance: '+₹210',
    status: 'You are owed',
    icon: MapPin,
    positive: true,
  },
  {
    name: 'Hostel 4B',
    members: '4 members',
    balance: '-₹60',
    status: 'You owe',
    icon: Home,
    positive: false,
  },
  {
    name: 'Friday poker',
    members: '6 members',
    balance: 'Settled',
    status: 'All settled up',
    icon: Users,
    positive: null,
  },
]

function GroupsSection() {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-slate-900">
          Your groups
        </h2>

        <button className="flex items-center gap-1 text-sm font-medium text-emerald-600 transition hover:text-emerald-700">
          View all
          <ArrowRight size={15} />
        </button>
      </div>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        {groups.map((group) => {
          const Icon = group.icon

          return (
            <button
              key={group.name}
              className="group text-left rounded-xl border border-slate-200 bg-white p-4 transition duration-200 hover:-translate-y-0.5 hover:border-emerald-200 hover:bg-emerald-50/20 hover:shadow-sm"
            >
              <div className="flex items-start justify-between">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
                  <Icon size={19} />
                </div>

                <ArrowRight
                  size={16}
                  className="text-slate-300 transition group-hover:translate-x-1 group-hover:text-emerald-500"
                />
              </div>

              <h3 className="mt-4 font-semibold text-slate-900">
                {group.name}
              </h3>

              <p className="mt-1 flex items-center gap-1 text-xs text-slate-400">
                <Users size={12} />
                {group.members}
              </p>

              <div className="mt-4">
                <p
                  className={`text-lg font-semibold ${
                    group.positive === true
                      ? 'text-emerald-600'
                      : group.positive === false
                        ? 'text-rose-500'
                        : 'text-slate-500'
                  }`}
                >
                  {group.balance}
                </p>

                <p className="mt-0.5 text-xs text-slate-400">
                  {group.status}
                </p>
              </div>
            </button>
          )
        })}
      </div>
    </section>
  )
}

export default GroupsSection