'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { getModerationSummary } from '../../lib/api/moderation';
import { ModerationSummary } from '../../types/moderation';

export default function AdminDashboardPage() {
  const [summary, setSummary] = useState<ModerationSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    async function loadData() {
      try {
        setIsLoading(true);
        const data = await getModerationSummary();
        if (mounted) {
          setSummary(data);
          setError(null);
        }
      } catch (err: any) {
        if (mounted) {
          setError(err.message || 'Unable to load summary metrics.');
        }
      } finally {
        if (mounted) setIsLoading(false);
      }
    }
    loadData();
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div>
      {/* Title & Actions */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '32px' }}>
        <div>
          <h1 style={{ fontSize: '28px', fontWeight: '800', color: '#0f172a', letterSpacing: '-0.02em', marginBottom: '6px' }}>
            Moderation Operations
          </h1>
          <p style={{ color: '#64748b', fontSize: '15px' }}>
            Review, evaluate, and moderate citizen-reported issues and comments.
          </p>
        </div>
        <Link
          href="/admin/moderation"
          style={{
            padding: '12px 24px',
            background: '#1e40af',
            color: '#ffffff',
            borderRadius: '8px',
            textDecoration: 'none',
            fontWeight: '600',
            fontSize: '14px',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            boxShadow: '0 2px 4px rgba(30, 64, 175, 0.2)',
          }}
        >
          Open Moderation Queue →
        </Link>
      </div>

      {error && (
        <div style={{ padding: '16px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b', marginBottom: '24px', fontSize: '14px' }}>
          {error}
        </div>
      )}

      {/* Metric Cards Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '20px', marginBottom: '40px' }}>
        {/* Total Reports */}
        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '13px', fontWeight: '700', color: '#1e40af', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              All Time
            </span>
            <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#3b82f6' }} />
          </div>
          <div style={{ fontSize: '36px', fontWeight: '800', color: '#0f172a', marginBottom: '8px' }}>
            {isLoading ? '...' : summary?.totalReports ?? 0}
          </div>
          <div style={{ fontSize: '14px', color: '#64748b', marginBottom: '16px' }}>Total Reports</div>
          <Link href="/admin/moderation" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            View all reports →
          </Link>
        </div>

        {/* Open Reports */}
        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '13px', fontWeight: '700', color: '#d97706', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Action Required
            </span>
            <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#f59e0b' }} />
          </div>
          <div style={{ fontSize: '36px', fontWeight: '800', color: '#0f172a', marginBottom: '8px' }}>
            {isLoading ? '...' : summary?.openCount ?? 0}
          </div>
          <div style={{ fontSize: '14px', color: '#64748b', marginBottom: '16px' }}>Open Reports</div>
          <Link href="/admin/moderation?status=OPEN" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            Review open reports →
          </Link>
        </div>

        {/* In Review */}
        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '13px', fontWeight: '700', color: '#4f46e5', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              In Progress
            </span>
            <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#6366f1' }} />
          </div>
          <div style={{ fontSize: '36px', fontWeight: '800', color: '#0f172a', marginBottom: '8px' }}>
            {isLoading ? '...' : summary?.inReviewCount ?? 0}
          </div>
          <div style={{ fontSize: '14px', color: '#64748b', marginBottom: '16px' }}>In Review</div>
          <Link href="/admin/moderation?status=IN_REVIEW" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            View in-review queue →
          </Link>
        </div>

        {/* Resolved */}
        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '13px', fontWeight: '700', color: '#059669', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Actioned
            </span>
            <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981' }} />
          </div>
          <div style={{ fontSize: '36px', fontWeight: '800', color: '#0f172a', marginBottom: '8px' }}>
            {isLoading ? '...' : summary?.resolvedCount ?? 0}
          </div>
          <div style={{ fontSize: '14px', color: '#64748b', marginBottom: '16px' }}>Resolved Reports</div>
          <Link href="/admin/moderation?status=RESOLVED" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            View resolved history →
          </Link>
        </div>

        {/* Dismissed */}
        <div style={{ background: '#ffffff', padding: '24px', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
            <span style={{ fontSize: '13px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Non-Violating
            </span>
            <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#94a3b8' }} />
          </div>
          <div style={{ fontSize: '36px', fontWeight: '800', color: '#0f172a', marginBottom: '8px' }}>
            {isLoading ? '...' : summary?.dismissedCount ?? 0}
          </div>
          <div style={{ fontSize: '14px', color: '#64748b', marginBottom: '16px' }}>Dismissed Reports</div>
          <Link href="/admin/moderation?status=DISMISSED" style={{ fontSize: '13px', color: '#1e40af', fontWeight: '600', textDecoration: 'none' }}>
            View dismissed reports →
          </Link>
        </div>
      </div>

      {/* Moderation Policy & Neutrality Guidelines */}
      <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '28px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '12px' }}>
          Civic Moderation Principles
        </h2>
        <p style={{ color: '#475569', fontSize: '14px', lineHeight: '1.7', marginBottom: '16px' }}>
          Nagrivic is an impartial civic accountability platform. Moderation decisions must be made strictly according to content safety policies (spam, harassment, explicit content, PII exposure). Political criticism or grievances regarding municipal bodies or elected representatives are never grounds for removal or hiding.
        </p>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', fontSize: '13px' }}>
          <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
            <strong style={{ display: 'block', color: '#1e293b', marginBottom: '4px' }}>Immutable Audit Log</strong>
            All moderation actions, actor identities, reasons, and timestamps are recorded in an append-only audit trail.
          </div>
          <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
            <strong style={{ display: 'block', color: '#1e293b', marginBottom: '4px' }}>Soft-Delete & Privacy Protection</strong>
            Content is soft-hidden or soft-deleted to preserve legal auditability while keeping public APIs clean.
          </div>
          <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
            <strong style={{ display: 'block', color: '#1e293b', marginBottom: '4px' }}>Separation of Concerns</strong>
            Moderation actions never alter an issue&apos;s civic workflow status (e.g. IN_PROGRESS) or civic priority calculation.
          </div>
        </div>
      </div>
    </div>
  );
}
