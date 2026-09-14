import React from 'react';
import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { getIssueById, getIssueImageUrl, getResolutionEvidence } from '../../../lib/api/issues';
import { getIssueComments } from '../../../lib/api/comments';
import { getIssueActivity, getIssueStatusHistory } from '../../../lib/api/activity';
import { StatusBadge } from '../../../components/StatusBadge';
import { PriorityBadge } from '../../../components/PriorityBadge';
import { StatusTimeline } from '../../../components/StatusTimeline';
import { ActivityTimeline } from '../../../components/ActivityTimeline';
import { CommentList } from '../../../components/CommentList';
import { ShareButton } from '../../../components/ShareButton';
import { IssueSupportButton } from '../../../components/IssueSupportButton';
import { Breadcrumbs } from '../../../components/Breadcrumbs';
import { formatRelativeTime, formatFullDate } from '../../../lib/formatters';
import { generateIssueJsonLd, getPublicSiteUrl } from '../../../lib/seo';
import type { ResolutionEvidenceResponse } from '../../../types/issue';

export const revalidate = 30; // ISR cache revalidation every 30s

interface IssueDetailPageProps {
  params: Promise<{
    issueId: string;
  }>;
}

export async function generateMetadata({ params }: IssueDetailPageProps): Promise<Metadata> {
  const { issueId } = await params;
  const siteUrl = getPublicSiteUrl();

  try {
    const issue = await getIssueById(issueId);
    const wardName =
      issue.civicResponsibility?.ward?.name ||
      issue.civicArea?.ward?.name ||
      issue.civicResponsibility?.city?.name ||
      issue.civicArea?.city ||
      'Ahmedabad';

    const safeDescription = `${(issue.description || '').slice(0, 160)} — Reported in ${wardName}, verified civic report.`;
    const primaryMediaId = issue.media && issue.media.length > 0 ? issue.media[0].id : undefined;
    const imageUrl = getIssueImageUrl(issue.id, primaryMediaId);

    return {
      title: `${issue.title}`,
      description: safeDescription,
      alternates: {
        canonical: `${siteUrl}/issues/${issue.id}`,
      },
      openGraph: {
        title: `${issue.title} | Nagrivic`,
        description: safeDescription,
        url: `${siteUrl}/issues/${issue.id}`,
        type: 'article',
        images: imageUrl ? [{ url: imageUrl, alt: issue.title }] : undefined,
      },
    };
  } catch {
    return {
      title: 'Civic Issue Detail | Nagrivic',
      description: 'View verified civic reports and resolution tracking on Nagrivic.',
    };
  }
}

export default async function IssueDetailPage({ params }: IssueDetailPageProps) {
  const { issueId } = await params;

  let issue;
  try {
    issue = await getIssueById(issueId);
  } catch (err: any) {
    if (err?.status === 404) {
      notFound();
    }
    throw err;
  }

  if (!issue) {
    notFound();
  }

  // Fetch comments, status history, activity, and resolution evidence in parallel
  const [commentsRes, historyRes, activityRes, evidenceRes] = await Promise.allSettled([
    getIssueComments(issue.id, 0, 30),
    getIssueStatusHistory(issue.id),
    getIssueActivity(issue.id, 0, 30),
    getResolutionEvidence(issue.id),
  ]);

  const comments = commentsRes.status === 'fulfilled' ? commentsRes.value.content : [];
  const statusHistory = historyRes.status === 'fulfilled' ? historyRes.value : null;
  const activities = activityRes.status === 'fulfilled' ? activityRes.value.content : [];
  const resolutionEvidence: ResolutionEvidenceResponse[] =
    evidenceRes.status === 'fulfilled' ? evidenceRes.value : [];

  const primaryMediaId = issue.media && issue.media.length > 0 ? issue.media[0].id : undefined;
  const imageUrl = getIssueImageUrl(issue.id, primaryMediaId);
  const jsonLd = generateIssueJsonLd(issue);

  // Civic responsibility breakdown
  const resp = issue.civicResponsibility;
  const isAuthorityDetermined =
    Boolean(resp?.civicBody?.name) ||
    Boolean(resp?.department?.name) ||
    Boolean(issue.civicArea?.civicBody?.name) ||
    Boolean(issue.civicArea?.department?.name);

  const civicBodyName = resp?.civicBody?.name || issue.civicArea?.civicBody?.name;
  const departmentName = resp?.department?.name || issue.civicArea?.department?.name;
  const wardName = resp?.ward?.name || issue.civicArea?.ward?.name;
  const cityName = resp?.city?.name || issue.civicArea?.city;

  return (
    <div className="container" style={{ padding: '2rem 1.25rem 5rem' }}>
      {/* JSON-LD Structured Data */}
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: jsonLd }}
      />

      <Breadcrumbs
        items={[
          { label: 'Home', href: '/' },
          { label: 'Issues', href: '/issues' },
          { label: issue.title },
        ]}
      />

      {/* Duplicate Warning Banner */}
      {issue.isDuplicate && (
        <div
          style={{
            padding: '1.25rem',
            backgroundColor: 'var(--color-status-reported-bg)',
            border: '1px solid var(--color-status-reported-border)',
            borderRadius: 'var(--radius-lg)',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#92400e', fontWeight: 700 }}>
              <span>⚠️</span> This issue is linked to another report.
            </div>
            <p style={{ fontSize: '0.9rem', color: '#78350f', marginTop: '4px' }}>
              This submission was flagged as a duplicate of an existing report. Follow the primary report for live municipal resolution milestones.
            </p>
          </div>

          {issue.primaryIssueId && (
            <Link
              href={`/issues/${issue.primaryIssueId}`}
              className="btn btn-primary btn-sm"
              style={{ backgroundColor: '#b45309' }}
            >
              View Primary Issue →
            </Link>
          )}
        </div>
      )}

      {/* Main Two-Column Layout */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '2.5rem',
          alignItems: 'flex-start',
        }}
      >
        {/* Left Column: Media & Primary Details */}
        <div>
          {/* Media Container */}
          <div
            style={{
              width: '100%',
              minHeight: '280px',
              maxHeight: '460px',
              borderRadius: 'var(--radius-xl)',
              overflow: 'hidden',
              backgroundColor: 'var(--color-surface-muted)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: '1px solid var(--color-border)',
              marginBottom: '1.5rem',
            }}
          >
            {imageUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={imageUrl}
                alt={issue.title}
                style={{ width: '100%', height: '100%', objectFit: 'contain' }}
              />
            ) : (
              <div style={{ textAlign: 'center', color: 'var(--color-text-muted)', padding: '2rem' }}>
                <span style={{ fontSize: '3rem', display: 'block', marginBottom: '0.5rem' }}>📷</span>
                <p style={{ fontSize: '0.95rem' }}>No photo evidence attached to this report.</p>
              </div>
            )}
          </div>

          {/* Core Info Card */}
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '0.75rem' }}>
              <StatusBadge status={issue.status} />
              {issue.priority && <PriorityBadge level={issue.priority.level} score={issue.priority.score} />}
              <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                • Reported {formatRelativeTime(issue.createdAt)}
              </span>
            </div>

            <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--color-primary)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.5rem' }}>
              {issue.category?.name || 'General Civic'}
            </div>

            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--color-text)', letterSpacing: '-0.02em', lineHeight: 1.3, marginBottom: '1rem' }}>
              {issue.title}
            </h1>

            <p style={{ fontSize: '1.05rem', color: 'var(--color-text-secondary)', lineHeight: 1.7, marginBottom: '1.5rem', whiteSpace: 'pre-line' }}>
              {issue.description}
            </p>

            {/* Timestamps Row */}
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', borderTop: '1px solid var(--color-border-light)', paddingTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
              <div>Submitted on: {formatFullDate(issue.createdAt)}</div>
              {issue.updatedAt && issue.updatedAt !== issue.createdAt && (
                <div>Last updated: {formatFullDate(issue.updatedAt)}</div>
              )}
            </div>
          </div>

          {/* Civic Responsibility Card */}
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <span style={{ fontSize: '1.25rem' }}>🛡️</span>
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--color-text)' }}>
                Civic Responsibility
              </h2>
            </div>

            {isAuthorityDetermined ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.925rem' }}>
                {departmentName && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--color-border-light)', paddingBottom: '0.5rem' }}>
                    <span style={{ color: 'var(--color-text-secondary)' }}>Department:</span>
                    <strong style={{ color: 'var(--color-text)' }}>{departmentName}</strong>
                  </div>
                )}
                {civicBodyName && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--color-border-light)', paddingBottom: '0.5rem' }}>
                    <span style={{ color: 'var(--color-text-secondary)' }}>Civic Body:</span>
                    <strong style={{ color: 'var(--color-text)' }}>{civicBodyName}</strong>
                  </div>
                )}
                {wardName && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--color-border-light)', paddingBottom: '0.5rem' }}>
                    <span style={{ color: 'var(--color-text-secondary)' }}>Municipal Ward:</span>
                    <strong style={{ color: 'var(--color-text)' }}>{wardName}</strong>
                  </div>
                )}
                {cityName && (
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: 'var(--color-text-secondary)' }}>City:</span>
                    <strong style={{ color: 'var(--color-text)' }}>{cityName}</strong>
                  </div>
                )}
              </div>
            ) : (
              <div style={{ padding: '0.75rem 0', color: 'var(--color-text-secondary)', fontSize: '0.925rem' }}>
                <p>Responsible civic authority is being determined.</p>
                <p style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', marginTop: '4px' }}>
                  Municipal routing polygons inspect and assign departments upon report verification.
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Status Progression, Activity & Discussion */}
        <div>
          {/* Action & Engagement Bar */}
          <div className="card" style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
            <div>
              <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--color-primary)' }}>
                👍 {issue.supportCount} {issue.supportCount === 1 ? 'Citizen' : 'Citizens'}
              </div>
              <span style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                backed this civic report
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
              <ShareButton
                title={issue.title}
                categoryName={issue.category?.name}
              />
              <IssueSupportButton
                issueId={issue.id}
                initialSupportCount={issue.supportCount}
              />
              <Link href="/#report" className="btn btn-outline btn-sm" style={{ fontSize: '0.8rem' }}>
                Support via App
              </Link>
            </div>
          </div>

          {/* Municipal Resolution Evidence Card */}
          {(['RESOLVED', 'CITIZEN_VERIFIED', 'NOT_FIXED'].includes(issue.status) || resolutionEvidence.length > 0) && (
            <section
              className="card"
              aria-labelledby="resolution-evidence-heading"
              style={{
                marginBottom: '1.5rem',
                border: '1px solid var(--color-border)',
                borderLeft: '4px solid var(--color-primary)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <span style={{ fontSize: '1.25rem' }}>📋</span>
                  <h2 id="resolution-evidence-heading" style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--color-text)' }}>
                    Authority Resolution Evidence
                  </h2>
                </div>
                {resolutionEvidence.length > 0 && (
                  <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-primary)', backgroundColor: 'var(--color-surface-muted)', padding: '2px 8px', borderRadius: 'var(--radius-full)' }}>
                    {resolutionEvidence.length} {resolutionEvidence.length === 1 ? 'record' : 'records'}
                  </span>
                )}
              </div>

              <p style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)', marginBottom: '1rem' }}>
                Evidence submitted by civic authorities documenting operational work and completion.
              </p>

              {/* Citizen Verification Status Distinction Banner */}
              <div
                style={{
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.85rem',
                  marginBottom: '1rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.625rem',
                  backgroundColor:
                    issue.status === 'CITIZEN_VERIFIED'
                      ? 'var(--color-status-verified-bg, #ecfdf5)'
                      : issue.status === 'NOT_FIXED'
                      ? 'var(--color-status-not-fixed-bg, #fef2f2)'
                      : 'var(--color-status-in-progress-bg, #eff6ff)',
                  color:
                    issue.status === 'CITIZEN_VERIFIED'
                      ? '#065f46'
                      : issue.status === 'NOT_FIXED'
                      ? '#991b1b'
                      : '#1e40af',
                  border: `1px solid ${
                    issue.status === 'CITIZEN_VERIFIED'
                      ? '#a7f3d0'
                      : issue.status === 'NOT_FIXED'
                      ? '#fecaca'
                      : '#bfdbfe'
                  }`,
                }}
              >
                <span>
                  {issue.status === 'CITIZEN_VERIFIED' ? '✅' : issue.status === 'NOT_FIXED' ? '⚠️' : 'ℹ️'}
                </span>
                <div>
                  <strong>
                    {issue.status === 'CITIZEN_VERIFIED'
                      ? 'Citizen Confirmed'
                      : issue.status === 'NOT_FIXED'
                      ? 'Citizen Contested'
                      : 'Citizen Verification Pending'}
                    :
                  </strong>{' '}
                  {issue.status === 'CITIZEN_VERIFIED'
                    ? 'The reporting citizen independently inspected and verified that this issue has been resolved.'
                    : issue.status === 'NOT_FIXED'
                    ? 'The reporting citizen indicated that the issue was not adequately fixed. Authority evidence remains on record.'
                    : 'The civic authority has submitted resolution documentation. Citizen confirmation is still pending.'}
                </div>
              </div>

              {/* Resolution Evidence Items or Neutral Notice */}
              {resolutionEvidence.length === 0 ? (
                <div
                  style={{
                    padding: '1rem',
                    backgroundColor: 'var(--color-surface-muted)',
                    borderRadius: 'var(--radius-md)',
                    color: 'var(--color-text-secondary)',
                    fontSize: '0.875rem',
                    fontStyle: 'italic',
                  }}
                >
                  Authority has marked this issue resolved, but no resolution evidence has been provided.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  {resolutionEvidence.map((ev) => (
                    <div
                      key={ev.id}
                      style={{
                        padding: '1rem',
                        backgroundColor: 'var(--color-surface-muted)',
                        borderRadius: 'var(--radius-md)',
                        border: '1px solid var(--color-border-light)',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
                        <span
                          style={{
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            textTransform: 'uppercase',
                            letterSpacing: '0.04em',
                            padding: '2px 8px',
                            borderRadius: 'var(--radius-sm)',
                            backgroundColor:
                              ev.evidenceType === 'COMPLETION_PHOTO'
                                ? '#dbeafe'
                                : ev.evidenceType === 'BEFORE_AFTER_PHOTO'
                                ? '#fef3c7'
                                : '#f3e8ff',
                            color:
                              ev.evidenceType === 'COMPLETION_PHOTO'
                                ? '#1e40af'
                                : ev.evidenceType === 'BEFORE_AFTER_PHOTO'
                                ? '#92400e'
                                : '#6b21a8',
                          }}
                        >
                          {ev.evidenceType === 'COMPLETION_PHOTO'
                            ? 'Completion Photo'
                            : ev.evidenceType === 'BEFORE_AFTER_PHOTO'
                            ? 'Before / After Evidence'
                            : 'Operational Note'}
                        </span>
                        <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                          {formatRelativeTime(ev.createdAt)}
                        </span>
                      </div>

                      {/* Photo Display */}
                      {ev.mediaUrl && (
                        <div
                          style={{
                            marginBottom: '0.75rem',
                            borderRadius: 'var(--radius-md)',
                            overflow: 'hidden',
                            backgroundColor: '#000',
                            maxHeight: '360px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                          }}
                        >
                          {/* eslint-disable-next-line @next/next/no-img-element */}
                          <img
                            src={ev.mediaUrl}
                            alt={ev.note || 'Resolution evidence photo'}
                            style={{
                              maxWidth: '100%',
                              maxHeight: '360px',
                              objectFit: 'contain',
                            }}
                          />
                        </div>
                      )}

                      {/* Notes / Description */}
                      {ev.note && (
                        <p
                          style={{
                            fontSize: '0.9rem',
                            color: 'var(--color-text)',
                            lineHeight: 1.5,
                            whiteSpace: 'pre-line',
                            margin: 0,
                          }}
                        >
                          {ev.note}
                        </p>
                      )}

                      {/* Attribution footer */}
                      <div
                        style={{
                          fontSize: '0.75rem',
                          color: 'var(--color-text-muted)',
                          marginTop: '0.5rem',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem',
                        }}
                      >
                        <span>🏛️ Civic Authority Evidence</span>
                        {ev.capturedAt && (
                          <span>• Captured: {formatFullDate(ev.capturedAt)}</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </section>
          )}

          {/* Status Progression Flowchart */}
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--color-text)', marginBottom: '0.75rem' }}>
              Official Status Progression
            </h2>
            <StatusTimeline
              currentStatus={issue.status}
              history={statusHistory}
            />
          </div>

          {/* Activity Timeline */}
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--color-text)', marginBottom: '1rem' }}>
              Public Activity Stream ({activities.length})
            </h2>
            <ActivityTimeline activities={activities} />
          </div>

          {/* Discussion & Community Comments */}
          <div className="card">
            <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--color-text)', marginBottom: '1rem' }}>
              Community Discussion ({comments.length})
            </h2>
            <CommentList comments={comments} issueId={issue.id} />
          </div>
        </div>
      </div>
    </div>
  );
}
