import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { categoriesApi } from '@/api/categories';
import { 
  Plus, 
  Trash2, 
  Edit2, 
  Tag, 
  Loader2, 
  Lock,
  X,
  Check
} from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';

const CategoriesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [newName, setNewName] = useState('');

  const { data: categoriesData, isLoading } = useQuery({
    queryKey: ['categories', 'all'],
    queryFn: categoriesApi.getCategories,
  });

  const createMutation = useMutation({
    mutationFn: (name: string) => categoriesApi.createCategory({ name }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] });
      toast.success('Category created');
      setIsAdding(false);
      setNewName('');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to create category');
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) => 
      categoriesApi.updateCategory(id, { name }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] });
      toast.success('Category updated');
      setEditingId(null);
      setNewName('');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to update category');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: categoriesApi.deleteCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] });
      toast.success('Category deleted');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to delete category');
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (newName.trim()) {
      createMutation.mutate(newName.trim());
    }
  };

  const handleUpdate = (id: string) => {
    if (newName.trim()) {
      updateMutation.mutate({ id, name: newName.trim() });
    }
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure? All transactions in this category will be moved to Uncategorized.')) {
      deleteMutation.mutate(id);
    }
  };

  const startEditing = (cat: any) => {
    setEditingId(cat.id);
    setNewName(cat.name);
  };

  const categories = categoriesData?.data || [];
  const defaultCategories = categories.filter((c: any) => c.isDefault);
  const customCategories = categories.filter((c: any) => !c.isDefault);

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Categories</h1>
        <p className="text-slate-500 dark:text-slate-400">Organize your transactions with custom categories.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        {/* Default Categories */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden h-fit">
          <div className="p-6 border-b border-slate-200 dark:border-slate-800 flex items-center gap-3">
            <Lock className="w-5 h-5 text-slate-400" />
            <h3 className="font-bold text-slate-900 dark:text-white">System Defaults</h3>
          </div>
          <div className="p-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
            {defaultCategories.map((cat: any) => (
              <div key={cat.id} className="p-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl flex items-center gap-3 border border-slate-100 dark:border-slate-800">
                <div className="w-8 h-8 rounded-lg bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400">
                  <Tag className="w-4 h-4" />
                </div>
                <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">{cat.name}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Custom Categories */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden h-fit">
          <div className="p-6 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Tag className="w-5 h-5 text-blue-600" />
              <h3 className="font-bold text-slate-900 dark:text-white">Your Categories</h3>
            </div>
            {!isAdding && (
              <button
                onClick={() => setIsAdding(true)}
                className="p-2 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-lg transition-all"
              >
                <Plus className="w-5 h-5" />
              </button>
            )}
          </div>

          <div className="p-4 space-y-3">
            {isAdding && (
              <form onSubmit={handleCreate} className="p-3 bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-900/30 flex items-center gap-2">
                <input
                  autoFocus
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="Category name..."
                  className="flex-1 bg-transparent border-none focus:ring-0 text-sm font-semibold text-slate-900 dark:text-white outline-none"
                />
                <button type="button" onClick={() => setIsAdding(false)} className="p-1 text-slate-400 hover:text-slate-600">
                  <X className="w-4 h-4" />
                </button>
                <button type="submit" disabled={createMutation.isPending} className="p-1 text-blue-600 hover:text-blue-700">
                  {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
                </button>
              </form>
            )}

            {isLoading ? (
              <div className="p-8 flex justify-center">
                <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
              </div>
            ) : customCategories.length > 0 ? (
              <div className="grid grid-cols-1 gap-2">
                {customCategories.map((cat: any) => (
                  <div key={cat.id} className="group p-3 hover:bg-slate-50 dark:hover:bg-slate-800/50 rounded-xl flex items-center justify-between transition-all border border-transparent hover:border-slate-100 dark:hover:border-slate-800">
                    {editingId === cat.id ? (
                      <div className="flex-1 flex items-center gap-2">
                        <input
                          autoFocus
                          value={newName}
                          onChange={(e) => setNewName(e.target.value)}
                          className="flex-1 bg-transparent border-none focus:ring-0 text-sm font-semibold text-slate-900 dark:text-white outline-none"
                        />
                        <button onClick={() => setEditingId(null)} className="p-1 text-slate-400 hover:text-slate-600">
                          <X className="w-4 h-4" />
                        </button>
                        <button onClick={() => handleUpdate(cat.id)} className="p-1 text-blue-600 hover:text-blue-700">
                          <Check className="w-4 h-4" />
                        </button>
                      </div>
                    ) : (
                      <>
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-lg bg-emerald-100 dark:bg-emerald-900/30 flex items-center justify-center text-emerald-600 dark:text-emerald-400">
                            <Tag className="w-4 h-4" />
                          </div>
                          <span className="text-sm font-semibold text-slate-700 dark:text-slate-300">{cat.name}</span>
                        </div>
                        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                          <button onClick={() => startEditing(cat)} className="p-1.5 text-slate-400 hover:text-blue-600 rounded-lg">
                            <Edit2 className="w-4 h-4" />
                          </button>
                          <button onClick={() => handleDelete(cat.id)} className="p-1.5 text-slate-400 hover:text-red-600 rounded-lg">
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                ))}
              </div>
            ) : !isAdding && (
              <div className="p-12 text-center">
                <p className="text-sm text-slate-500">No custom categories yet.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default CategoriesPage;
