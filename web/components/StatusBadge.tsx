import React from 'react';
import { IssueStatus } from '../types/issue';
import { formatStatusLabel } from '../lib/formatters';

interface StatusBadgeProps {
  status: IssueStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const label = formatStatusLabel(status);
  const badgeClass = `badge badge-${status.toLowerCase()}`;

  return (
    <span
      className={badgeClass}
      role="status"
      aria-label={`Status: ${label}`}
    >
      <span
        aria-hidden="true"
        style={{
          width: '6px',
          height: '6px',
          borderRadius: '50%',
          backgroundColor: 'currentColor',
          display: 'inline-block',
        }}
      />
      {label}
    </span>
  );
}
