'use client';

import React, { Suspense, useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import { useSearchParams, useRouter } from 'next/navigation';
import { getModerationReports } from '../../../lib/api/moderation';
import {
  ModerationReport,
  ReportStatus,
  ModerationTargetType,
  ModerationReason,
  MODERATION_REASON_LABELS,
} from '../../../types/moderation';
import { PagedResponse } from '../../../types/api';

function ModerationQueueContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  const [statusFilter, setStatusFilter] = useState<ReportStatus | 'ALL'>(
    (searchParams.get('status') as ReportStatus) || 'ALL'
  );
  const [targetFilter, setTargetFilter] = useState<ModerationTargetType | 'ALL'>(
    (searchParams.get('targetType') as ModerationTargetType) || 'ALL'
  );
  const [reasonFilter, setReasonFilter] = useState<ModerationReason | 'ALL'>(
    (searchParams.get('reason') as ModerationReason) || 'ALL'
  );
  const [page, setPage] = useState<number>(0);
  const [sort, setSort] = useState<string>('createdAt,desc');

  const [data, setData] = useState<PagedResponse<ModerationReport> | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchReports = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await getModerationReports({
        status: statusFilter,
        targetType: targetFilter,
        reason: reasonFilter,
        page,
        size: 20,
        sort,
      });
      setData(res);
    } catch (err: any) {
      setError(err.message || 'Unable to load moderation reports.');
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter, targetFilter, reasonFilter, page, sort]);

  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  const handleFilterChange = (
    newStatus: ReportStatus | 'ALL',
    newTarget: ModerationTargetType | 'ALL',
    newReason: ModerationReason | 'ALL',
    newSort: string
  ) => {
    setStatusFilter(newStatus);
    setTargetFilter(newTarget);
    setReasonFilter(newReason);
    setSort(newSort);
    setPage(0);

    const q = new URLSearchParams();
    if (newStatus !== 'ALL') q.set('status', newStatus);
    if (newTarget !== 'ALL') q.set('targetType', newTarget);
    if (newReason !== 'ALL') q.set('reason', newReason);
    router.replace(`/admin/moderation?${q.toString()}`);
  };

  const getStatusBadge = (status: ReportStatus) => {
    switch (status) {
      case 'OPEN':
        return { bg: '#fef3c7', text: '#92400e', border: '#fde68a' };
      case 'IN_REVIEW':
        return { bg: '#e0e7ff', text: '#3730a3', border: '#c7d2fe' };
      case 'RESOLVED':
        return { bg: '#d1fae5', text: '#065f46', border: '#a7f3d0' };
      case 'DISMISSED':
        return { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };
    }
  };

  return (
    <div>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '26px', fontWeight: '800', color: '#0f172a', letterSpacing: '-0.02em', marginBottom: '4px' }}>
            Moderation Queue
          </h1>
          <p style={{ color: '#64748b', fontSize: '14px' }}>
            Review flagged citizen issues and comments requiring moderation.
          </p>
        </div>
        <button
          onClick={() => fetchReports()}
          style={{
            padding: '8px 16px',
            background: '#ffffff',
            border: '1px solid #cbd5e1',
            borderRadius: '6px',
            fontSize: '13px',
            fontWeight: '600',
            cursor: 'pointer',
            color: '#334155',
          }}
        >
          Refresh Queue
        </button>
      </div>

      {/* Filter Controls Bar */}
      <div
        style={{
          background: '#ffffff',
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
          padding: '16px 20px',
          marginBottom: '24px',
          display: 'flex',
          gap: '16px',
          flexWrap: 'wrap',
          alignItems: 'center',
        }}
      >
        <div>
          <label htmlFor="filter-status" style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#475569', marginBottom: '4px' }}>
            Status
          </label>
          <select
            id="filter-status"
            value={statusFilter}
            onChange={(e) => handleFilterChange(e.target.value as any, targetFilter, reasonFilter, sort)}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', background: '#ffffff' }}
          >
            <option value="ALL">All Statuses</option>
            <option value="OPEN">Open (Unreviewed)</option>
            <option value="IN_REVIEW">In Review</option>
            <option value="RESOLVED">Resolved</option>
            <option value="DISMISSED">Dismissed</option>
          </select>
        </div>

        <div>
          <label htmlFor="filter-target" style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#475569', marginBottom: '4px' }}>
            Target Type
          </label>
          <select
            id="filter-target"
            value={targetFilter}
            onChange={(e) => handleFilterChange(statusFilter, e.target.value as any, reasonFilter, sort)}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', background: '#ffffff' }}
          >
            <option value="ALL">All Targets</option>
            <option value="ISSUE">Issues Only</option>
            <option value="COMMENT">Comments Only</option>
          </select>
        </div>

        <div>
          <label htmlFor="filter-reason" style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#475569', marginBottom: '4px' }}>
            Violation Reason
          </label>
          <select
            id="filter-reason"
            value={reasonFilter}
            onChange={(e) => handleFilterChange(statusFilter, targetFilter, e.target.value as any, sort)}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', background: '#ffffff', maxWidth: '240px' }}
          >
            <option value="ALL">All Reasons</option>
            {Object.entries(MODERATION_REASON_LABELS).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="filter-sort" style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#475569', marginBottom: '4px' }}>
            Sort
          </label>
          <select
            id="filter-sort"
            value={sort}
            onChange={(e) => handleFilterChange(statusFilter, targetFilter, reasonFilter, e.target.value)}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', background: '#ffffff' }}
          >
            <option value="createdAt,desc">Newest First</option>
            <option value="createdAt,asc">Oldest First</option>
          </select>
        </div>

        {(statusFilter !== 'ALL' || targetFilter !== 'ALL' || reasonFilter !== 'ALL') && (
          <button
            onClick={() => handleFilterChange('ALL', 'ALL', 'ALL', 'createdAt,desc')}
            style={{
              marginTop: '18px',
              padding: '8px 14px',
              background: '#f1f5f9',
              border: 'none',
              borderRadius: '6px',
              color: '#475569',
              fontSize: '13px',
              cursor: 'pointer',
              fontWeight: '600',
            }}
          >
            Clear Filters
          </button>
        )}
      </div>

      {error && (
        <div style={{ padding: '16px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '24px', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* Reports Table / Card Container */}
      <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', overflow: 'hidden', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        {isLoading ? (
          <div style={{ padding: '60px 20px', textAlign: 'center', color: '#64748b' }}>
            <div style={{ width: '36px', height: '36px', border: '3px solid #e2e8f0', borderTopColor: '#1e40af', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 12px' }} />
            <p style={{ fontSize: '14px' }}>Loading moderation reports...</p>
          </div>
        ) : !data || data.content.length === 0 ? (
          <div style={{ padding: '60px 20px', textAlign: 'center', color: '#64748b' }}>
            <div style={{ fontSize: '32px', marginBottom: '12px' }}>✓</div>
            <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginBottom: '6px' }}>Queue is Clear</h2>
            <p style={{ fontSize: '14px', color: '#64748b', maxWidth: '360px', margin: '0 auto' }}>
              No moderation reports found matching the selected criteria.
            </p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
              <thead>
                <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontSize: '12px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  <th style={{ padding: '14px 20px', fontWeight: '700' }}>ID</th>
                  <th style={{ padding: '14px 20px', fontWeight: '700' }}>Target</th>
                  <th style={{ padding: '14px 20px', fontWeight: '700' }}>Reason</th>
                  <th style={{ padding: '14px 20px', fontWeight: '700' }}>Status</th>
                  <th style={{ padding: '14px 20px', fontWeight: '700' }}>Reported At</th>
                  <th style={{ padding: '14px 20px', fontWeight: '700', textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((report) => {
                  const badge = getStatusBadge(report.status);
                  const reasonLabel = MODERATION_REASON_LABELS[report.reason] || report.reason;

                  return (
                    <tr key={report.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                      <td style={{ padding: '16px 20px', fontFamily: 'monospace', fontSize: '13px', color: '#64748b' }}>
                        #{report.id.substring(0, 8)}
                      </td>
                      <td style={{ padding: '16px 20px' }}>
                        <span
                          style={{
                            padding: '4px 8px',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: '700',
                            background: report.targetType === 'ISSUE' ? '#e0f2fe' : '#ede9fe',
                            color: report.targetType === 'ISSUE' ? '#0369a1' : '#6d28d9',
                            letterSpacing: '0.05em',
                          }}
                        >
                          {report.targetType}
                        </span>
                      </td>
                      <td style={{ padding: '16px 20px', maxWidth: '280px' }}>
                        <div style={{ fontWeight: '600', color: '#0f172a', marginBottom: '2px' }}>{reasonLabel}</div>
                        {report.description && (
                          <div style={{ fontSize: '12px', color: '#64748b', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {report.description}
                          </div>
                        )}
                      </td>
                      <td style={{ padding: '16px 20px' }}>
                        <span
                          style={{
                            padding: '4px 10px',
                            borderRadius: '9999px',
                            fontSize: '12px',
                            fontWeight: '700',
                            background: badge.bg,
                            color: badge.text,
                            border: `1px solid ${badge.border}`,
                          }}
                        >
                          {report.status.replace('_', ' ')}
                        </span>
                      </td>
                      <td style={{ padding: '16px 20px', color: '#64748b', fontSize: '13px' }}>
                        {new Date(report.createdAt).toLocaleDateString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
                        })}
                      </td>
                      <td style={{ padding: '16px 20px', textAlign: 'right' }}>
                        <Link
                          href={`/admin/moderation/${report.id}`}
                          style={{
                            padding: '6px 14px',
                            background: '#1e40af',
                            color: '#ffffff',
                            borderRadius: '6px',
                            textDecoration: 'none',
                            fontSize: '13px',
                            fontWeight: '600',
                          }}
                        >
                          Review →
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        {data && data.totalPages > 1 && (
          <div style={{ padding: '16px 20px', background: '#f8fafc', borderTop: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '13px', color: '#64748b' }}>
              Showing Page {(data.page ?? 0) + 1} of {data.totalPages} ({data.totalElements} total reports)
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                disabled={data.first}
                onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                style={{
                  padding: '6px 14px',
                  borderRadius: '6px',
                  border: '1px solid #cbd5e1',
                  background: data.first ? '#f1f5f9' : '#ffffff',
                  color: data.first ? '#94a3b8' : '#0f172a',
                  cursor: data.first ? 'not-allowed' : 'pointer',
                  fontSize: '13px',
                  fontWeight: '600',
                }}
              >
                ← Previous
              </button>
              <button
                disabled={data.last}
                onClick={() => setPage((prev) => prev + 1)}
                style={{
                  padding: '6px 14px',
                  borderRadius: '6px',
                  border: '1px solid #cbd5e1',
                  background: data.last ? '#f1f5f9' : '#ffffff',
                  color: data.last ? '#94a3b8' : '#0f172a',
                  cursor: data.last ? 'not-allowed' : 'pointer',
                  fontSize: '13px',
                  fontWeight: '600',
                }}
              >
                Next →
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function ModerationQueuePage() {
  return (
    <Suspense
      fallback={
        <div style={{ padding: '60px 20px', textAlign: 'center', color: '#64748b' }}>
          <div style={{ width: '36px', height: '36px', border: '3px solid #e2e8f0', borderTopColor: '#1e40af', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 12px' }} />
          <p style={{ fontSize: '14px' }}>Loading queue...</p>
        </div>
      }
    >
      <ModerationQueueContent />
    </Suspense>
  );
}
