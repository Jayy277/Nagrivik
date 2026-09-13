import React from 'react';
import Link from 'next/link';
import { IssueResponse } from '../types/issue';
import { StatusBadge } from './StatusBadge';
import { PriorityBadge } from './PriorityBadge';
import { formatRelativeTime, formatDistance } from '../lib/formatters';
import { getIssueImageUrl } from '../lib/api/issues';

interface IssueCardProps {
  issue: IssueResponse;
}

export function IssueCard({ issue }: IssueCardProps) {
  const primaryMediaId = issue.media && issue.media.length > 0 ? issue.media[0].id : undefined;
  const imageUrl = getIssueImageUrl(issue.id, primaryMediaId);
  const distanceText = formatDistance(issue.distanceMeters);
  const relativeDate = formatRelativeTime(issue.createdAt);

  const wardOrCity =
    issue.civicResponsibility?.ward?.name ||
    issue.civicArea?.ward?.name ||
    issue.civicResponsibility?.city?.name ||
    issue.civicArea?.city ||
    'Local Area';

  return (
    <article className="card card-hover" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      {/* Thumbnail */}
      <div
        style={{
          width: '100%',
          height: '180px',
          borderRadius: 'var(--radius-md)',
          overflow: 'hidden',
          backgroundColor: 'var(--color-surface-muted)',
          position: 'relative',
          marginBottom: '1rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        {imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={imageUrl}
            alt={issue.title}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            loading="lazy"
          />
        ) : (
          <div style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '1.75rem' }}>📷</span>
            <span>No photo attached</span>
          </div>
        )}

        {/* Distance Pill if available */}
        {distanceText && (
          <span
            style={{
              position: 'absolute',
              top: '10px',
              right: '10px',
              backgroundColor: 'rgba(15, 23, 42, 0.75)',
              color: '#fff',
              fontSize: '0.75rem',
              fontWeight: 600,
              padding: '0.2rem 0.5rem',
              borderRadius: 'var(--radius-full)',
              backdropFilter: 'blur(4px)',
            }}
          >
            📍 {distanceText}
          </span>
        )}
      </div>

      {/* Badges Row */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '0.75rem' }}>
        <StatusBadge status={issue.status} />
        {issue.priority && <PriorityBadge level={issue.priority.level} score={issue.priority.score} />}
      </div>

      {/* Category Tag */}
      <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-primary)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '0.35rem' }}>
        {issue.category?.name || 'General Civic'}
      </div>

      {/* Title */}
      <h3 style={{ fontSize: '1.15rem', fontWeight: 700, lineHeight: 1.35, marginBottom: '0.5rem', color: 'var(--color-text)' }}>
        <Link
          href={`/issues/${issue.id}`}
          style={{ textDecoration: 'none', color: 'inherit' }}
        >
          {issue.title}
        </Link>
      </h3>

      {/* Description Excerpt */}
      <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', lineHeight: 1.5, marginBottom: '1rem', flex: 1, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
        {issue.description}
      </p>

      {/* Location Context */}
      <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
        <span>📍</span>
        <span>{wardOrCity}</span>
      </div>

      {/* Footer Metrics */}
      <div
        style={{
          borderTop: '1px solid var(--color-border)',
          paddingTop: '0.75rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          fontSize: '0.8rem',
          color: 'var(--color-text-secondary)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span>👍 {issue.supportCount} {issue.supportCount === 1 ? 'support' : 'supports'}</span>
          <span>💬 {issue.commentCount}</span>
        </div>
        <span>{relativeDate}</span>
      </div>
    </article>
  );
}
