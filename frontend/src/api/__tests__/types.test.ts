import { describe, it, expect } from 'vitest';
import type {
  ApiResponse,
  PaginationDto,
  TransactionResponse,
  TransactionType,
  DashboardSummary,
  BudgetEnriched,
  InsightData,
} from '@/api/types';

describe('API Types', () => {
  it('validates ApiResponse structure', () => {
    const response: ApiResponse<string> = {
      success: true,
      data: 'test',
    };

    expect(response.success).toBe(true);
    expect(response.data).toBe('test');
  });

  it('validates ApiResponse with pagination', () => {
    const pagination: PaginationDto = {
      page: 1,
      limit: 20,
      total: 100,
      totalPages: 5,
    };

    const response: ApiResponse<string[]> = {
      success: true,
      data: ['item1', 'item2'],
      pagination,
    };

    expect(response.pagination?.totalPages).toBe(5);
    expect(response.pagination?.total).toBe(100);
  });

  it('validates TransactionResponse with income type', () => {
    const transaction: TransactionResponse = {
      id: '1',
      userId: 'user1',
      amount: 1000.50,
      type: 'INCOME',
      category: { id: 'cat1', name: 'Salary', isDefault: true },
      description: 'Monthly salary',
      date: '2026-06-01',
      createdAt: '2026-06-01T10:00:00Z',
      updatedAt: '2026-06-01T10:00:00Z',
    };

    expect(transaction.type).toBe('INCOME');
    expect(transaction.amount).toBeGreaterThan(0);
  });

  it('validates TransactionResponse with expense type', () => {
    const transaction: TransactionResponse = {
      id: '2',
      userId: 'user1',
      amount: -50.00,
      type: 'EXPENSE',
      category: { id: 'cat2', name: 'Groceries', isDefault: false },
      description: null,
      date: '2026-06-15',
      createdAt: '2026-06-15T14:00:00Z',
      updatedAt: '2026-06-15T14:00:00Z',
    };

    expect(transaction.type).toBe('EXPENSE');
    expect(transaction.description).toBeNull();
  });

  it('validates TransactionType union', () => {
    const validTypes: TransactionType[] = ['INCOME', 'EXPENSE'];
    const invalidValue = 'SAVINGS' as TransactionType;

    expect(validTypes).toContain('INCOME');
    expect(validTypes).toContain('EXPENSE');
    // Should not be a valid type
    expect(validTypes).not.toContain(invalidValue);
  });

  it('validates DashboardSummary default values', () => {
    const summary: DashboardSummary = {
      totalIncome: 0,
      totalExpenses: 0,
      balance: 0,
      savings: 0,
      currency: 'USD',
      month: '2026-06',
      recentTransactions: [],
      activeBudgetAlerts: [],
      recentSpending: [],
    };

    expect(summary.recentTransactions).toHaveLength(0);
    expect(summary.currency).toBe('USD');
    expect(summary.balance).toBe(0);
  });

  it('validates BudgetEnriched budget status', () => {
    const budget: BudgetEnriched = {
      id: 'budget1',
      category: { id: 'cat1', name: 'Food' },
      limitAmount: 500,
      spent: 300,
      remaining: 200,
      percentage: 60,
      status: 'warning',
      month: 6,
      year: 2026,
    };

    expect(budget.percentage).toBe(60);
    expect(budget.status).toBe('warning');
    expect(budget.remaining).toBe(budget.limitAmount - budget.spent);
  });

  it('validates InsightData structure', () => {
    const insight: InsightData = {
      sufficient: true,
      patterns: ['spending increases on weekends'],
      recommendations: ['set a weekend budget'],
      anomalies: [],
      projectedMonthlyExpense: 2500,
    };

    expect(insight.sufficient).toBe(true);
    expect(insight.patterns).toHaveLength(1);
    expect(insight.anomalies).toHaveLength(0);
  });
});
