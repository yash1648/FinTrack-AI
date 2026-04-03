import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { analysisApi } from '@/api/analysis';
import { useAuthStore } from '@/stores/authStore';
import { formatCurrency, cn } from '@/lib/utils';
import { 
  Lightbulb, 
  AlertTriangle, 
  TrendingUp, 
  Loader2, 
  CheckCircle2,
  Calendar,
  Sparkles,
  ArrowRight,
  RefreshCw,
  Info
} from 'lucide-react';

const InsightsPage: React.FC = () => {
  const { user } = useAuthStore();
  
  const { data: insightsData, isLoading: isInsightsLoading, refetch: refetchInsights } = useQuery({
    queryKey: ['analysis', 'insights'],
    queryFn: analysisApi.getInsights,
  });

  const { data: anomaliesData, isLoading: isAnomaliesLoading } = useQuery({
    queryKey: ['analysis', 'anomalies'],
    queryFn: analysisApi.getAnomalies,
  });

  const { data: projectionData, isLoading: isProjectionLoading } = useQuery({
    queryKey: ['analysis', 'projection'],
    queryFn: analysisApi.getProjection,
  });

  const isLoading = isInsightsLoading || isAnomaliesLoading || isProjectionLoading;

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <Loader2 className="w-10 h-10 animate-spin text-blue-600" />
        <p className="text-slate-500 font-medium">Analyzing your financial patterns with AI...</p>
      </div>
    );
  }

  const insights = insightsData?.data || {};
  const anomalies = anomaliesData?.data || [];
  const projection = projectionData?.data || {};

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
            AI Insights & Analysis <Sparkles className="w-5 h-5 text-blue-500" />
          </h1>
          <p className="text-slate-500 dark:text-slate-400">Advanced AI analysis of your spending habits.</p>
        </div>
        <button 
          onClick={() => refetchInsights()}
          className="flex items-center gap-2 px-4 py-2 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 rounded-xl font-bold text-sm border border-slate-200 dark:border-slate-800 shadow-sm hover:bg-slate-50 transition-all"
        >
          <RefreshCw className="w-4 h-4" />
          Refresh Analysis
        </button>
      </div>

      {!insights.sufficient ? (
        <div className="bg-blue-50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-900/20 rounded-3xl p-8 text-center max-w-2xl mx-auto">
          <div className="w-16 h-16 bg-blue-100 dark:bg-blue-900/30 rounded-full flex items-center justify-center text-blue-600 mx-auto mb-6">
            <Info className="w-8 h-8" />
          </div>
          <h3 className="text-xl font-bold text-slate-900 dark:text-white mb-2">More Data Needed</h3>
          <p className="text-slate-600 dark:text-slate-400 leading-relaxed">
            {insights.message || "Our AI needs a few more transactions to start identifying patterns and generating personalized insights for you. Keep tracking your daily expenses!"}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Insights Panel */}
          <div className="lg:col-span-2 space-y-8">
            {/* Patterns & Recommendations */}
            <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
              <div className="p-6 border-b border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20">
                <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <Lightbulb className="w-5 h-5 text-amber-500" />
                  Smart Recommendations
                </h3>
              </div>
              <div className="p-6 space-y-4">
                {insights.recommendations?.map((rec: string, i: number) => (
                  <div key={i} className="flex gap-4 p-4 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800 hover:border-blue-200 dark:hover:border-blue-900/30 transition-all">
                    <div className="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center shrink-0 text-blue-600 text-xs font-bold">
                      {i + 1}
                    </div>
                    <p className="text-sm text-slate-700 dark:text-slate-300 font-medium leading-relaxed">{rec}</p>
                  </div>
                ))}
              </div>
            </div>

            {/* Anomalies Table */}
            <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
              <div className="p-6 border-b border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 flex items-center justify-between">
                <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5 text-rose-500" />
                  Detected Anomalies
                </h3>
                <span className="text-xs font-bold px-2 py-1 rounded-full bg-rose-50 dark:bg-rose-900/20 text-rose-600">
                  {anomalies.length} Found
                </span>
              </div>
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {anomalies.length > 0 ? (
                  anomalies.map((ano: any, i: number) => (
                    <div key={i} className="p-6 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-bold text-slate-900 dark:text-white">{ano.category}</span>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-500">
                            {ano.date}
                          </span>
                        </div>
                        <p className="text-sm text-slate-500 dark:text-slate-400">{ano.reason}</p>
                      </div>
                      <div className="flex items-center gap-4 text-right">
                        <div>
                          <p className="text-sm font-bold text-rose-600">{formatCurrency(ano.amount, user?.currency)}</p>
                          <p className="text-[10px] font-bold text-slate-400">AVG: {formatCurrency(ano.average, user?.currency)}</p>
                        </div>
                        <div className="px-3 py-1 bg-rose-50 dark:bg-rose-900/20 rounded-lg text-rose-600 font-bold text-xs">
                          {ano.deviation}
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-12 text-center">
                    <div className="w-12 h-12 bg-emerald-50 dark:bg-emerald-900/20 rounded-full flex items-center justify-center text-emerald-600 mx-auto mb-4">
                      <CheckCircle2 className="w-6 h-6" />
                    </div>
                    <p className="text-slate-500 font-medium">No unusual spending patterns detected!</p>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Sidebar Panels */}
          <div className="space-y-8">
            {/* Projection Card */}
            <div className="bg-slate-900 dark:bg-slate-900 rounded-3xl p-8 text-white shadow-xl shadow-blue-900/20 relative overflow-hidden group">
              <div className="absolute top-0 right-0 w-32 h-32 bg-blue-500/10 rounded-full -translate-y-1/2 translate-x-1/2 blur-2xl group-hover:scale-150 transition-transform duration-700" />
              
              <div className="relative z-10 space-y-6">
                <div className="flex items-center justify-between">
                  <div className="w-12 h-12 bg-white/10 rounded-2xl flex items-center justify-center">
                    <TrendingUp className="w-6 h-6 text-blue-400" />
                  </div>
                  <span className="text-[10px] font-bold uppercase tracking-widest text-blue-400">End-of-Month Forecast</span>
                </div>
                
                <div className="space-y-2">
                  <p className="text-sm font-medium text-slate-400">Projected Monthly Expense</p>
                  <h3 className="text-4xl font-bold tracking-tight">
                    {formatCurrency(projection.projected || 0, user?.currency)}
                  </h3>
                </div>

                <div className="space-y-3">
                  <div className="flex justify-between text-xs font-bold text-slate-400">
                    <span>Spent so far</span>
                    <span>{Math.round((projection.days_elapsed / projection.days_in_month) * 100)}% of month</span>
                  </div>
                  <div className="w-full h-2 bg-white/5 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-blue-500 rounded-full shadow-[0_0_10px_rgba(59,130,246,0.5)]"
                      style={{ width: `${(projection.days_elapsed / projection.days_in_month) * 100}%` }}
                    />
                  </div>
                  <p className="text-xs text-slate-400 italic">
                    Based on your spending in the first {projection.days_elapsed} days.
                  </p>
                </div>

                <div className="pt-4 flex items-center gap-3">
                  <div className="flex-1 p-3 bg-white/5 rounded-2xl border border-white/5">
                    <p className="text-[10px] font-bold text-slate-500 uppercase">Actual Spent</p>
                    <p className="text-sm font-bold text-slate-200">{formatCurrency(projection.spent || 0, user?.currency)}</p>
                  </div>
                  <div className="flex-1 p-3 bg-white/5 rounded-2xl border border-white/5">
                    <p className="text-[10px] font-bold text-slate-500 uppercase">Daily Average</p>
                    <p className="text-sm font-bold text-slate-200">{formatCurrency((projection.spent / projection.days_elapsed) || 0, user?.currency)}</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Patterns Card */}
            <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm p-6 space-y-6">
              <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <Calendar className="w-5 h-5 text-blue-600" />
                Detected Patterns
              </h3>
              <div className="space-y-4">
                {insights.patterns?.map((pattern: string, i: number) => (
                  <div key={i} className="flex items-start gap-3">
                    <div className="w-2 h-2 rounded-full bg-blue-500 mt-1.5 shrink-0" />
                    <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed">{pattern}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default InsightsPage;
