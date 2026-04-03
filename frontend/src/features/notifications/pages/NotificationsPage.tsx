import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '@/api/notifications';
import { 
  Bell, 
  CheckCheck, 
  Trash2, 
  Loader2, 
  Circle,
  AlertCircle,
  TrendingUp,
  Receipt,
  Tag,
  ChevronLeft,
  ChevronRight
} from 'lucide-react';
import { toast } from 'sonner';
import { formatDate, cn } from '@/lib/utils';

const NotificationsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [unreadOnly, setUnreadOnly] = useState(false);

  const { data: notificationsData, isLoading } = useQuery({
    queryKey: ['notifications', { page, unreadOnly }],
    queryFn: () => notificationsApi.getNotifications({ page, limit: 10, unread_only: unreadOnly }),
  });

  const markReadMutation = useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('Marked as read');
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('All notifications marked as read');
    },
  });

  const handleMarkRead = (id: string) => {
    markReadMutation.mutate(id);
  };

  const handleMarkAllRead = () => {
    markAllReadMutation.mutate();
  };

  const notifications = notificationsData?.data || [];
  const pagination = notificationsData?.pagination || { page: 1, totalPages: 1 };

  const getIcon = (type: string) => {
    switch (type) {
      case 'BUDGET_ALERT': return AlertCircle;
      case 'TRANSACTION': return Receipt;
      case 'CATEGORY': return Tag;
      default: return Bell;
    }
  };

  const getColor = (type: string) => {
    switch (type) {
      case 'BUDGET_ALERT': return 'text-amber-500 bg-amber-50 dark:bg-amber-900/20';
      case 'TRANSACTION': return 'text-emerald-500 bg-emerald-50 dark:bg-emerald-900/20';
      case 'CATEGORY': return 'text-blue-500 bg-blue-50 dark:bg-blue-900/20';
      default: return 'text-slate-500 bg-slate-50 dark:bg-slate-900/20';
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Notifications</h1>
          <p className="text-slate-500 dark:text-slate-400">Stay updated with your budget alerts and activity.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setUnreadOnly(!unreadOnly)}
            className={cn(
              "px-4 py-2 rounded-xl text-sm font-bold transition-all border",
              unreadOnly 
                ? "bg-blue-50 border-blue-200 text-blue-600 dark:bg-blue-900/20 dark:border-blue-800" 
                : "bg-white border-slate-200 text-slate-600 dark:bg-slate-900 dark:border-slate-800"
            )}
          >
            {unreadOnly ? 'Showing Unread' : 'Show Unread Only'}
          </button>
          <button
            onClick={handleMarkAllRead}
            disabled={markAllReadMutation.isPending}
            className="flex items-center gap-2 px-4 py-2 bg-slate-900 dark:bg-white text-white dark:text-slate-900 rounded-xl font-bold text-sm shadow-lg hover:opacity-90 transition-all disabled:opacity-50"
          >
            <CheckCheck className="w-4 h-4" />
            Mark All Read
          </button>
        </div>
      </div>

      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-20 flex flex-col items-center justify-center gap-4">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            <p className="text-slate-500 font-medium">Loading notifications...</p>
          </div>
        ) : notifications.length > 0 ? (
          <div className="divide-y divide-slate-100 dark:divide-slate-800">
            {notifications.map((notif: any) => {
              const Icon = getIcon(notif.type);
              const colorClass = getColor(notif.type);
              
              return (
                <div 
                  key={notif.id} 
                  className={cn(
                    "p-6 flex items-start gap-4 transition-colors group",
                    !notif.is_read ? "bg-blue-50/30 dark:bg-blue-900/10" : "hover:bg-slate-50 dark:hover:bg-slate-800/50"
                  )}
                >
                  <div className={cn("w-12 h-12 rounded-2xl flex items-center justify-center shrink-0 shadow-sm", colorClass)}>
                    <Icon className="w-6 h-6" />
                  </div>
                  
                  <div className="flex-1 space-y-1">
                    <div className="flex items-center justify-between">
                      <h4 className={cn("text-base font-bold", !notif.is_read ? "text-slate-900 dark:text-white" : "text-slate-600 dark:text-slate-400")}>
                        {notif.title}
                      </h4>
                      <span className="text-xs text-slate-400 font-medium">{formatDate(notif.created_at)}</span>
                    </div>
                    <p className={cn("text-sm leading-relaxed", !notif.is_read ? "text-slate-700 dark:text-slate-300" : "text-slate-500 dark:text-slate-400")}>
                      {notif.body}
                    </p>
                    
                    {!notif.is_read && (
                      <button
                        onClick={() => handleMarkRead(notif.id)}
                        className="text-xs font-bold text-blue-600 hover:text-blue-500 mt-2 flex items-center gap-1 transition-colors"
                      >
                        <CheckCheck className="w-3.5 h-3.5" />
                        Mark as read
                      </button>
                    )}
                  </div>
                  
                  {!notif.is_read && (
                    <div className="w-2 h-2 rounded-full bg-blue-500 mt-2 shrink-0" />
                  )}
                </div>
              );
            })}
          </div>
        ) : (
          <div className="p-20 text-center">
            <div className="w-16 h-16 bg-slate-100 dark:bg-slate-800 rounded-full flex items-center justify-center text-slate-400 mx-auto mb-4">
              <Bell className="w-8 h-8" />
            </div>
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">All caught up!</h3>
            <p className="text-slate-500 max-w-xs mx-auto">No new notifications at the moment.</p>
          </div>
        )}

        {/* Pagination */}
        {pagination.totalPages > 1 && (
          <div className="p-4 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-800/20">
            <span className="text-sm text-slate-500 font-medium">
              Page {pagination.page} of {pagination.totalPages}
            </span>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 1}
                onClick={() => setPage(page - 1)}
                className="p-2 text-slate-600 dark:text-slate-400 hover:bg-white dark:hover:bg-slate-800 rounded-lg disabled:opacity-50 transition-all border border-transparent hover:border-slate-200 dark:hover:border-slate-700 shadow-sm"
              >
                <ChevronLeft className="w-5 h-5" />
              </button>
              <button
                disabled={page === pagination.totalPages}
                onClick={() => setPage(page + 1)}
                className="p-2 text-slate-600 dark:text-slate-400 hover:bg-white dark:hover:bg-slate-800 rounded-lg disabled:opacity-50 transition-all border border-transparent hover:border-slate-200 dark:hover:border-slate-700 shadow-sm"
              >
                <ChevronRight className="w-5 h-5" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;
