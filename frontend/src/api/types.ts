// =============================================================================
// Shared TypeScript types matching backend DTOs
// =============================================================================

// ─── API Envelope ────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  pagination?: PaginationDto;
}

export interface PaginationDto {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

// ─── Auth ────────────────────────────────────────────────────────────────────

export interface UserDto {
  id: string;
  name: string;
  email: string;
  currency: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserDto;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UpdateProfileRequest {
  name?: string;
  currency?: string;
}

// ─── Transactions ────────────────────────────────────────────────────────────

export type TransactionType = 'INCOME' | 'EXPENSE';

export interface CategoryResponse {
  id: string;
  name: string;
  isDefault: boolean;
}

export interface TransactionResponse {
  id: string;
  userId: string;
  amount: number;
  type: TransactionType;
  category: CategoryResponse;
  description: string | null;
  date: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTransactionRequest {
  amount: number;
  type: TransactionType;
  categoryId: string;
  description?: string;
  date: string;
}

export interface UpdateTransactionRequest {
  amount?: number;
  type?: TransactionType;
  categoryId?: string;
  description?: string;
  date?: string;
}

export interface TransactionQueryParams {
  page?: number;
  limit?: number;
  from?: string;
  to?: string;
  type?: TransactionType;
  category_id?: string;
  min_amount?: number;
  max_amount?: number;
  search?: string;
}

// ─── Categories ──────────────────────────────────────────────────────────────

export interface CategoryDTO {
  id: string;
  name: string;
  isDefault: boolean;
}

export interface CreateCategoryRequest {
  name: string;
}

// ─── Budgets ─────────────────────────────────────────────────────────────────

export interface BudgetEnriched {
  id: string;
  category: { id: string; name: string };
  limitAmount: number;
  spent: number;
  remaining: number;
  percentage: number;
  status: 'ok' | 'warning' | 'exceeded';
  month: number;
  year: number;
}

export interface CreateBudgetRequest {
  categoryId: string;
  limitAmount: number;
  month: number;
  year: number;
}

// ─── Dashboard ───────────────────────────────────────────────────────────────

export interface DashboardSummary {
  totalIncome: number;
  totalExpenses: number;
  balance: number;
  savings: number;
  currency: string;
  month: string;
  recentTransactions: TransactionResponse[];
  activeBudgetAlerts: BudgetAlert[];
  recentSpending: SpendingTrend[];
  categoryBreakdown?: Record<string, number>;
}

export interface BudgetAlert {
  budgetId: string;
  categoryName: string;
  status: string;
  percentage: number;
  spent: number;
  limit: number;
}

export interface SpendingTrend {
  date: string;
  amount: number;
}

// ─── Analysis / Insights ────────────────────────────────────────────────────

export interface InsightData {
  sufficient: boolean;
  message?: string;
  patterns: string[];
  recommendations: string[];
  anomalies: AnomalyResponse[];
  projectedMonthlyExpense: number;
  cachedAt?: string;
}

export interface AnomalyResponse {
  transactionId: string;
  date: string;
  amount: number;
  category: string;
  average: number;
  deviation: string;
  reason: string;
}

export interface ProjectionResponse {
  spent: number;
  projected: number;
  daysElapsed: number;
  daysInMonth: number;
  currency: string;
}

// ─── Notifications ──────────────────────────────────────────────────────────

export interface NotificationResponse {
  id: string;
  type: string;
  title: string;
  body: string;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationQueryParams {
  page?: number;
  limit?: number;
  unreadOnly?: boolean;
}

// ─── NLP ─────────────────────────────────────────────────────────────────────

export interface DraftTransactionDTO {
  amount: number;
  type: string;
  categoryId: string;
  categoryName: string;
  description: string;
  date: string;
}

export interface ParseResponse {
  parsed: boolean;
  draft: DraftTransactionDTO | null;
  message?: string;
}

// ─── Reports ─────────────────────────────────────────────────────────────────

export interface ReportDistribution {
  category: string;
  amount: number;
}

export interface MonthlyTrend {
  year: number;
  month: number;
  type: TransactionType;
  amount: number;
}

export interface DailyTrend {
  date: string;
  type: TransactionType;
  amount: number;
}

export interface ReportSummary {
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
  largestExpense: {
    amount: number;
    description: string;
    date: string;
  } | null;
  transactionCount: number;
  averageDailySpend: number;
  currency: string;
}

export interface ReportQueryParams {
  from: string;
  to: string;
}

// ─── Messages ────────────────────────────────────────────────────────────────

export interface MessageResponse {
  message: string;
}
