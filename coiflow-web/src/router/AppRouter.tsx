import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '@/shared/components/ProtectedRoute';
import LoginPage from '@/features/auth/pages/LoginPage';
import AdminLayout from '@/features/admin/pages/AdminLayout';
import SalonsPage from '@/features/admin/pages/SalonsPage';
import ManagerLayout from '@/features/manager/pages/ManagerLayout';
import ManagerDashboard from '@/features/manager/pages/ManagerDashboard';
import StaffPage from '@/features/manager/pages/StaffPage';
import ServicesPage from '@/features/manager/pages/ServicesPage';
import ClientsPage from '@/features/manager/pages/ClientsPage';
import AppointmentsPage from '@/features/manager/pages/AppointmentsPage';
import TransactionsPage from '@/features/manager/pages/TransactionsPage';
import CommissionsPage from '@/features/manager/pages/CommissionsPage';
import BarberLayout from '@/features/barber/pages/BarberLayout';
import MySchedule from '@/features/barber/pages/MySchedule';
import MyCommissions from '@/features/barber/pages/MyCommissions';

export default function AppRouter() {
  return (
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />

        {/* Admin */}
        <Route
          path="/admin"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AdminLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<SalonsPage />} />
        </Route>

        {/* Manager */}
        <Route
          path="/manager"
          element={
            <ProtectedRoute allowedRoles={['MANAGER']}>
              <ManagerLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<ManagerDashboard />} />
          <Route path="appointments" element={<AppointmentsPage />} />
          <Route path="transactions" element={<TransactionsPage />} />
          <Route path="commissions" element={<CommissionsPage />} />
          <Route path="staff" element={<StaffPage />} />
          <Route path="services" element={<ServicesPage />} />
          <Route path="clients" element={<ClientsPage />} />
        </Route>

        {/* Barber */}
        <Route
          path="/barber"
          element={
            <ProtectedRoute allowedRoles={['BARBER']}>
              <BarberLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<MySchedule />} />
          <Route path="commissions" element={<MyCommissions />} />
          <Route path="clients" element={<ClientsPage />} />
        </Route>

        {/* Legacy redirects */}
        <Route path="/dashboard" element={<Navigate to="/manager" replace />} />
        <Route path="/my-schedule" element={<Navigate to="/barber" replace />} />

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
