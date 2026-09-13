import React from 'react';
import Link from 'next/link';

export function LoadingSpinner({ message = 'Loading civic data...' }: { message?: string }) {
  return (
    <div
      role="status"
      aria-live="polite"
      style={{
        padding: '3.5rem 1rem',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '1rem',
        color: 'var(--color-text-secondary)',
      }}
    >
      <div
        style={{
          width: '36px',
          height: '36px',
          border: '3px solid var(--color-border)',
          borderTopColor: 'var(--color-primary)',
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
        }}
      />
      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
      <p style={{ fontSize: '0.95rem' }}>{message}</p>
    </div>
  );
}

export function ErrorState({
  title = 'Something went wrong',
  message = 'Unable to load civic information. Please verify your connection.',
  onRetry,
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div
      role="alert"
      style={{
        padding: '3rem 1.5rem',
        backgroundColor: '#fef2f2',
        border: '1px solid #fecaca',
        borderRadius: 'var(--radius-lg)',
        textAlign: 'center',
        maxWidth: '540px',
        margin: '2rem auto',
      }}
    >
      <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>⚠️</div>
      <h3 style={{ fontSize: '1.25rem', color: '#991b1b', marginBottom: '0.5rem', fontWeight: 700 }}>
        {title}
      </h3>
      <p style={{ fontSize: '0.95rem', color: '#7f1d1d', marginBottom: '1.25rem', lineHeight: 1.5 }}>
        {message}
      </p>
      {onRetry && (
        <button onClick={onRetry} className="btn btn-primary btn-sm">
          Try Again
        </button>
      )}
    </div>
  );
}

export function EmptyState({
  title = 'No issues found',
  description = 'There are no reports matching your query or selected filters.',
  actionLabel,
  actionHref,
  onAction,
}: {
  title?: string;
  description?: string;
  actionLabel?: string;
  actionHref?: string;
  onAction?: () => void;
}) {
  return (
    <div
      style={{
        padding: '4rem 1.5rem',
        backgroundColor: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-lg)',
        textAlign: 'center',
        maxWidth: '540px',
        margin: '2rem auto',
      }}
    >
      <div style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>🔍</div>
      <h3 style={{ fontSize: '1.35rem', color: 'var(--color-text)', marginBottom: '0.5rem', fontWeight: 700 }}>
        {title}
      </h3>
      <p style={{ fontSize: '0.95rem', color: 'var(--color-text-secondary)', marginBottom: '1.5rem', lineHeight: 1.6 }}>
        {description}
      </p>

      {actionHref && (
        <Link href={actionHref} className="btn btn-secondary btn-sm">
          {actionLabel || 'Reset Filters'}
        </Link>
      )}

      {!actionHref && onAction && (
        <button onClick={onAction} className="btn btn-secondary btn-sm">
          {actionLabel || 'Reset Filters'}
        </button>
      )}
    </div>
  );
}
