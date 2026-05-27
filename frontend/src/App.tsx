import React, { Suspense, lazy } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';

// Lazy-loaded route components for code splitting
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage'));
const RegisterPage = lazy(() => import('@/features/auth/pages/RegisterPage'));
const DashboardPage = lazy(() => import('@/features/dashboard/pages/DashboardPage'));
const TransactionsPage = lazy(() => import('@/features/transactions/pages/TransactionsPage'));
const CategoriesPage = lazy(() => import('@/features/categories/pages/CategoriesPage'));
const BudgetsPage = lazy(() => import('@/features/budgets/pages/BudgetsPage'));
const NotificationsPage = lazy(() => import('@/features/notifications/pages/NotificationsPage'));
const InsightsPage = lazy(() => import('@/features/analysis/pages/InsightsPage'));
const ReportsPage = lazy(() => import('@/features/reports/pages/ReportsPage'));
const SettingsPage = lazy(() => import('@/features/settings/pages/SettingsPage'));
const AppLayout = lazy(() => import('@/components/layout/AppLayout'));
const AuthLayout = lazy(() => import('@/components/layout/AuthLayout'));
const ProtectedRoute = lazy(() => import('@/components/auth/ProtectedRoute'));

const PageLoader = () => (
  <div className="flex items-center justify-center min-h-[60vh]">
    <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
  </div>
);

function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        {/* Public Auth Routes */}
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>

        {/* Protected App Routes */}
        <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/categories" element={<CategoriesPage />} />
          <Route path="/budgets" element={<BudgetsPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/insights" element={<InsightsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}

export default App;
