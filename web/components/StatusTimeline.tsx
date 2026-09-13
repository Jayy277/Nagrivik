import React from 'react';
import { IssueStatus, StatusHistoryResponse } from '../types/issue';
import { formatStatusLabel, formatRelativeTime } from '../lib/formatters';

interface StatusTimelineProps {
  currentStatus: IssueStatus;
  history?: StatusHistoryResponse | null;
}

const ORDERED_STEPS: IssueStatus[] = [
  'REPORTED',
  'VERIFIED',
  'ACKNOWLEDGED',
  'IN_PROGRESS',
  'RESOLVED',
  'CITIZEN_VERIFIED',
];

export function StatusTimeline({ currentStatus, history }: StatusTimelineProps) {
  const currentStepIndex = ORDERED_STEPS.indexOf(
    currentStatus === 'NOT_FIXED' ? 'RESOLVED' : currentStatus
  );

  return (
    <div style={{ width: '100%' }}>
      {/* Visual Stepper */}
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          position: 'relative',
          overflowX: 'auto',
          padding: '1rem 0.5rem',
          marginBottom: '1.5rem',
        }}
      >
        {ORDERED_STEPS.map((step, idx) => {
          const isPassed = currentStepIndex >= idx;
          const isCurrent = currentStatus === step;
          const label = formatStatusLabel(step);

          return (
            <div
              key={step}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                flex: 1,
                minWidth: '90px',
                position: 'relative',
                zIndex: 1,
              }}
            >
              {/* Connecting Line */}
              {idx < ORDERED_STEPS.length - 1 && (
                <div
                  style={{
                    position: 'absolute',
                    top: '14px',
                    left: '50%',
                    width: '100%',
                    height: '3px',
                    backgroundColor: currentStepIndex > idx ? 'var(--color-primary)' : 'var(--color-border)',
                    zIndex: -1,
                  }}
                />
              )}

              {/* Step Circle */}
              <div
                style={{
                  width: '28px',
                  height: '28px',
                  borderRadius: '50%',
                  backgroundColor: isCurrent
                    ? 'var(--color-primary)'
                    : isPassed
                    ? 'var(--color-secondary)'
                    : 'var(--color-surface)',
                  border: `2px solid ${isCurrent || isPassed ? 'transparent' : 'var(--color-border)'}`,
                  color: isCurrent || isPassed ? '#fff' : 'var(--color-text-muted)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  marginBottom: '0.5rem',
                  boxShadow: isCurrent ? '0 0 0 4px var(--color-primary-light)' : 'none',
                }}
                aria-hidden="true"
              >
                {isPassed && !isCurrent ? '✓' : idx + 1}
              </div>

              {/* Step Label */}
              <span
                style={{
                  fontSize: '0.75rem',
                  fontWeight: isCurrent ? 700 : 500,
                  color: isCurrent
                    ? 'var(--color-primary)'
                    : isPassed
                    ? 'var(--color-text)'
                    : 'var(--color-text-muted)',
                  textAlign: 'center',
                }}
              >
                {label}
              </span>
            </div>
          );
        })}
      </div>

      {/* Special Not Fixed Notice */}
      {currentStatus === 'NOT_FIXED' && (
        <div
          style={{
            padding: '0.75rem 1rem',
            backgroundColor: 'var(--color-status-not-fixed-bg)',
            color: 'var(--color-status-not-fixed-text)',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.85rem',
            marginBottom: '1rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <span>⚠️</span>
          <span>
            <strong>Not Fixed:</strong> The citizen verification indicated the problem was not adequately resolved. The issue has been routed back for municipal rework.
          </span>
        </div>
      )}

      {/* Official History Log if available */}
      {history && history.history && history.history.length > 0 && (
        <div style={{ marginTop: '1.5rem', borderTop: '1px solid var(--color-border)', paddingTop: '1rem' }}>
          <h4 style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', marginBottom: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
            Official Progression Log
          </h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {history.history.map((h, i) => (
              <div
                key={h.id || i}
                style={{
                  fontSize: '0.85rem',
                  backgroundColor: 'var(--color-surface-muted)',
                  padding: '0.75rem',
                  borderRadius: 'var(--radius-md)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'flex-start',
                }}
              >
                <div>
                  <div style={{ fontWeight: 600, color: 'var(--color-text)', marginBottom: '2px' }}>
                    Status updated to {formatStatusLabel(h.toStatus)}
                  </div>
                  {h.reason && (
                    <div style={{ color: 'var(--color-text-secondary)', fontStyle: 'italic', fontSize: '0.8rem' }}>
                      “{h.reason}”
                    </div>
                  )}
                  <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '4px' }}>
                    By {h.changedBy?.displayName || 'Municipal Authority'}
                  </div>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', whiteSpace: 'nowrap', marginLeft: '1rem' }}>
                  {formatRelativeTime(h.createdAt)}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
