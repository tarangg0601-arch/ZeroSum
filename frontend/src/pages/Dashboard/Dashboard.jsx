import BalanceOverview from '../../components/dashboard/BalanceOverview'
import GroupsSection from '../../components/dashboard/GroupsSection'
import SettlementOptimizer from '../../components/dashboard/SettlementOptimizer'
import RecentActivity from '../../components/dashboard/RecentActivity'
import UpcomingSettlements from '../../components/dashboard/UpcomingSettlements'

function Dashboard() {
  return (
    <div className="mx-auto max-w-[1400px]">

      {/* Greeting */}
      <section className="mb-7 flex items-end justify-between">

        <div>
          <p className="text-xs font-semibold uppercase tracking-widest text-emerald-600">
            Overview
          </p>

          <h1 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">
            Good evening, Tarang 👋
          </h1>

          <p className="mt-2 text-sm text-slate-500">
            Here’s what’s happening with your groups today.
          </p>
        </div>

        <p className="hidden text-sm text-slate-400 lg:block">
          3 active groups
        </p>

      </section>

      {/* Financial Overview */}
      <BalanceOverview />

      {/* Groups + Settlement Optimizer */}
      <div className="mt-5 grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">

        <GroupsSection />

        <SettlementOptimizer />

      </div>

      {/* Recent Activity + Upcoming Settlements */}
      <div className="mt-5 grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">

        <RecentActivity />

        <UpcomingSettlements />

      </div>

    </div>
  )
}

export default Dashboard