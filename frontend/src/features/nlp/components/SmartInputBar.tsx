import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { nlpApi } from '@/api/nlp';
import { Sparkles, Loader2, Send } from 'lucide-react';
import { toast } from 'sonner';

interface SmartInputBarProps {
  onParsed: (draft: any) => void;
}

const SmartInputBar: React.FC<SmartInputBarProps> = ({ onParsed }) => {
  const [text, setText] = useState('');

  const mutation = useMutation({
    mutationFn: nlpApi.parse,
    onSuccess: (response: any) => {
      if (response.data.parsed) {
        onParsed(response.data.draft);
        setText('');
        toast.success('Transaction parsed successfully!');
      } else {
        toast.error(response.data.message || 'Could not parse transaction. Try something like: "Spent 500 on groceries today"');
      }
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to parse text');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (text.trim()) {
      mutation.mutate(text.trim());
    }
  };

  return (
    <form onSubmit={handleSubmit} className="relative group">
      <div className="absolute inset-y-0 left-4 flex items-center pointer-events-none">
        <Sparkles className={cn(
          "w-5 h-5 transition-colors",
          mutation.isPending ? "text-blue-500 animate-pulse" : "text-slate-400 group-focus-within:text-blue-500"
        )} />
      </div>
      <input
        type="text"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder='Try: "Spent 500 on groceries today" or "Received 50000 salary yesterday"'
        className="w-full pl-12 pr-24 py-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl shadow-sm focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all dark:text-white font-medium"
      />
      <div className="absolute inset-y-2 right-2 flex items-center">
        <button
          type="submit"
          disabled={mutation.isPending || !text.trim()}
          className="h-full px-6 bg-slate-900 dark:bg-blue-600 text-white rounded-xl font-bold text-sm flex items-center gap-2 hover:opacity-90 disabled:opacity-50 transition-all shadow-lg shadow-blue-500/10"
        >
          {mutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
          Parse
        </button>
      </div>
    </form>
  );
};

// Helper for cn in case it's not imported
import { cn } from '@/lib/utils';

export default SmartInputBar;
