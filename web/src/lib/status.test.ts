import { describe, expect, it } from 'vitest';
import { formatDate, statusLabel, statusTone } from './status';

describe('status helpers', () => {
  it('labels every lifecycle status', () => {
    expect(statusLabel('SUBMITTED')).toBe('Submitted');
    expect(statusLabel('UNDER_REVIEW')).toBe('Under review');
    expect(statusLabel('RESOLVED')).toBe('Resolved');
    expect(statusLabel('CLOSED')).toBe('Closed');
    expect(statusLabel('REOPENED')).toBe('Reopened');
  });

  it('maps resolved/closed to success tone and rejected to error', () => {
    expect(statusTone('RESOLVED')).toBe('success');
    expect(statusTone('CLOSED')).toBe('success');
    expect(statusTone('REJECTED')).toBe('error');
    expect(statusTone('NEEDS_INFORMATION')).toBe('warning');
  });

  it('formats ISO timestamps and handles null', () => {
    expect(formatDate(null)).toBe('—');
    expect(formatDate('not-a-date')).toBe('—');
    expect(formatDate('2026-10-01T00:00:00Z')).toContain('2026');
  });
});
