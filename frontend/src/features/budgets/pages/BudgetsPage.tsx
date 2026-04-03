import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { budgetsApi } from '@/api/budgets';
import { categoriesApi } from '@/api/categories';
import { useAuthStore } from '@/stores/authStore';
import { formatCurrency, cn } from '@/lib/utils';
import { 
  Plus, 
  Trash2, 
  Edit2, 
  Wallet, 
  Loader2, 
  AlertCircle,
  TrendingUp,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  ChevronLeft,
  ChevronRight,
  X
} from 'lucide-react';
import { toast } from 'sonner';

const BudgetsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState<any>(null);
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());

  const { data: budgetsData, isLoading } = useQuery({
    queryKey: ['budgets', { month: selectedMonth, year: selectedYear }],
    queryFn: () => budgetsApi.getBudgets({ month: selectedMonth, year: selectedYear }),
  });

  const { data: categoriesData } = useQuery({
    queryKey: ['categories', 'all'],
    queryFn: categoriesApi.getCategories,
  });

  const deleteMutation = useMutation({
    mutationFn: budgetsApi.deleteBudget,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['budgets'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast.success('Budget deleted');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to delete budget');
    },
  });

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this budget?')) {
      deleteMutation.mutate(id);
    }
  };

  const budgets = budgetsData?.data || [];
  const categories = categoriesData?.data || [];

  const handleMonthChange = (direction: 'prev' | 'next') => {
    if (direction === 'prev') {
      if (selectedMonth === 1) {
        setSelectedMonth(12);
        setSelectedYear(selectedYear - 1);
      } else {
        setSelectedMonth(selectedMonth - 1);
      }
    } else {
      if (selectedMonth === 12) {
        setSelectedMonth(1);
        setSelectedYear(selectedYear + 1);
      } else {
        setSelectedMonth(selectedMonth + 1);
      }
    }
  };

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Budgets</h1>
          <p className="text-slate-500 dark:text-slate-400">Set spending limits for categories and track progress.</p>
        </div>
        <button
          onClick={() => {
            setEditingBudget(null);
            setIsFormOpen(true);
          }}
          className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-xl font-semibold shadow-lg shadow-blue-500/20 hover:bg-blue-700 transition-all"
        >
          <Plus className="w-5 h-5" />
          Set Budget
        </button>
      </div>

      {/* Month Selector */}
      <div className="bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm flex items-center justify-between">
        <button 
          onClick={() => handleMonthChange('prev')}
          className="p-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-600 dark:text-slate-400"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <div className="text-center">
          <h3 className="text-lg font-bold text-slate-900 dark:text-white">{monthNames[selectedMonth - 1]} {selectedYear}</h3>
          <p className="text-xs font-medium text-slate-500 uppercase tracking-widest mt-0.5">Active Period</p>
        </div>
        <button 
          onClick={() => handleMonthChange('next')}
          className="p-2 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-slate-600 dark:text-slate-400"
        >
          <ChevronRight className="w-5 h-5" />
        </button>
      </div>

      {isLoading ? (
        <div className="p-20 flex flex-col items-center justify-center gap-4">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          <p className="text-slate-500 font-medium">Loading budgets...</p>
        </div>
      ) : budgets.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {budgets.map((budget: any) => {
            const percentage = parseFloat(budget.percentage);
            const status = budget.status; // ok, warning, exceeded
            
            return (
              <div key={budget.id} className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden flex flex-col group transition-all hover:shadow-md">
                <div className="p-6 flex-1 space-y-6">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-blue-600 dark:text-blue-400">
                        <Wallet className="w-5 h-5" />
                      </div>
                      <div>
                        <h4 className="font-bold text-slate-900 dark:text-white leading-none">{budget.category.name}</h4>
                        <span className={cn(
                          "text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full mt-2 inline-block",
                          status === 'ok' ? "bg-emerald-50 text-emerald-600 dark:bg-emerald-900/20" :
                          status === 'warning' ? "bg-amber-50 text-amber-600 dark:bg-amber-900/20" :
                          "bg-rose-50 text-rose-600 dark:bg-rose-900/20"
                        )}>
                          {status}
                        </span>
                      </div>
                    </div>
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button 
                        onClick={() => {
                          setEditingBudget(budget);
                          setIsFormOpen(true);
                        }}
                        className="p-1.5 text-slate-400 hover:text-blue-600 rounded-lg"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button 
                        onClick={() => handleDelete(budget.id)}
                        className="p-1.5 text-slate-400 hover:text-red-600 rounded-lg"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  <div className="space-y-3">
                    <div className="flex items-end justify-between">
                      <div className="space-y-1">
                        <p className="text-xs font-medium text-slate-500">Spent / Limit</p>
                        <div className="flex items-baseline gap-1.5">
                          <span className="text-lg font-bold text-slate-900 dark:text-white">
                            {formatCurrency(budget.spent, user?.currency)}
                          </span>
                          <span className="text-xs text-slate-400">/ {formatCurrency(budget.limitAmount, user?.currency)}</span>
                        </div>
                      </div>
                      <div className="text-right space-y-1">
                        <p className="text-xs font-medium text-slate-500">Remaining</p>
                        <span className={cn(
                          "text-sm font-bold",
                          percentage >= 100 ? "text-rose-600" : "text-emerald-600"
                        )}>
                          {formatCurrency(budget.remaining, user?.currency)}
                        </span>
                      </div>
                    </div>

                    <div className="w-full h-2.5 bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden">
                      <div 
                        className={cn(
                          "h-full rounded-full transition-all duration-500",
                          status === 'ok' ? "bg-emerald-500" :
                          status === 'warning' ? "bg-amber-500" :
                          "bg-red-500"
                        )}
                        style={{ width: `${Math.min(percentage, 100)}%` }}
                      />
                    </div>
                  </div>
                </div>
                
                <div className={cn(
                  "px-6 py-3 border-t text-xs font-semibold flex items-center gap-2",
                  status === 'ok' ? "bg-emerald-50/30 text-emerald-600 border-emerald-100 dark:border-emerald-900/10" :
                  status === 'warning' ? "bg-amber-50/30 text-amber-600 border-amber-100 dark:border-amber-900/10" :
                  "bg-rose-50/30 text-rose-600 border-rose-100 dark:border-rose-900/10"
                )}>
                  {status === 'ok' ? <CheckCircle2 className="w-3.5 h-3.5" /> :
                   status === 'warning' ? <AlertTriangle className="w-3.5 h-3.5" /> :
                   <XCircle className="w-3.5 h-3.5" />}
                  {status === 'ok' ? 'You are within your budget.' :
                   status === 'warning' ? 'Watch out, you are close to the limit.' :
                   'Budget exceeded for this category.'}
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="p-20 text-center bg-white dark:bg-slate-900 rounded-3xl border-2 border-dashed border-slate-200 dark:border-slate-800">
          <div className="w-16 h-16 bg-blue-50 dark:bg-blue-900/20 rounded-full flex items-center justify-center text-blue-600 dark:text-blue-400 mx-auto mb-4">
            <TrendingUp className="w-8 h-8" />
          </div>
          <h3 className="text-xl font-bold text-slate-900 dark:text-white">No budgets set for this month</h3>
          <p className="text-slate-500 max-w-sm mx-auto mt-2">Start controlling your expenses by setting up a budget for your favorite categories.</p>
          <button
            onClick={() => setIsFormOpen(true)}
            className="mt-6 px-6 py-2.5 bg-blue-600 text-white rounded-xl font-bold shadow-lg shadow-blue-500/20 hover:bg-blue-700 transition-all"
          >
            Create My First Budget
          </button>
        </div>
      )}

      {/* Budget Form Modal */}
      {isFormOpen && (
        <BudgetForm
          budget={editingBudget}
          categories={categories}
          month={selectedMonth}
          year={selectedYear}
          onClose={() => setIsFormOpen(false)}
        />
      )}
    </div>
  );
};

interface BudgetFormProps {
  budget?: any;
  categories: any[];
  month: number;
  year: number;
  onClose: () => void;
}

const BudgetForm: React.FC<BudgetFormProps> = ({ budget, categories, month, year, onClose }) => {
  const queryClient = useQueryClient();
  const isEditing = !!budget;
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    category_id: budget?.category?.id || '',
    limit_amount: budget?.limitAmount?.toString() || '',
    month: month,
    year: year,
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      if (isEditing) {
        await budgetsApi.updateBudget(budget.id, { limitAmount: formData.limit_amount });
      } else {
        await budgetsApi.createBudget(formData);
      }
      queryClient.invalidateQueries({ queryKey: ['budgets'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast.success(isEditing ? 'Budget updated' : 'Budget set successfully');
      onClose();
    } catch (error: any) {
      toast.error(error.message || 'Failed to save budget');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-300">
      <div className="bg-white dark:bg-slate-900 w-full max-w-md rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 overflow-hidden animate-in zoom-in-95 duration-300">
        <div className="p-6 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <h3 className="text-xl font-bold text-slate-900 dark:text-white">
            {isEditing ? 'Edit Budget' : 'Set Category Budget'}
          </h3>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-600 rounded-lg transition-all">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">Category</label>
            <select
              disabled={isEditing}
              value={formData.category_id}
              onChange={(e) => setFormData({ ...formData, category_id: e.target.value })}
              className="w-full px-4 py-2 bg-slate-50 dark:bg-slate-800 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none dark:text-white transition-all appearance-none disabled:opacity-50"
              required
            >
              <option value="">Select Category</option>
              {categories.map((cat: any) => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">Limit Amount</label>
            <div className="relative">
              <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 font-bold">₹</span>
              <input
                type="number"
                value={formData.limit_amount}
                onChange={(e) => setFormData({ ...formData, limit_amount: e.target.value })}
                className="w-full pl-8 pr-4 py-2 bg-slate-50 dark:bg-slate-800 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none dark:text-white transition-all"
                placeholder="0.00"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="p-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-100 dark:border-slate-800">
              <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Month</p>
              <p className="text-sm font-bold text-slate-700 dark:text-slate-300">
                {["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][formData.month - 1]}
              </p>
            </div>
            <div className="p-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-100 dark:border-slate-800">
              <p className="text-[10px] uppercase font-bold text-slate-400 tracking-wider">Year</p>
              <p className="text-sm font-bold text-slate-700 dark:text-slate-300">{formData.year}</p>
            </div>
          </div>

          <div className="flex gap-4 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-3 px-4 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-bold rounded-xl hover:bg-slate-200 transition-all"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-[2] py-3 px-4 bg-blue-600 text-white font-bold rounded-xl shadow-lg shadow-blue-500/20 hover:bg-blue-700 disabled:bg-blue-400 transition-all flex items-center justify-center gap-2"
            >
              {loading && <Loader2 className="w-5 h-5 animate-spin" />}
              {isEditing ? 'Update Limit' : 'Set Budget'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default BudgetsPage;
