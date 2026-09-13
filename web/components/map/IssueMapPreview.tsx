import React from 'react';
import Link from 'next/link';
import { IssueResponse } from '../../types/issue';
import { StatusBadge } from '../StatusBadge';
import { PriorityBadge } from '../PriorityBadge';
import { getIssueImageUrl } from '../../lib/api/issues';
import { formatRelativeTime, formatDistance } from '../../lib/formatters';

interface IssueMapPreviewProps {
  issue: IssueResponse;
  onClose: () => void;
}

export function IssueMapPreview({ issue, onClose }: IssueMapPreviewProps) {
  const mediaId = issue.media && issue.media.length > 0 ? issue.media[0].id : undefined;
  const imageUrl = getIssueImageUrl(issue.id, mediaId);

  return (
    <article
      className="map-preview-card"
      aria-label={`Selected civic issue preview: ${issue.title}`}
    >
      <div className="map-preview-header">
        <div className="map-preview-badges">
          {issue.category && (
            <span className="civic-category-pill">{issue.category.name}</span>
          )}
          <StatusBadge status={issue.status} />
          {issue.priority && <PriorityBadge level={issue.priority.level} />}
        </div>
        <button
          type="button"
          className="map-preview-close"
          onClick={onClose}
          aria-label="Close preview"
        >
          &times;
        </button>
      </div>

      <div className="map-preview-body">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={issue.title}
            className="map-preview-thumbnail"
            loading="lazy"
            onError={(e) => {
              e.currentTarget.style.display = 'none';
            }}
          />
        ) : (
          <div className="map-preview-placeholder" aria-hidden="true">
            <span>📍</span>
          </div>
        )}

        <div className="map-preview-details">
          <h3 className="map-preview-title">{issue.title}</h3>
          <div className="map-preview-meta">
            {issue.distanceMeters !== undefined && issue.distanceMeters !== null && (
              <span className="map-preview-distance">
                📍 {formatDistance(issue.distanceMeters)}
              </span>
            )}
            <span className="map-preview-supports">
              👍 {issue.supportCount || 0} supports
            </span>
            <span className="map-preview-time">
              ⏱️ {formatRelativeTime(issue.createdAt)}
            </span>
          </div>
        </div>
      </div>

      <div className="map-preview-actions">
        <Link
          href={`/issues/${issue.id}`}
          className="civic-button civic-button-primary map-preview-button"
        >
          View Full Issue
        </Link>
      </div>
    </article>
  );
}
