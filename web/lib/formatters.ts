import { IssueStatus, PriorityLevel, ActivityResponse } from '../types/issue';

export function formatRelativeTime(isoDate?: string | null): string {
  if (!isoDate) return '';
  const date = new Date(isoDate);
  if (isNaN(date.getTime())) return '';

  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.max(0, Math.floor(diffMs / 1000));
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (diffSec < 60) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  if (diffHour < 24) return `${diffHour}h ago`;
  if (diffDay < 7) return `${diffDay}d ago`;

  return date.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
  });
}

export function formatFullDate(isoDate?: string | null): string {
  if (!isoDate) return '';
  const date = new Date(isoDate);
  if (isNaN(date.getTime())) return '';

  return date.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDistance(distanceMeters?: number | null): string | null {
  if (distanceMeters === undefined || distanceMeters === null) return null;
  if (distanceMeters < 1000) {
    return `${Math.round(distanceMeters)}m away`;
  }
  return `${(distanceMeters / 1000).toFixed(1)} km away`;
}

export function formatStatusLabel(status: IssueStatus): string {
  const map: Record<IssueStatus, string> = {
    REPORTED: 'Reported',
    VERIFIED: 'Verified',
    ACKNOWLEDGED: 'Acknowledged',
    IN_PROGRESS: 'In Progress',
    RESOLVED: 'Resolved',
    CITIZEN_VERIFIED: 'Citizen Verified',
    NOT_FIXED: 'Not Fixed',
  };
  return map[status] || status;
}

export function formatPriorityLabel(priority: PriorityLevel): string {
  const map: Record<PriorityLevel, string> = {
    LOW: 'Low Priority',
    MEDIUM: 'Medium Priority',
    HIGH: 'High Priority',
    CRITICAL: 'Critical Priority',
  };
  return map[priority] || priority;
}

export function formatActivityEvent(activity: ActivityResponse): {
  title: string;
  subtitle: string;
} {
  const actorName = activity.actor?.displayName || 'Citizen';
  const time = formatRelativeTime(activity.createdAt);

  switch (activity.eventType) {
    case 'ISSUE_REPORTED':
      return {
        title: 'Civic issue reported',
        subtitle: `Submitted by ${actorName} • ${time}`,
      };
    case 'STATUS_CHANGED': {
      const toStatus =
        (activity.data?.toStatus as string) ||
        (activity.data?.newStatus as string) ||
        'In Progress';
      return {
        title: `Status changed to ${toStatus.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())}`,
        subtitle: `Updated by ${actorName} • ${time}`,
      };
    }
    case 'COMMENT_ADDED':
      return {
        title: 'Comment added',
        subtitle: `Added by ${actorName} • ${time}`,
      };
    case 'COMMENT_DELETED':
      return {
        title: 'Comment removed',
        subtitle: `Removed • ${time}`,
      };
    case 'SUPPORT_ADDED':
      return {
        title: 'Issue supported',
        subtitle: `A citizen backed this report • ${time}`,
      };
    case 'SUPPORT_REMOVED':
      return {
        title: 'Support withdrawn',
        subtitle: `A citizen withdrew backing • ${time}`,
      };
    case 'DUPLICATE_LINKED':
      return {
        title: 'Linked as duplicate report',
        subtitle: `Connected to primary issue • ${time}`,
      };
    case 'RESPONSIBILITY_RESOLVED':
      return {
        title: 'Civic authority assigned',
        subtitle: `Responsible department allocated • ${time}`,
      };
    case 'RESPONSIBILITY_RE_RESOLVED':
      return {
        title: 'Civic responsibility updated',
        subtitle: `Reallocated by municipal team • ${time}`,
      };
    case 'PRIORITY_RECALCULATED':
      return {
        title: 'Civic priority updated',
        subtitle: `Recalculated based on community impact • ${time}`,
      };
    case 'MEDIA_ADDED':
      return {
        title: 'Photo evidence attached',
        subtitle: `Uploaded by ${actorName} • ${time}`,
      };
    default:
      return {
        title: 'Municipal activity',
        subtitle: time,
      };
  }
}
