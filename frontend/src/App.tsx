import React, { Suspense, lazy, useEffect, useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useAuthStore } from '@/stores/authStore';

// Lazy-loaded route components for code splitting
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage'));
const RegisterPage = lazy(() => import('@/features/auth/pages/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('@/features/auth/pages/ForgotPasswordPage'));
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
  <div className="flex items-center justify-center min-h-screen">
    <div className="text-center space-y-4">
      <Loader2 className="w-10 h-10 animate-spin text-blue-600 mx-auto" />
      <p className="text-slate-500 font-medium">Loading FinTrack AI...</p>
    </div>
  </div>
);

const InitGate: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isInitialized, isInitializing, initAuth } = useAuthStore();
  const [called, setCalled] = useState(false);

  useEffect(() => {
    if (!called) {
      setCalled(true);
      initAuth();
    }
  }, [called, initAuth]);

  if (!isInitialized || isInitializing) {
    return <PageLoader />;
  }

  return <>{children}</>;
};

function App() {
  return (
    <InitGate>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          {/* Public Auth Routes */}
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
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
    </InitGate>
  );
}

export default App;
