import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

import MainLayout from './components/layout/MainLayout'

import Dashboard from './pages/Dashboard/Dashboard'
import Groups from './pages/Groups/Groups'
import GroupDetails from './pages/Group/GroupDetails'
import AddExpense from './pages/AddExpense/AddExpense'
import Balances from './pages/Balances/Balances'
import Recurring from './pages/Recurring/Recurring'
import Insights from './pages/Insights/Insights'
import Ious from './pages/Ious/Ious'
import Auth from './pages/Auth/Auth'
import AuthAction from './pages/Auth/AuthAction'
import { useLocation } from 'react-router-dom'
import Activity from './pages/Activity/Activity'
import Settings from './pages/Settings/Settings'
import { Link } from 'react-router-dom'


function App() {
  return (
    <BrowserRouter>

      <Routes>

        <Route path="/login" element={<Auth />} />
        <Route path="/verify-email" element={<AuthAction />} />
        <Route path="/forgot-password" element={<AuthAction />} />
        <Route path="/reset-password" element={<AuthAction />} />
        <Route path="/claim-legacy-account" element={<AuthAction />} />
        <Route path="/claim-account" element={<AuthAction />} />

        <Route element={<RequireAuth><MainLayout /></RequireAuth>}>

          {/* Dashboard */}
          <Route
            path="/dashboard"
            element={<Dashboard />}
          />

          {/* Groups list */}
          <Route
            path="/groups"
            element={<Groups />}
          />

          {/* Individual group */}
          <Route
            path="/groups/:groupId"
            element={<GroupDetails />}
          />

          {/* Add expense */}
          <Route
            path="/expenses/new"
            element={<AddExpense />}
          />

          {/* Balances / settlement */}
          <Route
            path="/balances"
            element={<Balances />}
          />

          <Route path="/groups/:groupId/recurring" element={<Recurring />} />
          <Route path="/groups/:groupId/insights" element={<Insights />} />
          <Route path="/groups/:groupId/ious" element={<Ious />} />
          <Route path="/expenses" element={<Activity expensesOnly />} />
          <Route path="/activity" element={<Activity />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="*" element={<NotFound />} />

        </Route>

        {/* Default route */}
        <Route
          path="/"
          element={<Navigate to="/dashboard" replace />}
        />

      </Routes>

    </BrowserRouter>
  )
}

function NotFound() {
  return <main className="mx-auto max-w-lg rounded-2xl border border-slate-200 bg-white p-8 text-center"><h1 className="text-2xl font-bold text-slate-900">Page not found</h1><p className="mt-2 text-sm text-slate-500">This ZeroSum page does not exist.</p><Link to="/dashboard" className="mt-5 inline-flex rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-white">Back to dashboard</Link></main>
}

function RequireAuth({ children }) {
  const location = useLocation()
  return sessionStorage.getItem('zerosum_token') ? children : <Navigate to="/login" replace state={{ from: location }} />
}

export default App
