import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

import MainLayout from './components/layout/MainLayout'

import Dashboard from './pages/Dashboard/Dashboard'
import Groups from './pages/Groups/Groups'
import GroupDetails from './pages/Group/GroupDetails'
import AddExpense from './pages/AddExpense/AddExpense'
import Balances from './pages/Balances/Balances'


function App() {
  return (
    <BrowserRouter>

      <Routes>

        <Route element={<MainLayout />}>

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

export default App