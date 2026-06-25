import { describe, it, expect } from 'vitest';
import { cn, formatCurrency, formatDate } from '@/lib/utils';

describe('cn', () => {
  it('merges class names correctly', () => {
    expect(cn('px-4', 'py-2')).toBe('px-4 py-2');
  });

  it('handles conditional classes', () => {
    const show = false;
    expect(cn('base', show && 'hidden', 'visible')).toBe('base visible');
  });

  it('handles tailwind conflict resolution', () => {
    expect(cn('px-4', 'px-6')).toBe('px-6');
  });

  it('returns empty string for no inputs', () => {
    expect(cn()).toBe('');
  });
});

describe('formatCurrency', () => {
  it('formats INR by default', () => {
    const result = formatCurrency(1000);
    expect(result).toContain('1,000');
  });

  it('formats USD with dollar sign', () => {
    const result = formatCurrency(500, 'USD');
    expect(result).toContain('$');
    expect(result).toContain('500');
  });

  it('handles string amounts', () => {
    const result = formatCurrency('2500', 'INR');
    expect(result).toContain('2,500');
  });

  it('handles zero', () => {
    const result = formatCurrency(0);
    expect(result).toContain('0');
  });
});

describe('formatDate', () => {
  it('formats a date string', () => {
    const result = formatDate('2026-06-15');
    expect(result).toContain('Jun');
    expect(result).toContain('2026');
  });

  it('formats a Date object', () => {
    const result = formatDate(new Date('2026-01-01'));
    expect(result).toContain('Jan');
    expect(result).toContain('2026');
  });
});
