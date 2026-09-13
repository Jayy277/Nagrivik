import React from 'react';
import { PriorityLevel } from '../types/issue';
import { formatPriorityLabel } from '../lib/formatters';

interface PriorityBadgeProps {
  level: PriorityLevel;
  score?: number;
}

export function PriorityBadge({ level, score }: PriorityBadgeProps) {
  const label = formatPriorityLabel(level);
  const badgeClass = `badge badge-priority-${level.toLowerCase()}`;

  return (
    <span
      className={badgeClass}
      aria-label={`Priority: ${label}${score !== undefined ? `, score ${score}` : ''}`}
    >
      {label}
      {score !== undefined && score > 0 ? ` (${score})` : ''}
    </span>
  );
}
