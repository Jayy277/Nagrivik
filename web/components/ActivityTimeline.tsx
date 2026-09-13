import React from 'react';
import { ActivityResponse } from '../types/issue';
import { formatActivityEvent } from '../lib/formatters';

interface ActivityTimelineProps {
  activities: ActivityResponse[];
}

export function ActivityTimeline({ activities }: ActivityTimelineProps) {
  if (!activities || activities.length === 0) {
    return (
      <div style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem', fontStyle: 'italic', padding: '1rem 0' }}>
        No public activity recorded yet.
      </div>
    );
  }

  return (
    <div style={{ position: 'relative', paddingLeft: '1.75rem' }}>
      {/* Vertical Timeline Guide Line */}
      <div
        style={{
          position: 'absolute',
          top: '8px',
          bottom: '12px',
          left: '7px',
          width: '2px',
          backgroundColor: 'var(--color-border)',
        }}
      />

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {activities.map((act) => {
          const { title, subtitle } = formatActivityEvent(act);
          return (
            <div key={act.id} style={{ position: 'relative' }}>
              {/* Timeline Dot */}
              <div
                style={{
                  position: 'absolute',
                  left: '-1.75rem',
                  top: '4px',
                  width: '12px',
                  height: '12px',
                  borderRadius: '50%',
                  backgroundColor: 'var(--color-primary)',
                  border: '2px solid var(--color-surface)',
                  boxShadow: '0 0 0 2px var(--color-border)',
                }}
                aria-hidden="true"
              />

              <div>
                <h4 style={{ fontSize: '0.925rem', fontWeight: 600, color: 'var(--color-text)' }}>
                  {title}
                </h4>
                <p style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
                  {subtitle}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
