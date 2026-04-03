import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuthStore } from '@/stores/authStore';
import { authApi } from '@/api/auth';
import { toast } from 'sonner';
import { 
  User, 
  Lock, 
  Settings as SettingsIcon, 
  DollarSign, 
  Moon, 
  Sun, 
  Monitor,
  Loader2,
  ShieldCheck,
  UserCircle
} from 'lucide-react';
import { cn } from '@/lib/utils';

const profileSchema = z.object({
  name: z.string().min(2, 'Name too short').max(100),
  currency: z.string().min(1, 'Currency required'),
});

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password required'),
  newPassword: z.string().min(8, 'New password must be at least 8 characters'),
  confirmPassword: z.string(),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
});

const SettingsPage: React.FC = () => {
  const { user, updateUser } = useAuthStore();
  const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'appearance'>('profile');

  const profileForm = useForm({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      name: user?.name || '',
      currency: user?.currency || 'INR',
    },
  });

  const passwordForm = useForm({
    resolver: zodResolver(passwordSchema),
  });

  // Sync profile form with user store (which may be loaded async)
  useEffect(() => {
    if (user?.name) {
      profileForm.reset({ name: user.name, currency: user.currency || 'INR' });
    }
  }, [user]);

  const onUpdateProfile = async (data: any) => {
    try {
      const response = await authApi.updateProfile(data);
      updateUser(response.data);
      toast.success('Profile updated successfully');
    } catch (error: any) {
      toast.error(error.message || 'Failed to update profile');
    }
  };

  const onChangePassword = async (data: any) => {
    try {
      await authApi.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      toast.success('Password changed successfully');
      passwordForm.reset();
    } catch (error: any) {
      toast.error(error.message || 'Failed to change password');
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-500 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Settings</h1>
        <p className="text-slate-500 dark:text-slate-400">Manage your account preferences and security.</p>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        {/* Sidebar Tabs */}
        <aside className="w-full md:w-64 shrink-0 space-y-1">
          <button
            onClick={() => setActiveTab('profile')}
            className={cn(
              "w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold transition-all",
              activeTab === 'profile' 
                ? "bg-blue-600 text-white shadow-lg shadow-blue-500/20" 
                : "text-slate-600 dark:text-slate-400 hover:bg-white dark:hover:bg-slate-900 border border-transparent hover:border-slate-200 dark:hover:border-slate-800"
            )}
          >
            <User className="w-5 h-5" />
            Profile
          </button>
          <button
            onClick={() => setActiveTab('security')}
            className={cn(
              "w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold transition-all",
              activeTab === 'security' 
                ? "bg-blue-600 text-white shadow-lg shadow-blue-500/20" 
                : "text-slate-600 dark:text-slate-400 hover:bg-white dark:hover:bg-slate-900 border border-transparent hover:border-slate-200 dark:hover:border-slate-800"
            )}
          >
            <ShieldCheck className="w-5 h-5" />
            Security
          </button>
          <button
            onClick={() => setActiveTab('appearance')}
            className={cn(
              "w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-bold transition-all",
              activeTab === 'appearance' 
                ? "bg-blue-600 text-white shadow-lg shadow-blue-500/20" 
                : "text-slate-600 dark:text-slate-400 hover:bg-white dark:hover:bg-slate-900 border border-transparent hover:border-slate-200 dark:hover:border-slate-800"
            )}
          >
            <SettingsIcon className="w-5 h-5" />
            Appearance
          </button>
        </aside>

        {/* Content Area */}
        <div className="flex-1 bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
          {activeTab === 'profile' && (
            <div className="p-8 space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div className="flex items-center gap-6 pb-8 border-b border-slate-100 dark:border-slate-800">
                <div className="w-20 h-20 rounded-3xl bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center text-blue-600 dark:text-blue-400 border border-blue-100 dark:border-blue-900/30">
                  <UserCircle className="w-12 h-12" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-slate-900 dark:text-white">{user?.name}</h3>
                  <p className="text-sm text-slate-500 font-medium">{user?.email}</p>
                </div>
              </div>

              <form onSubmit={profileForm.handleSubmit(onUpdateProfile)} className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-sm font-bold text-slate-700 dark:text-slate-300">Full Name</label>
                    <input
                      {...profileForm.register('name')}
                      className="w-full px-4 py-2 bg-slate-50 dark:bg-slate-800 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none dark:text-white"
                    />
                    {profileForm.formState.errors.name && (
                      <p className="text-xs text-red-500">{profileForm.formState.errors.name.message}</p>
                    )}
                  </div>
                  <div className="space-y-2">
                    <label className="text-sm font-bold text-slate-700 dark:text-slate-300">Default Currency</label>
                    <select
                      {...profileForm.register('currency')}
                      className="w-full px-4 py-2 bg-slate-50 dark:bg-slate-800 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none dark:text-white appearance-none cursor-pointer"
                    >
                      <option value="INR">INR (₹)</option>
                      <option value="USD">USD ($)</option>
                      <option value="EUR">EUR (€)</option>
                      <option value="GBP">GBP (£)</option>
                    </select>
                  </div>
                </div>

                <div className="pt-4">
                  <button
                    type="submit"
                    disabled={profileForm.formState.isSubmitting}
                    className="px-6 py-2.5 bg-blue-600 text-white rounded-xl font-bold shadow-lg shadow-blue-500/20 hover:bg-blue-700 disabled:bg-blue-400 transition-all flex items-center gap-2"
                  >
                    {profileForm.formState.isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
                    Save Changes
                  </button>
                </div>
              </form>
            </div>
          )}

          {activeTab === 'security' && (
            <div className="p-8 space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-1">Update Password</h3>
                <p className="text-sm text-slate-500">Ensure your account is using a long, random password to stay secure.</p>
              </div>

              <form onSubmit={passwordForm.handleSubmit(onChangePassword)} className="space-y-6 max-w-md">
                <div className="space-y-2">
                  <label className="text-sm font-bold text-slate-700 dark:text-slate-300">Current Password</label>
                  <input
                    {...passwordForm.register('currentPassword')}
                    type="password"
                    className="w-full px-4 py-2 bg-slate-50 dark:bg-slate-800 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none dark:text-white"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-bold text-slate-700 dark:text-slate-300">New Password</label>
                  <input
                    {...passwordForm.register('newPassword')}
                    type="password"
                    className="w-full px-4 py-2 bg-slate-50 dark:bg-slate-800 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none dark:text-white"
                  />
                  {passwordForm.formState.errors.newPassword && (
                    <p className="text-xs text-red-500">{passwordForm.formState.errors.newPassword.message}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-bold text-slate-700 dark:text-slate-300">Confirm New Password</label>
                  <input
                    {...passwordForm.register('confirmPassword')}
                    type="password"
                    className="w-full px-4 py-2 bg-slate-50 dark:bg-slate-800 border-none rounded-xl focus:ring-2 focus:ring-blue-500 outline-none dark:text-white"
                  />
                  {passwordForm.formState.errors.confirmPassword && (
                    <p className="text-xs text-red-500">{passwordForm.formState.errors.confirmPassword.message}</p>
                  )}
                </div>

                <div className="pt-4">
                  <button
                    type="submit"
                    disabled={passwordForm.formState.isSubmitting}
                    className="px-6 py-2.5 bg-blue-600 text-white rounded-xl font-bold shadow-lg shadow-blue-500/20 hover:bg-blue-700 disabled:bg-blue-400 transition-all flex items-center gap-2"
                  >
                    {passwordForm.formState.isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
                    Change Password
                  </button>
                </div>
              </form>
            </div>
          )}

          {activeTab === 'appearance' && (
            <div className="p-8 space-y-8 animate-in slide-in-from-right-4 duration-300">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white mb-1">Theme Preferences</h3>
                <p className="text-sm text-slate-500">Choose how FinTrack AI looks on your device.</p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <button className="p-4 rounded-2xl border-2 border-blue-600 bg-white text-slate-900 flex flex-col items-center gap-3 transition-all ring-4 ring-blue-500/10">
                  <Sun className="w-6 h-6 text-blue-600" />
                  <span className="text-sm font-bold">Light Mode</span>
                </button>
                <button className="p-4 rounded-2xl border-2 border-slate-200 dark:border-slate-800 bg-slate-950 text-white flex flex-col items-center gap-3 hover:border-slate-300 transition-all">
                  <Moon className="w-6 h-6 text-slate-400" />
                  <span className="text-sm font-bold text-slate-400">Dark Mode</span>
                </button>
                <button className="p-4 rounded-2xl border-2 border-slate-200 dark:border-slate-800 bg-slate-50 flex flex-col items-center gap-3 hover:border-slate-300 transition-all">
                  <Monitor className="w-6 h-6 text-slate-500" />
                  <span className="text-sm font-bold text-slate-500">System</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SettingsPage;
