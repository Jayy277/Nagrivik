import React from 'react';
import type { Metadata } from 'next';
import Link from 'next/link';
import { getPublicAccountability } from '../../lib/api/accountability';
import { AccountabilityFilters } from '../../components/AccountabilityFilters';
import { Breadcrumbs } from '../../components/Breadcrumbs';
import { formatRelativeTime, formatFullDate } from '../../lib/formatters';
import { getPublicSiteUrl } from '../../lib/seo';
import type { PublicAccountabilityResponse } from '../../types/accountability';

export const revalidate = 60; // ISR cache revalidation every 60 seconds

interface AccountabilityPageProps {
  searchParams: Promise<{
    cityId?: string;
    wardId?: string;
    categoryId?: string;
    range?: string;
  }>;
}

export async function generateMetadata(): Promise<Metadata> {
  const siteUrl = getPublicSiteUrl();

  return {
    title: 'Civic Accountability Dashboard | Ahmedabad | Nagrivic',
    description:
      'Public, evidence-based civic accountability dashboard summarizing citizen-reported defect progression, ward performance, category breakdowns, and independent resolution verification in Ahmedabad.',
    alternates: {
      canonical: `${siteUrl}/accountability`,
    },
    openGraph: {
      title: 'Nagrivic Civic Accountability Dashboard | Ahmedabad',
      description:
        'Explore verified municipal resolution milestones, ward-level progress, and independent citizen verification across Ahmedabad.',
      url: `${siteUrl}/accountability`,
      type: 'website',
    },
  };
}

export default async function AccountabilityPage({ searchParams }: AccountabilityPageProps) {
  const resolvedParams = await searchParams;

  let data: PublicAccountabilityResponse;
  try {
    data = await getPublicAccountability(resolvedParams);
  } catch (err: unknown) {
    return (
      <div className="container" style={{ padding: '3rem 1.25rem' }}>
        <div className="card" style={{ padding: '2rem', textAlign: 'center', border: '1px solid #fecaca', backgroundColor: '#fef2f2' }}>
          <h2 style={{ fontSize: '1.25rem', color: '#991b1b', marginBottom: '0.5rem' }}>
            Accountability Data Temporarily Unavailable
          </h2>
          <p style={{ color: '#7f1d1d', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
            Unable to fetch live municipal accountability statistics. Please check back shortly.
          </p>
          <Link href="/issues" className="btn btn-primary btn-sm">
            Browse Public Issues
          </Link>
        </div>
      </div>
    );
  }

  const {
    cityName = 'Ahmedabad',
    wardName,
    categoryName,
    range,
    summary,
    statusBreakdown,
    priorityBreakdown,
    categoryBreakdown,
    wardBreakdown,
    agingBreakdown,
    verificationSummary,
    responsibilitySummary,
    trend,
    lastUpdated,
  } = data;

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'DataCatalog',
    name: `${cityName} Civic Accountability Dashboard`,
    description: 'Verifiable civic defect reporting, operational progress, and independent citizen resolution statistics.',
    provider: {
      '@type': 'Organization',
      name: 'Nagrivic',
      url: getPublicSiteUrl(),
    },
  };

  return (
    <div className="container" style={{ padding: '2rem 1.25rem 5rem' }}>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />

      <Breadcrumbs
        items={[
          { label: 'Home', href: '/' },
          { label: 'Accountability Dashboard' },
        ]}
      />

      {/* Header Banner */}
      <div style={{ marginBottom: '2rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem', marginBottom: '0.75rem' }}>
          <div>
            <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem', backgroundColor: 'var(--color-primary-light, #eff6ff)', color: 'var(--color-primary, #0284c7)', padding: '4px 12px', borderRadius: 'var(--radius-full)', fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.5rem' }}>
              <span>🏛️</span> Public Civic Transparency
            </div>
            <h1 style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--color-text)', letterSpacing: '-0.02em', margin: 0 }}>
              {cityName} Civic Accountability Dashboard
            </h1>
          </div>

          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', textAlign: 'right' }}>
            <div>Last updated from database:</div>
            <strong style={{ color: 'var(--color-text)' }}>{formatFullDate(lastUpdated)}</strong>{' '}
            <span>({formatRelativeTime(lastUpdated)})</span>
          </div>
        </div>

        <p style={{ fontSize: '1.05rem', color: 'var(--color-text-secondary)', lineHeight: 1.6, maxWidth: '840px', margin: 0 }}>
          Verifiable civic defect progression, municipal remediation milestones, and independent citizen resolution tracking. All statistics are aggregated directly from authentic Nagrivic issue reports.
        </p>

        {/* Active Scope Pill */}
        {(wardName || categoryName) && (
          <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {wardName && (
              <span style={{ fontSize: '0.8rem', padding: '3px 10px', backgroundColor: '#e0f2fe', color: '#0369a1', borderRadius: 'var(--radius-full)', fontWeight: 600 }}>
                Ward: {wardName}
              </span>
            )}
            {categoryName && (
              <span style={{ fontSize: '0.8rem', padding: '3px 10px', backgroundColor: '#fef3c7', color: '#92400e', borderRadius: 'var(--radius-full)', fontWeight: 600 }}>
                Category: {categoryName}
              </span>
            )}
          </div>
        )}
      </div>

      {/* Filter Controls */}
      <AccountabilityFilters
        wards={wardBreakdown}
        categories={categoryBreakdown}
        selectedWardId={resolvedParams.wardId}
        selectedCategoryId={resolvedParams.categoryId}
        selectedRange={range}
      />

      {/* Summary Metrics Cards */}
      <section aria-labelledby="summary-metrics-heading" style={{ marginBottom: '2.5rem' }}>
        <h2 id="summary-metrics-heading" className="sr-only">
          Executive Summary Metrics
        </h2>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: '1rem',
          }}
        >
          {/* Total Public Reports */}
          <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid var(--color-primary)' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.25rem' }}>
              Total Public Reports
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: 'var(--color-text)', lineHeight: 1.2 }}>
              {summary.totalPublicIssues.toLocaleString()}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
              Canonical reports (duplicates excluded)
            </div>
          </div>

          {/* Actionable / Open */}
          <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #f59e0b' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.25rem' }}>
              Open / In Progress
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#b45309', lineHeight: 1.2 }}>
              {summary.actionableCount.toLocaleString()}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
              Under review or field crew repair
            </div>
          </div>

          {/* Resolved by Authority */}
          <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #0284c7' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.25rem' }}>
              Resolved by Authority
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#0369a1', lineHeight: 1.2 }}>
              {summary.resolvedCount.toLocaleString()}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
              Work completed, awaiting verification
            </div>
          </div>

          {/* Citizen Verified */}
          <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #10b981' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.25rem' }}>
              Citizen Confirmed
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#047857', lineHeight: 1.2 }}>
              {summary.citizenVerifiedCount.toLocaleString()}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
              Independently verified fixed
            </div>
          </div>

          {/* Citizen Contested */}
          <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #ef4444' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.25rem' }}>
              Reported Not Fixed
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#b91c1c', lineHeight: 1.2 }}>
              {summary.notFixedCount.toLocaleString()}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
              Citizen contested, returned to queue
            </div>
          </div>

          {/* Critical & High Priority */}
          <div className="card" style={{ padding: '1.25rem', borderLeft: '4px solid #8b5cf6' }}>
            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.25rem' }}>
              High & Critical Hazard
            </div>
            <div style={{ fontSize: '2rem', fontWeight: 800, color: '#6d28d9', lineHeight: 1.2 }}>
              {(summary.highPriorityCount + summary.criticalPriorityCount).toLocaleString()}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
              {summary.criticalPriorityCount} critical, {summary.highPriorityCount} high priority
            </div>
          </div>
        </div>
      </section>

      {/* Two Column Layout: Resolution Funnel & Citizen Verification */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '1.5rem',
          marginBottom: '2.5rem',
        }}
      >
        {/* Resolution Funnel */}
        <section className="card" aria-labelledby="resolution-funnel-heading">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '1.2rem' }}>📊</span>
            <h2 id="resolution-funnel-heading" style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
              Operational Lifecycle Funnel
            </h2>
          </div>
          <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginBottom: '1.25rem' }}>
            Actual count of public issues currently at each stage of the civic resolution process.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {[
              { label: '1. Reported (Queued for Triage)', count: statusBreakdown.reported, color: '#f59e0b' },
              { label: '2. Verified (Inspection Confirmed)', count: statusBreakdown.verified, color: '#0284c7' },
              { label: '3. Acknowledged (Work Order Assigned)', count: statusBreakdown.acknowledged, color: '#6366f1' },
              { label: '4. In Progress (Crews Dispatched)', count: statusBreakdown.inProgress, color: '#ec4899' },
              { label: '5. Resolved by Authority (Work Finished)', count: statusBreakdown.resolved, color: '#0ea5e9' },
              { label: '6. Citizen Confirmed (Successfully Verified)', count: statusBreakdown.citizenVerified, color: '#10b981' },
            ].map((step) => {
              const pct = summary.totalPublicIssues > 0
                ? Math.round((step.count / summary.totalPublicIssues) * 100)
                : 0;
              return (
                <div key={step.label}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                    <span style={{ fontWeight: 600, color: 'var(--color-text)' }}>{step.label}</span>
                    <span style={{ color: 'var(--color-text-secondary)' }}>
                      <strong>{step.count.toLocaleString()}</strong> ({pct}%)
                    </span>
                  </div>
                  <div style={{ height: '8px', backgroundColor: 'var(--color-surface-muted)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                    <div
                      style={{
                        height: '100%',
                        width: `${pct}%`,
                        backgroundColor: step.color,
                        borderRadius: 'var(--radius-full)',
                        transition: 'width 0.3s ease',
                      }}
                      role="progressbar"
                      aria-valuenow={step.count}
                      aria-valuemin={0}
                      aria-valuemax={summary.totalPublicIssues}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          {/* Not Fixed Callout */}
          {statusBreakdown.notFixed > 0 && (
            <div
              style={{
                marginTop: '1.25rem',
                padding: '0.75rem 1rem',
                backgroundColor: 'var(--color-status-not-fixed-bg, #fef2f2)',
                border: '1px solid #fecaca',
                borderRadius: 'var(--radius-md)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                fontSize: '0.85rem',
                color: '#991b1b',
              }}
            >
              <span>⚠️ Citizen Reported Not Fixed (Reopened for Remediation):</span>
              <strong>{statusBreakdown.notFixed.toLocaleString()} issues</strong>
            </div>
          )}
        </section>

        {/* Independent Citizen Verification Card */}
        <section className="card" aria-labelledby="citizen-verification-heading">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <span style={{ fontSize: '1.2rem' }}>✅</span>
            <h2 id="citizen-verification-heading" style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
              Citizen Resolution Verification
            </h2>
          </div>
          <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginBottom: '1.25rem' }}>
            In Nagrivic, an issue is only marked <em>Citizen Confirmed</em> after the reporting citizen independently confirms the fix on site.
          </p>

          <div
            style={{
              padding: '1.25rem',
              backgroundColor: 'var(--color-surface-muted)',
              borderRadius: 'var(--radius-md)',
              marginBottom: '1rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div>
              <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: 600, textTransform: 'uppercase' }}>
                Citizen Confirmation Rate
              </div>
              <div style={{ fontSize: '2.25rem', fontWeight: 800, color: '#047857' }}>
                {verificationSummary.verificationRate}%
              </div>
              <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                of all {verificationSummary.resolvedByAuthority} authority-resolved issues
              </div>
            </div>

            <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.85rem' }}>
              <div>
                <span style={{ color: '#047857', fontWeight: 700 }}>{verificationSummary.citizenVerified}</span> Confirmed
              </div>
              <div>
                <span style={{ color: '#b91c1c', fontWeight: 700 }}>{verificationSummary.citizenReportedNotFixed}</span> Contested
              </div>
              <div>
                <span style={{ color: '#0369a1', fontWeight: 700 }}>{verificationSummary.verificationPending}</span> Pending Review
              </div>
            </div>
          </div>

          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', lineHeight: 1.5 }}>
            <p style={{ margin: '0 0 0.5rem' }}>
              <strong>Authority Resolution Claim:</strong> Municipal crews report physical completion and attach resolution evidence (photos/work notes).
            </p>
            <p style={{ margin: 0 }}>
              <strong>Citizen Verification:</strong> Original reporters hold the independent power to accept or dispute completion. Authority claims do not guarantee physical remediation.
            </p>
          </div>
        </section>
      </div>

      {/* Category Breakdown */}
      <section className="card" aria-labelledby="category-breakdown-heading" style={{ marginBottom: '2.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1rem' }}>
          <div>
            <h2 id="category-breakdown-heading" style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
              Civic Category Breakdown
            </h2>
            <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', margin: '4px 0 0' }}>
              Public defect volume and remediation status classified by civic domain.
            </p>
          </div>
          <Link href="/issues" className="btn btn-outline btn-sm" style={{ fontSize: '0.8rem' }}>
            Explore All Issues →
          </Link>
        </div>

        {/* Category Table */}
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid var(--color-border)', textAlign: 'left', color: 'var(--color-text-secondary)' }}>
                <th style={{ padding: '0.75rem 0.5rem' }}>Category</th>
                <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Total</th>
                <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Open / Actionable</th>
                <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>In Progress</th>
                <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Resolved</th>
                <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Citizen Verified</th>
                <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Action</th>
              </tr>
            </thead>
            <tbody>
              {categoryBreakdown.map((cat) => (
                <tr key={cat.categoryId} style={{ borderBottom: '1px solid var(--color-border-light)' }}>
                  <td style={{ padding: '0.75rem 0.5rem', fontWeight: 600, color: 'var(--color-text)' }}>
                    {cat.name}
                  </td>
                  <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', fontWeight: 700 }}>
                    {cat.total.toLocaleString()}
                  </td>
                  <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: '#b45309' }}>
                    {cat.openActionable.toLocaleString()}
                  </td>
                  <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: '#ec4899' }}>
                    {cat.inProgress.toLocaleString()}
                  </td>
                  <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: '#0369a1' }}>
                    {cat.resolved.toLocaleString()}
                  </td>
                  <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: '#047857', fontWeight: 600 }}>
                    {cat.citizenVerified.toLocaleString()}
                  </td>
                  <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>
                    <Link
                      href={`/issues?category=${cat.slug}`}
                      className="btn btn-outline btn-sm"
                      style={{ fontSize: '0.75rem', padding: '2px 8px' }}
                    >
                      View →
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Ward Breakdown */}
      <section className="card" aria-labelledby="ward-breakdown-heading" style={{ marginBottom: '2.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1rem' }}>
          <div>
            <h2 id="ward-breakdown-heading" style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
              Authoritative Ward Progression
            </h2>
            <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', margin: '4px 0 0' }}>
              Civic issue counts aggregated exclusively by authoritative municipal ward boundaries in {cityName}.
            </p>
          </div>
          <Link href="/map" className="btn btn-outline btn-sm" style={{ fontSize: '0.8rem' }}>
            View Civic Map →
          </Link>
        </div>

        {wardBreakdown.length === 0 ? (
          <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '0.9rem' }}>
            No authoritative ward records found for the selected filter.
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid var(--color-border)', textAlign: 'left', color: 'var(--color-text-secondary)' }}>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Ward</th>
                  <th style={{ padding: '0.75rem 0.5rem' }}>Code</th>
                  <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Total</th>
                  <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Open</th>
                  <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>In Progress</th>
                  <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Resolved</th>
                  <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>Verified</th>
                  <th style={{ padding: '0.75rem 0.5rem', textAlign: 'right' }}>High/Critical</th>
                </tr>
              </thead>
              <tbody>
                {wardBreakdown.map((w) => (
                  <tr key={w.wardId} style={{ borderBottom: '1px solid var(--color-border-light)' }}>
                    <td style={{ padding: '0.75rem 0.5rem', fontWeight: 600, color: 'var(--color-text)' }}>
                      {w.wardNumber ? `Ward ${w.wardNumber} — ${w.name}` : w.name}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>
                      {w.wardCode || '—'}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', fontWeight: 700 }}>
                      {w.total.toLocaleString()}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: '#b45309' }}>
                      {w.openActionable.toLocaleString()}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: '#ec4899' }}>
                      {w.inProgress.toLocaleString()}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: '#0369a1' }}>
                      {w.resolved.toLocaleString()}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: '#047857', fontWeight: 600 }}>
                      {w.citizenVerified.toLocaleString()}
                    </td>
                    <td style={{ padding: '0.75rem 0.5rem', textAlign: 'right', color: w.highOrCriticalCount > 0 ? '#6d28d9' : 'var(--color-text-muted)', fontWeight: w.highOrCriticalCount > 0 ? 700 : 400 }}>
                      {w.highOrCriticalCount.toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Two Column: Analytical Aging Distribution & Priority Triage */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '1.5rem',
          marginBottom: '2.5rem',
        }}
      >
        {/* Analytical Aging Distribution */}
        <section className="card" aria-labelledby="aging-breakdown-heading">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '1.2rem' }}>⏱️</span>
            <h2 id="aging-breakdown-heading" style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
              Analytical Age Distribution (Open Issues)
            </h2>
          </div>
          <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginBottom: '1.25rem' }}>
            Elapsed calendar time since submission for active unresolved reports.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1rem' }}>
            {[
              { label: '0 – 1 Day (New)', count: agingBreakdown.zeroToOneDay, color: '#10b981' },
              { label: '2 – 7 Days (Recent)', count: agingBreakdown.twoToSevenDays, color: '#0284c7' },
              { label: '8 – 30 Days (Ongoing)', count: agingBreakdown.eightToThirtyDays, color: '#f59e0b' },
              { label: '31 – 90 Days (Aging)', count: agingBreakdown.thirtyOneToNinetyDays, color: '#f97316' },
              { label: '90+ Days (Prolonged)', count: agingBreakdown.overNinetyDays, color: '#ef4444' },
            ].map((bucket) => {
              const totalOpen = summary.actionableCount;
              const pct = totalOpen > 0 ? Math.round((bucket.count / totalOpen) * 100) : 0;
              return (
                <div key={bucket.label}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                    <span style={{ fontWeight: 600, color: 'var(--color-text)' }}>{bucket.label}</span>
                    <span style={{ color: 'var(--color-text-secondary)' }}>
                      <strong>{bucket.count.toLocaleString()}</strong> ({pct}%)
                    </span>
                  </div>
                  <div style={{ height: '8px', backgroundColor: 'var(--color-surface-muted)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                    <div
                      style={{
                        height: '100%',
                        width: `${pct}%`,
                        backgroundColor: bucket.color,
                        borderRadius: 'var(--radius-full)',
                      }}
                      role="progressbar"
                      aria-valuenow={bucket.count}
                      aria-valuemin={0}
                      aria-valuemax={totalOpen}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          <div style={{ padding: '0.75rem', backgroundColor: 'var(--color-surface-muted)', borderRadius: 'var(--radius-md)', fontSize: '0.75rem', color: 'var(--color-text-muted)', lineHeight: 1.4 }}>
            ℹ️ <strong>Non-SLA Disclaimer:</strong> Age brackets reflect analytical age distribution of reports on Nagrivic. They do not constitute statutory government Service Level Agreements (SLAs).
          </div>
        </section>

        {/* Priority Triage Breakdown */}
        <section className="card" aria-labelledby="priority-breakdown-heading">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <span style={{ fontSize: '1.2rem' }}>🎯</span>
            <h2 id="priority-breakdown-heading" style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
              Civic Impact & Priority Triage
            </h2>
          </div>
          <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginBottom: '1.25rem' }}>
            Algorithmic priority score derived from physical hazard, public safety impact, citizen backing, and age.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1rem' }}>
            {[
              { label: 'Critical Priority (Score 80–100)', count: priorityBreakdown.critical, color: '#dc2626' },
              { label: 'High Priority (Score 60–79)', count: priorityBreakdown.high, color: '#ea580c' },
              { label: 'Medium Priority (Score 40–59)', count: priorityBreakdown.medium, color: '#0284c7' },
              { label: 'Low Priority (Score 0–39)', count: priorityBreakdown.low, color: '#64748b' },
            ].map((p) => {
              const total = summary.totalPublicIssues;
              const pct = total > 0 ? Math.round((p.count / total) * 100) : 0;
              return (
                <div key={p.label}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', marginBottom: '4px' }}>
                    <span style={{ fontWeight: 600, color: 'var(--color-text)' }}>{p.label}</span>
                    <span style={{ color: 'var(--color-text-secondary)' }}>
                      <strong>{p.count.toLocaleString()}</strong> ({pct}%)
                    </span>
                  </div>
                  <div style={{ height: '8px', backgroundColor: 'var(--color-surface-muted)', borderRadius: 'var(--radius-full)', overflow: 'hidden' }}>
                    <div
                      style={{
                        height: '100%',
                        width: `${pct}%`,
                        backgroundColor: p.color,
                        borderRadius: 'var(--radius-full)',
                      }}
                      role="progressbar"
                      aria-valuenow={p.count}
                      aria-valuemin={0}
                      aria-valuemax={total}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          <div style={{ padding: '0.75rem', backgroundColor: 'var(--color-surface-muted)', borderRadius: 'var(--radius-md)', fontSize: '0.75rem', color: 'var(--color-text-muted)', lineHeight: 1.4 }}>
            ℹ️ Priority represents Nagrivic&apos;s open civic-impact model prioritizing severe hazards (exposed wires, deep craters) over cosmetic defects.
          </div>
        </section>
      </div>

      {/* Reporting & Resolution Trends */}
      {trend.length > 0 && (
        <section className="card" aria-labelledby="trend-heading" style={{ marginBottom: '2.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '1rem' }}>
            <div>
              <h2 id="trend-heading" style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
                Reporting & Resolution Activity Timeline ({range.toUpperCase()})
              </h2>
              <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', margin: '4px 0 0' }}>
                Daily activity volume for new citizen reports, municipal completions, and citizen verifications.
              </p>
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
              UTC Calendar Days ({trend.length} points)
            </div>
          </div>

          {/* Accessible Table */}
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid var(--color-border)', textAlign: 'left', color: 'var(--color-text-secondary)' }}>
                  <th style={{ padding: '0.5rem' }}>Date</th>
                  <th style={{ padding: '0.5rem', textAlign: 'right' }}>Reported</th>
                  <th style={{ padding: '0.5rem', textAlign: 'right' }}>Resolved by Authority</th>
                  <th style={{ padding: '0.5rem', textAlign: 'right' }}>Citizen Verified</th>
                </tr>
              </thead>
              <tbody>
                {trend.map((pt) => (
                  <tr key={pt.date} style={{ borderBottom: '1px solid var(--color-border-light)' }}>
                    <td style={{ padding: '0.5rem', fontWeight: 600, color: 'var(--color-text)' }}>{pt.date}</td>
                    <td style={{ padding: '0.5rem', textAlign: 'right', color: pt.reportedCount > 0 ? 'var(--color-primary)' : 'var(--color-text-muted)' }}>
                      {pt.reportedCount.toLocaleString()}
                    </td>
                    <td style={{ padding: '0.5rem', textAlign: 'right', color: pt.resolvedCount > 0 ? '#0369a1' : 'var(--color-text-muted)' }}>
                      {pt.resolvedCount.toLocaleString()}
                    </td>
                    <td style={{ padding: '0.5rem', textAlign: 'right', color: pt.citizenVerifiedCount > 0 ? '#047857' : 'var(--color-text-muted)', fontWeight: pt.citizenVerifiedCount > 0 ? 600 : 400 }}>
                      {pt.citizenVerifiedCount.toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Civic Responsibility Coverage */}
      <section className="card" aria-labelledby="responsibility-heading" style={{ marginBottom: '2.5rem' }}>
        <h2 id="responsibility-heading" style={{ fontSize: '1.25rem', fontWeight: 700, margin: '0 0 0.5rem', color: 'var(--color-text)' }}>
          Civic Responsibility & Data Quality Coverage
        </h2>
        <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginBottom: '1.25rem' }}>
          Nagrivic routes reported defects to authoritative departments using geographic point-in-polygon ward boundaries and category mapping tables.
        </p>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
            gap: '1rem',
            marginBottom: '1.5rem',
          }}
        >
          <div style={{ padding: '1rem', backgroundColor: 'var(--color-surface-muted)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: 600 }}>
              Responsibility Coverage Rate
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#0369a1', margin: '4px 0' }}>
              {responsibilitySummary.coveragePercentage}%
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
              {responsibilitySummary.resolvedCount} resolved / {responsibilitySummary.unresolvedCount} pending mapping
            </div>
          </div>

          <div style={{ padding: '1rem', backgroundColor: 'var(--color-surface-muted)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', fontWeight: 600 }}>
              Unresolved Responsibility
            </div>
            <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#b45309', margin: '4px 0' }}>
              {responsibilitySummary.unresolvedCount}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
              Awaiting municipal boundary or category rule refinement
            </div>
          </div>
        </div>

        {responsibilitySummary.departmentBreakdown.length > 0 && (
          <div style={{ overflowX: 'auto' }}>
            <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--color-text)', marginBottom: '0.75rem' }}>
              Departmental Issue Distribution
            </h3>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid var(--color-border)', textAlign: 'left', color: 'var(--color-text-secondary)' }}>
                  <th style={{ padding: '0.5rem' }}>Department</th>
                  <th style={{ padding: '0.5rem' }}>Code</th>
                  <th style={{ padding: '0.5rem', textAlign: 'right' }}>Total Assigned</th>
                  <th style={{ padding: '0.5rem', textAlign: 'right' }}>Open</th>
                  <th style={{ padding: '0.5rem', textAlign: 'right' }}>Resolved</th>
                  <th style={{ padding: '0.5rem', textAlign: 'right' }}>Verified</th>
                </tr>
              </thead>
              <tbody>
                {responsibilitySummary.departmentBreakdown.map((dept) => (
                  <tr key={dept.departmentId} style={{ borderBottom: '1px solid var(--color-border-light)' }}>
                    <td style={{ padding: '0.5rem', fontWeight: 600, color: 'var(--color-text)' }}>{dept.name}</td>
                    <td style={{ padding: '0.5rem', color: 'var(--color-text-muted)' }}>{dept.code}</td>
                    <td style={{ padding: '0.5rem', textAlign: 'right', fontWeight: 700 }}>{dept.totalAssigned.toLocaleString()}</td>
                    <td style={{ padding: '0.5rem', textAlign: 'right', color: '#b45309' }}>{dept.openActionable.toLocaleString()}</td>
                    <td style={{ padding: '0.5rem', textAlign: 'right', color: '#0369a1' }}>{dept.resolved.toLocaleString()}</td>
                    <td style={{ padding: '0.5rem', textAlign: 'right', color: '#047857', fontWeight: 600 }}>{dept.citizenVerified.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Methodology, Transparency & Neutrality Section */}
      <section
        className="card"
        aria-labelledby="methodology-heading"
        style={{
          borderLeft: '4px solid #64748b',
          backgroundColor: '#f8fafc',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
          <span style={{ fontSize: '1.2rem' }}>📜</span>
          <h2 id="methodology-heading" style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0, color: 'var(--color-text)' }}>
            Methodology, Data Integrity & Neutrality Principles
          </h2>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem', fontSize: '0.85rem', color: '#334155', lineHeight: 1.6 }}>
          <div>
            <h3 style={{ fontSize: '0.9rem', fontWeight: 700, color: '#0f172a', marginBottom: '0.25rem' }}>
              1. Non-Partisan & Objective
            </h3>
            <p style={{ margin: 0 }}>
              Nagrivic does not rate elected representatives, grade political parties, or allocate political blame. Metrics represent operational defect reporting and remediation facts exclusively.
            </p>
          </div>

          <div>
            <h3 style={{ fontSize: '0.9rem', fontWeight: 700, color: '#0f172a', marginBottom: '0.25rem' }}>
              2. Anti-Double Counting
            </h3>
            <p style={{ margin: 0 }}>
              When multiple citizens report the same physical defect, subsequent reports are linked as duplicates to a single canonical record. Duplicates are excluded from public aggregates so physical problems are counted exactly once.
            </p>
          </div>

          <div>
            <h3 style={{ fontSize: '0.9rem', fontWeight: 700, color: '#0f172a', marginBottom: '0.25rem' }}>
              3. Independent Verification
            </h3>
            <p style={{ margin: 0 }}>
              Authority resolution claims represent operational declarations of completion. Citizen verification represents independent citizen confirmation. The two are tracked and presented as separate milestones.
            </p>
          </div>

          <div>
            <h3 style={{ fontSize: '0.9rem', fontWeight: 700, color: '#0f172a', marginBottom: '0.25rem' }}>
              4. Platform Limitation Notice
            </h3>
            <p style={{ margin: 0 }}>
              Nagrivic is an independent civic transparency platform. These metrics describe data reported and processed through Nagrivic and do not constitute official statutory government performance reports.
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
