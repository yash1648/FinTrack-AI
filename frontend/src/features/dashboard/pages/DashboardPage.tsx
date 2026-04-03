import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '@/api/dashboard';
import { useAuthStore } from '@/stores/authStore';
import { formatCurrency, formatDate, cn } from '@/lib/utils';
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  ArrowUpRight,
  ArrowDownRight,
  Loader2,
  AlertCircle,
  PlusCircle,
  BarChart3,
  Receipt,
} from 'lucide-react';
import { 
  PieChart, 
  Pie, 
  Cell, 
  ResponsiveContainer, 
  Tooltip, 
  Legend 
} from 'recharts';
import { Link } from 'react-router-dom';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

const DashboardPage: React.FC = () => {
  const { user } = useAuthStore();
  const { data, isLoading, isError } = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: dashboardApi.getSummary,
  });

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <Loader2 className="w-10 h-10 animate-spin text-blue-600" />
        <p className="text-slate-500 font-medium">Loading your financial summary...</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4 text-center">
        <div className="w-16 h-16 bg-red-100 dark:bg-red-900/20 rounded-full flex items-center justify-center text-red-600 dark:text-red-400">
          <AlertCircle className="w-8 h-8" />
        </div>
        <h3 className="text-lg font-bold text-slate-900 dark:text-white">Failed to load dashboard</h3>
        <p className="text-slate-500 max-w-xs">There was an error fetching your data. Please try again later.</p>
        <button 
          onClick={() => window.location.reload()}
          className="px-4 py-2 bg-slate-900 dark:bg-white text-white dark:text-slate-900 rounded-lg font-medium"
        >
          Retry
        </button>
      </div>
    );
  }

  const summary = data?.data || {};
  const categoryData = Object.entries(summary.categoryBreakdown || {}).map(([name, value]) => ({
    name,
    value,
  }));

  const stats = [
    {
      label: 'Total Balance',
      value: summary.balance || 0,
      icon: Wallet,
      color: 'blue',
      trend: null,
    },
    {
      label: 'Monthly Income',
      value: summary.totalIncome || 0,
      icon: TrendingUp,
      color: 'emerald',
      trend: '+12%',
    },
    {
      label: 'Monthly Expenses',
      value: summary.totalExpenses || 0,
      icon: TrendingDown,
      color: 'rose',
      trend: '-5%',
    },
  ];

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Hello, {user?.name.split(' ')[0]}!
          </h1>
          <p className="text-slate-500 dark:text-slate-400">
            Here's what's happening with your money this month.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Link
            to="/transactions"
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-xl font-semibold shadow-lg shadow-blue-500/20 hover:bg-blue-700 transition-all"
          >
            <PlusCircle className="w-5 h-5" />
            Add Transaction
          </Link>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {stats.map((stat) => (
          <div
            key={stat.label}
            className="bg-white dark:bg-slate-900 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm"
          >
            <div className="flex items-start justify-between">
              <div className={cn(
                "p-3 rounded-xl",
                stat.color === 'blue' && "bg-blue-50 dark:bg-blue-900/20 text-blue-600 dark:text-blue-400",
                stat.color === 'emerald' && "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600 dark:text-emerald-400",
                stat.color === 'rose' && "bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400"
              )}>
                <stat.icon className="w-6 h-6" />
              </div>
              {stat.trend && (
                <span className={cn(
                  "flex items-center gap-1 text-xs font-bold px-2 py-1 rounded-full",
                  stat.trend.startsWith('+') 
                    ? "bg-emerald-50 dark:bg-emerald-900/20 text-emerald-600" 
                    : "bg-rose-50 dark:bg-rose-900/20 text-rose-600"
                )}>
                  {stat.trend.startsWith('+') ? <ArrowUpRight className="w-3 h-3" /> : <ArrowDownRight className="w-3 h-3" />}
                  {stat.trend}
                </span>
              )}
            </div>
            <div className="mt-4">
              <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{stat.label}</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">
                {formatCurrency(stat.value, user?.currency)}
              </h3>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Recent Transactions */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden flex flex-col">
          <div className="p-6 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <h3 className="font-bold text-slate-900 dark:text-white">Recent Transactions</h3>
            <Link to="/transactions" className="text-sm font-semibold text-blue-600 hover:text-blue-500">
              View All
            </Link>
          </div>
          <div className="flex-1">
            {summary.recentTransactions?.length > 0 ? (
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {summary.recentTransactions.map((tx: any) => (
                  <div key={tx.id} className="p-4 flex items-center justify-between hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                    <div className="flex items-center gap-4">
                      <div className={cn(
                        "w-10 h-10 rounded-full flex items-center justify-center",
                        tx.type === 'INCOME' ? "bg-emerald-100 dark:bg-emerald-900/20 text-emerald-600" : "bg-rose-100 dark:bg-rose-900/20 text-rose-600"
                      )}>
                        {tx.type === 'INCOME' ? <TrendingUp className="w-5 h-5" /> : <TrendingDown className="w-5 h-5" />}
                      </div>
                      <div>
                        <p className="text-sm font-bold text-slate-900 dark:text-white">{tx.description || tx.category.name}</p>
                        <p className="text-xs text-slate-500 dark:text-slate-400">{formatDate(tx.date)}</p>
                      </div>
                    </div>
                    <span className={cn(
                      "text-sm font-bold",
                      tx.type === 'INCOME' ? "text-emerald-600" : "text-rose-600"
                    )}>
                      {tx.type === 'INCOME' ? '+' : '-'}{formatCurrency(tx.amount, user?.currency)}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-12 text-center">
                <p className="text-slate-500">No transactions found for this month.</p>
              </div>
            )}
          </div>
        </div>

        {/* Category Breakdown */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col">
          <div className="p-6 border-b border-slate-200 dark:border-slate-800">
            <h3 className="font-bold text-slate-900 dark:text-white">Spending by Category</h3>
          </div>
          <div className="flex-1 h-[350px] p-6">
            {categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {categoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip 
                    formatter={(value: number) => formatCurrency(value, user?.currency)}
                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                  />
                  <Legend verticalAlign="bottom" height={36} />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-center p-8">
                <div className="w-16 h-16 bg-slate-100 dark:bg-slate-800 rounded-full flex items-center justify-center text-slate-400 mb-4">
                  <BarChart3 className="w-8 h-8" />
                </div>
                <p className="text-slate-500">Add some expenses to see your category breakdown.</p>
              </div>
            )}
          </div>
        </div>
      </div>
      
      {/* Budget Alerts */}
      {summary.budgetAlerts?.length > 0 && (
        <div className="bg-amber-50 dark:bg-amber-900/10 border border-amber-200 dark:border-amber-900/30 rounded-2xl p-6">
          <div className="flex items-center gap-3 mb-4">
            <AlertCircle className="w-6 h-6 text-amber-600" />
            <h3 className="font-bold text-amber-900 dark:text-amber-400">Budget Alerts</h3>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {summary.budgetAlerts.map((alert: any) => (
              <div key={alert.category} className="bg-white dark:bg-slate-900 p-4 rounded-xl shadow-sm border border-amber-100 dark:border-amber-900/20">
                <div className="flex justify-between items-center mb-2">
                  <span className="text-sm font-bold text-slate-900 dark:text-white">{alert.category}</span>
                  <span className="text-xs font-bold text-amber-600">{Math.round(alert.percentage)}%</span>
                </div>
                <div className="w-full bg-slate-100 dark:bg-slate-800 rounded-full h-2 overflow-hidden">
                  <div 
                    className={cn(
                      "h-full rounded-full transition-all",
                      alert.percentage >= 100 ? "bg-red-500" : "bg-amber-500"
                    )}
                    style={{ width: `${Math.min(alert.percentage, 100)}%` }}
                  />
                </div>
                <p className="text-xs text-slate-500 mt-2">
                  Spent {formatCurrency(alert.spent, user?.currency)} of {formatCurrency(alert.limit, user?.currency)}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
