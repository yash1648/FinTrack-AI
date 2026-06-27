import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { authApi } from '@/api/auth';
import { Loader2, Mail, ArrowLeft } from 'lucide-react';

const forgotSchema = z.object({
  email: z.string().email('Please enter a valid email'),
});

type ForgotFormValues = z.infer<typeof forgotSchema>;

const ForgotPasswordPage: React.FC = () => {
  const [sent, setSent] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotFormValues>({
    resolver: zodResolver(forgotSchema),
  });

  const onSubmit = async (data: ForgotFormValues) => {
    try {
      await authApi.forgotPassword(data.email);
      setSent(true);
      toast.success('If your email is registered, a reset link has been sent.');
    } catch (error: any) {
      toast.error(error.message || 'Failed to send reset email. Please try again.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h2 className="text-xl font-bold text-slate-900 dark:text-white">Reset password</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          {sent
            ? 'Check your email for the reset link.'
            : "Enter your email and we'll send you a reset link."}
        </p>
      </div>

      {!sent ? (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-700 dark:text-slate-300">Email</label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                {...register('email')}
                type="email"
                className="w-full pl-10 pr-4 py-2 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition-all dark:text-white"
                placeholder="name@example.com"
              />
            </div>
            {errors.email && <p className="text-xs text-red-500 mt-1">{errors.email.message}</p>}
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white rounded-lg font-semibold shadow-lg shadow-blue-500/20 transition-all flex items-center justify-center gap-2"
          >
            {isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
            Send Reset Link
          </button>
        </form>
      ) : (
        <div className="text-center py-8">
          <div className="w-16 h-16 bg-emerald-100 dark:bg-emerald-900/20 rounded-full flex items-center justify-center text-emerald-600 mx-auto mb-4">
            <Mail className="w-8 h-8" />
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            We've sent a password reset link to your email. Please check your inbox and follow the instructions.
          </p>
        </div>
      )}

      <div className="text-center text-sm">
        <Link
          to="/login"
          className="font-semibold text-blue-600 hover:text-blue-500 dark:text-blue-400 inline-flex items-center gap-1"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to login
        </Link>
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
