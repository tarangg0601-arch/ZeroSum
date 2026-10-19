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
import { useLocation } from 'react-router-dom'


function App() {
  return (
    <BrowserRouter>

      <Routes>

        <Route path="/login" element={<Auth />} />

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

function RequireAuth({ children }) {
  const location = useLocation()
  return sessionStorage.getItem('zerosum_token') ? children : <Navigate to="/login" replace state={{ from: location }} />
}

export default App
