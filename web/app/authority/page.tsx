'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { getAuthorityDashboard } from '../../lib/api/authority';
import { AuthorityDashboardMetrics } from '../../types/authority';

export default function AuthorityDashboardPage() {
  const [metrics, setMetrics] = useState<AuthorityDashboardMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDashboard();
  }, []);

  async function loadDashboard() {
    setLoading(true);
    setError(null);
    try {
      const data = await getAuthorityDashboard();
      setMetrics(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load authority dashboard';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div style={{ padding: '48px 0', textAlign: 'center' }}>
        <div style={{ width: '40px', height: '40px', border: '3px solid #e2e8f0', borderTopColor: '#0284c7', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px' }} />
        <p style={{ color: '#64748b', fontSize: '14px' }}>Loading authority operations dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '32px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '24px' }}>
        <h3 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '8px' }}>Unable to load authority metrics</h3>
        <p style={{ fontSize: '14px', marginBottom: '16px' }}>{error}</p>
        <button
          onClick={loadDashboard}
          style={{ padding: '8px 16px', background: '#dc2626', color: '#ffffff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: '500' }}
        >
          Try Again
        </button>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a', margin: '0 0 6px 0' }}>Authority Operations Dashboard</h1>
          <p style={{ fontSize: '14px', color: '#64748b', margin: 0 }}>
            Monitor and resolve civic issues within your designated authority scope.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={loadDashboard}
            style={{ padding: '8px 14px', background: '#ffffff', border: '1px solid #cbd5e1', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: '500', color: '#475569' }}
          >
            Refresh
          </button>
          <Link
            href="/authority/issues"
            style={{ padding: '8px 16px', background: '#0284c7', color: '#ffffff', borderRadius: '6px', textDecoration: 'none', fontSize: '13px', fontWeight: '600' }}
          >
            Manage Scoped Issues
          </Link>
        </div>
      </div>

      {/* Active Authority Assignments Card */}
      <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '20px', marginBottom: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
        <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>🛡️</span> Active Jurisdictional Assignments
        </h2>
        {(!metrics?.assignedScopes || metrics.assignedScopes.length === 0) ? (
          <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', color: '#64748b', fontSize: '14px' }}>
            No specific ward or department assignments configured. As an administrator, you have access to all resolved civic issues.
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '12px' }}>
            {metrics.assignedScopes.map((scope) => (
              <div key={scope.id} style={{ padding: '14px', background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: '8px' }}>
                <div style={{ fontWeight: '600', color: '#0369a1', fontSize: '14px', marginBottom: '4px' }}>
                  {scope.wardName ? `Ward: ${scope.wardName} (${scope.wardNumber || 'N/A'})` : scope.departmentName ? `Department: ${scope.departmentName}` : 'City-wide Scope'}
                </div>
                <div style={{ fontSize: '12px', color: '#0284c7' }}>
                  {scope.civicBodyName} • {scope.cityName}
                </div>
                {scope.designation && (
                  <div style={{ fontSize: '11px', color: '#0369a1', marginTop: '4px', fontStyle: 'italic' }}>
                    Role: {scope.designation}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Metric Cards Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '16px', borderLeft: '4px solid #0284c7' }}>
          <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Actionable Issues</div>
          <div style={{ fontSize: '28px', fontWeight: '700', color: '#0f172a', margin: '6px 0' }}>{metrics?.actionableIssues ?? 0}</div>
          <div style={{ fontSize: '12px', color: '#0284c7' }}>Requires operational attention</div>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '16px', borderLeft: '4px solid #f59e0b' }}>
          <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Verified</div>
          <div style={{ fontSize: '28px', fontWeight: '700', color: '#d97706', margin: '6px 0' }}>{metrics?.verifiedIssues ?? 0}</div>
          <div style={{ fontSize: '12px', color: '#b45309' }}>Confirmed by field team</div>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '16px', borderLeft: '4px solid #3b82f6' }}>
          <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Acknowledged</div>
          <div style={{ fontSize: '28px', fontWeight: '700', color: '#2563eb', margin: '6px 0' }}>{metrics?.acknowledgedIssues ?? 0}</div>
          <div style={{ fontSize: '12px', color: '#1d4ed8' }}>Work order scheduled</div>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '16px', borderLeft: '4px solid #8b5cf6' }}>
          <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>In Progress</div>
          <div style={{ fontSize: '28px', fontWeight: '700', color: '#7c3aed', margin: '6px 0' }}>{metrics?.inProgressIssues ?? 0}</div>
          <div style={{ fontSize: '12px', color: '#6d28d9' }}>Active field work</div>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '16px', borderLeft: '4px solid #10b981' }}>
          <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Resolved</div>
          <div style={{ fontSize: '28px', fontWeight: '700', color: '#059669', margin: '6px 0' }}>{metrics?.resolvedIssues ?? 0}</div>
          <div style={{ fontSize: '12px', color: '#047857' }}>Awaiting citizen verify</div>
        </div>

        <div style={{ background: '#ffffff', borderRadius: '10px', border: '1px solid #e2e8f0', padding: '16px', borderLeft: '4px solid #64748b' }}>
          <div style={{ fontSize: '12px', color: '#64748b', fontWeight: '500', textTransform: 'uppercase' }}>Total Scoped</div>
          <div style={{ fontSize: '28px', fontWeight: '700', color: '#334155', margin: '6px 0' }}>{metrics?.totalScopedIssues ?? 0}</div>
          <div style={{ fontSize: '12px', color: '#64748b' }}>In designated jurisdiction</div>
        </div>
      </div>

      {/* Quick Action Navigation */}
      <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
        <Link
          href="/authority/issues?actionableOnly=true"
          style={{
            flex: '1 1 300px',
            background: '#ffffff',
            border: '1px solid #cbd5e1',
            borderRadius: '10px',
            padding: '20px',
            textDecoration: 'none',
            color: '#0f172a',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            transition: 'border-color 0.15s ease',
          }}
        >
          <div style={{ fontSize: '18px', fontWeight: '600', marginBottom: '6px', color: '#0284c7' }}>
            ⚡ Go to Actionable Queue →
          </div>
          <p style={{ fontSize: '13px', color: '#64748b', margin: 0 }}>
            Inspect issues that are currently pending verification, work assignment, or field resolution.
          </p>
        </Link>
        <Link
          href="/authority/issues"
          style={{
            flex: '1 1 300px',
            background: '#ffffff',
            border: '1px solid #cbd5e1',
            borderRadius: '10px',
            padding: '20px',
            textDecoration: 'none',
            color: '#0f172a',
            boxShadow: '0 1px 3px rgba(0,0,0,0.04)',
            transition: 'border-color 0.15s ease',
          }}
        >
          <div style={{ fontSize: '18px', fontWeight: '600', marginBottom: '6px', color: '#334155' }}>
            📋 Browse All Scoped Issues →
          </div>
          <p style={{ fontSize: '13px', color: '#64748b', margin: 0 }}>
            Filter by status, priority, ward, or department with full pagination and search support.
          </p>
        </Link>
      </div>
    </div>
  );
}
