'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { getAudits } from '@/lib/api/geography';
import { CivicGeographyAuditResponse } from '@/types/geography';
import { PagedResponse } from '@/types/api';

export default function AdminGeographyAuditsPage() {
  const [auditData, setAuditData] = useState<PagedResponse<CivicGeographyAuditResponse>>({
    content: [],
    page: 0,
    size: 25,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
  });
  const [selectedEntityType, setSelectedEntityType] = useState('');
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const res = await getAudits({
        entityType: selectedEntityType || undefined,
        page,
        size: 25,
      });
      setAuditData(res);
    } catch (err: any) {
      setError(err.message || 'Failed to load audit history.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedEntityType, page]);

  return (
    <div>
      {/* Breadcrumb */}
      <div style={{ marginBottom: '16px', fontSize: '13px', color: '#64748b' }}>
        <Link href="/admin/geography" style={{ color: '#1e40af', textDecoration: 'none' }}>
          Civic Geography
        </Link>{' '}
        / <span style={{ color: '#0f172a', fontWeight: '600' }}>Audit History</span>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', margin: 0, letterSpacing: '-0.02em' }}>
            Civic Geography Audit Trail
          </h1>
          <p style={{ color: '#64748b', fontSize: '14px', margin: '4px 0 0 0' }}>
            Append-only, immutable record of all administrative changes to civic bodies, wards, departments, and mappings.
          </p>
        </div>

        <div>
          <select
            value={selectedEntityType}
            onChange={(e) => {
              setSelectedEntityType(e.target.value);
              setPage(0);
            }}
            style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
          >
            <option value="">All Entity Types</option>
            <option value="CIVIC_BODY">Civic Body</option>
            <option value="CITY">City</option>
            <option value="WARD">Ward</option>
            <option value="DEPARTMENT">Department</option>
            <option value="CATEGORY_MAPPING">Category Mapping</option>
            <option value="WARD_MAPPING">Ward Mapping</option>
            <option value="RESPONSIBILITY">Responsibility</option>
          </select>
        </div>
      </div>

      {error && (
        <div style={{ padding: '14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '20px', fontSize: '13px' }}>
          {error}
        </div>
      )}

      {/* Table */}
      <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
          <thead>
            <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0', color: '#475569', fontWeight: '600' }}>
              <th style={{ padding: '12px 16px' }}>Timestamp</th>
              <th style={{ padding: '12px 16px' }}>Action</th>
              <th style={{ padding: '12px 16px' }}>Entity Type</th>
              <th style={{ padding: '12px 16px' }}>Actor</th>
              <th style={{ padding: '12px 16px' }}>Details / Reason</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  Loading audit records...
                </td>
              </tr>
            ) : !auditData?.content || auditData.content.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: '#64748b' }}>
                  No audit records found.
                </td>
              </tr>
            ) : (
              auditData.content.map((a: CivicGeographyAuditResponse) => (
                <tr key={a.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '12px 16px', color: '#64748b', fontSize: '12px', whiteSpace: 'nowrap' }}>
                    {new Date(a.createdAt).toLocaleString()}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <span
                      style={{
                        fontSize: '11px',
                        fontWeight: '700',
                        padding: '2px 8px',
                        borderRadius: '4px',
                        background: a.action.includes('DEACTIVATED')
                          ? '#fee2e2'
                          : a.action.includes('CREATED')
                          ? '#dcfce7'
                          : '#e0f2fe',
                        color: a.action.includes('DEACTIVATED')
                          ? '#b91c1c'
                          : a.action.includes('CREATED')
                          ? '#15803d'
                          : '#0369a1',
                      }}
                    >
                      {a.action}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: '600', color: '#334155' }}>
                    {a.entityType}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#1e293b' }}>
                    {a.actorName || a.actorId || 'System'}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#64748b', fontSize: '12px', maxWidth: '360px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {a.reason || (a.newState ? JSON.stringify(a.newState) : '—')}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        {auditData && auditData.totalPages > 1 && (
          <div style={{ padding: '12px 16px', background: '#f8fafc', borderTop: '1px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '13px', color: '#64748b' }}>
            <span>
              Page {auditData.page + 1} of {auditData.totalPages}
            </span>
            <div style={{ display: 'flex', gap: '6px' }}>
              <button
                disabled={auditData.page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                style={{ padding: '4px 10px', borderRadius: '4px', border: '1px solid #cbd5e1', background: '#ffffff', cursor: auditData.page === 0 ? 'not-allowed' : 'pointer' }}
              >
                Previous
              </button>
              <button
                disabled={auditData.page >= auditData.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                style={{ padding: '4px 10px', borderRadius: '4px', border: '1px solid #cbd5e1', background: '#ffffff', cursor: auditData.page >= auditData.totalPages - 1 ? 'not-allowed' : 'pointer' }}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
