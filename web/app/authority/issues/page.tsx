'use client';

import React, { useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { getAuthorityIssues } from '../../../lib/api/authority';
import { AuthorityIssueItemResponse } from '../../../types/authority';
import { IssueStatus, PriorityLevel } from '../../../types/issue';

const STATUS_BADGE_STYLES: Record<IssueStatus, { bg: string; text: string; border: string }> = {
  REPORTED: { bg: '#fef3c7', text: '#92400e', border: '#fcd34d' },
  VERIFIED: { bg: '#e0f2fe', text: '#075985', border: '#7dd3fc' },
  ACKNOWLEDGED: { bg: '#ede9fe', text: '#5b21b6', border: '#c4b5fd' },
  IN_PROGRESS: { bg: '#fce7f3', text: '#9d174d', border: '#f472b6' },
  RESOLVED: { bg: '#dcfce7', text: '#166534', border: '#86efac' },
  CITIZEN_VERIFIED: { bg: '#ccfbf1', text: '#115e59', border: '#5eead4' },
  NOT_FIXED: { bg: '#fee2e2', text: '#991b1b', border: '#fca5a5' },
};

const PRIORITY_BADGE_STYLES: Record<PriorityLevel, { bg: string; text: string }> = {
  LOW: { bg: '#f1f5f9', text: '#475569' },
  MEDIUM: { bg: '#e0f2fe', text: '#0369a1' },
  HIGH: { bg: '#ffedd5', text: '#c2410c' },
  CRITICAL: { bg: '#fee2e2', text: '#b91c1c' },
};

export default function AuthorityIssuesPage() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [issues, setIssues] = useState<AuthorityIssueItemResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters from query or state
  const statusFilter = (searchParams.get('status') as IssueStatus) || '';
  const priorityFilter = (searchParams.get('priority') as PriorityLevel) || '';
  const actionableOnly = searchParams.get('actionableOnly') === 'true';
  const page = parseInt(searchParams.get('page') || '0', 10);

  const fetchIssues = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getAuthorityIssues({
        status: statusFilter || undefined,
        priority: priorityFilter || undefined,
        actionableOnly: actionableOnly || undefined,
        page,
        size: 15,
      });
      setIssues(res.content || []);
      setTotalPages(res.totalPages || 0);
      setTotalElements(res.totalElements || 0);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch scoped authority issues';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, priorityFilter, actionableOnly, page]);

  useEffect(() => {
    fetchIssues();
  }, [fetchIssues]);

  function updateQuery(newParams: Record<string, string | null>) {
    const params = new URLSearchParams(searchParams.toString());
    Object.entries(newParams).forEach(([k, v]) => {
      if (v === null || v === '') {
        params.delete(k);
      } else {
        params.set(k, v);
      }
    });
    router.push(`/authority/issues?${params.toString()}`);
  }

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a', margin: '0 0 4px 0' }}>Jurisdictional Issues</h1>
          <p style={{ fontSize: '14px', color: '#64748b', margin: 0 }}>
            {totalElements} civic issue{totalElements === 1 ? '' : 's'} within your server-enforced authority scope.
          </p>
        </div>
        <button
          onClick={fetchIssues}
          style={{ padding: '8px 14px', background: '#ffffff', border: '1px solid #cbd5e1', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: '500', color: '#475569' }}
        >
          Refresh Queue
        </button>
      </div>

      {/* Filter Controls Bar */}
      <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '16px', marginBottom: '20px', display: 'flex', gap: '16px', flexWrap: 'wrap', alignItems: 'center' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <label htmlFor="authority-status-filter" style={{ fontSize: '12px', fontWeight: '600', color: '#475569' }}>Status</label>
          <select
            id="authority-status-filter"
            value={statusFilter}
            onChange={(e) => updateQuery({ status: e.target.value || null, page: '0' })}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', background: '#ffffff' }}
          >
            <option value="">All Statuses</option>
            <option value="REPORTED">REPORTED</option>
            <option value="VERIFIED">VERIFIED</option>
            <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
            <option value="IN_PROGRESS">IN_PROGRESS</option>
            <option value="RESOLVED">RESOLVED</option>
            <option value="CITIZEN_VERIFIED">CITIZEN_VERIFIED</option>
            <option value="NOT_FIXED">NOT_FIXED</option>
          </select>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <label htmlFor="authority-priority-filter" style={{ fontSize: '12px', fontWeight: '600', color: '#475569' }}>Priority</label>
          <select
            id="authority-priority-filter"
            value={priorityFilter}
            onChange={(e) => updateQuery({ priority: e.target.value || null, page: '0' })}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', background: '#ffffff' }}
          >
            <option value="">All Priorities</option>
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
            <option value="CRITICAL">CRITICAL</option>
          </select>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '20px' }}>
          <input
            type="checkbox"
            id="actionable-only-check"
            checked={actionableOnly}
            onChange={(e) => updateQuery({ actionableOnly: e.target.checked ? 'true' : null, page: '0' })}
            style={{ cursor: 'pointer', width: '16px', height: '16px' }}
          />
          <label htmlFor="actionable-only-check" style={{ fontSize: '13px', fontWeight: '500', color: '#0f172a', cursor: 'pointer' }}>
            Actionable issues only
          </label>
        </div>

        {(statusFilter || priorityFilter || actionableOnly) && (
          <button
            onClick={() => updateQuery({ status: null, priority: null, actionableOnly: null, page: '0' })}
            style={{ marginTop: '20px', padding: '6px 12px', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '6px', cursor: 'pointer', fontSize: '12px', color: '#475569' }}
          >
            Reset Filters
          </button>
        )}
      </div>

      {/* Error State */}
      {error && (
        <div style={{ padding: '20px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '20px' }}>
          <div style={{ fontWeight: '600', marginBottom: '4px' }}>Error loading issues</div>
          <div style={{ fontSize: '14px' }}>{error}</div>
        </div>
      )}

      {/* Loading Skeleton */}
      {loading ? (
        <div style={{ padding: '48px 0', textAlign: 'center' }}>
          <div style={{ width: '36px', height: '36px', border: '3px solid #e2e8f0', borderTopColor: '#0284c7', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 12px' }} />
          <p style={{ color: '#64748b', fontSize: '14px' }}>Loading scoped issues...</p>
        </div>
      ) : issues.length === 0 ? (
        /* Empty State */
        <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '48px 24px', textAlign: 'center' }}>
          <div style={{ fontSize: '40px', marginBottom: '12px' }}>📭</div>
          <h2 style={{ fontSize: '18px', fontWeight: '600', color: '#0f172a', marginBottom: '6px' }}>No issues found</h2>
          <p style={{ fontSize: '14px', color: '#64748b', maxWidth: '440px', margin: '0 auto' }}>
            {actionableOnly || statusFilter || priorityFilter
              ? 'No issues match the selected filters within your authority scope.'
              : 'There are currently no resolved civic issues assigned to your jurisdiction.'}
          </p>
        </div>
      ) : (
        /* Issues List */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {issues.map((issue) => {
            const statusStyle = STATUS_BADGE_STYLES[issue.status] || { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };
            const priorityStyle = PRIORITY_BADGE_STYLES[issue.priorityLevel] || { bg: '#f1f5f9', text: '#475569' };

            return (
              <div
                key={issue.id}
                style={{
                  background: '#ffffff',
                  borderRadius: '10px',
                  border: '1px solid #e2e8f0',
                  padding: '16px 20px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  boxShadow: '0 1px 2px rgba(0,0,0,0.03)',
                  transition: 'border-color 0.15s ease',
                }}
              >
                <div style={{ flex: 1, marginRight: '24px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '11px', fontWeight: '700', padding: '2px 8px', borderRadius: '12px', background: statusStyle.bg, color: statusStyle.text, border: `1px solid ${statusStyle.border}` }}>
                      {issue.status}
                    </span>
                    <span style={{ fontSize: '11px', fontWeight: '600', padding: '2px 8px', borderRadius: '4px', background: priorityStyle.bg, color: priorityStyle.text }}>
                      {issue.priorityLevel} ({issue.priorityScore})
                    </span>
                    <span style={{ fontSize: '12px', color: '#64748b', background: '#f8fafc', padding: '2px 8px', borderRadius: '4px' }}>
                      {issue.categoryName}
                    </span>
                    {issue.actionable && (
                      <span style={{ fontSize: '11px', color: '#0369a1', background: '#e0f2fe', padding: '2px 6px', borderRadius: '4px', fontWeight: '600' }}>
                        ⚡ Actionable
                      </span>
                    )}
                  </div>

                  <Link
                    href={`/authority/issues/${issue.id}`}
                    style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', textDecoration: 'none', display: 'block', marginBottom: '4px' }}
                  >
                    {issue.title}
                  </Link>

                  <p style={{ fontSize: '13px', color: '#475569', margin: '0 0 8px 0', lineHeight: '1.4' }}>
                    {issue.descriptionSnippet}
                  </p>

                  <div style={{ display: 'flex', gap: '16px', fontSize: '12px', color: '#64748b', flexWrap: 'wrap' }}>
                    {issue.wardName && (
                      <span>📍 Ward: <strong>{issue.wardName}</strong></span>
                    )}
                    {issue.departmentName && (
                      <span>🏢 Dept: <strong>{issue.departmentName}</strong></span>
                    )}
                    <span>💬 {issue.commentCount} comments</span>
                    <span>👍 {issue.supportCount} supports</span>
                    <span>Created: {new Date(issue.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>

                <div>
                  <Link
                    href={`/authority/issues/${issue.id}`}
                    style={{
                      display: 'inline-block',
                      padding: '8px 16px',
                      background: '#0284c7',
                      color: '#ffffff',
                      borderRadius: '6px',
                      textDecoration: 'none',
                      fontSize: '13px',
                      fontWeight: '600',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    Manage Issue →
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '12px', marginTop: '24px' }}>
          <button
            disabled={page === 0}
            onClick={() => updateQuery({ page: String(page - 1) })}
            style={{
              padding: '8px 14px',
              background: '#ffffff',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              cursor: page === 0 ? 'not-allowed' : 'pointer',
              opacity: page === 0 ? 0.5 : 1,
              fontSize: '13px',
              fontWeight: '500',
            }}
          >
            ← Previous
          </button>
          <span style={{ fontSize: '13px', color: '#64748b' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            disabled={page >= totalPages - 1}
            onClick={() => updateQuery({ page: String(page + 1) })}
            style={{
              padding: '8px 14px',
              background: '#ffffff',
              border: '1px solid #cbd5e1',
              borderRadius: '6px',
              cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
              opacity: page >= totalPages - 1 ? 0.5 : 1,
              fontSize: '13px',
              fontWeight: '500',
            }}
          >
            Next →
          </button>
        </div>
      )}
    </div>
  );
}
