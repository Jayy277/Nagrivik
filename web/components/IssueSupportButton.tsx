'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../lib/context/AuthContext';

interface IssueSupportButtonProps {
  issueId: string;
  initialSupportCount: number;
}

export function IssueSupportButton({
  issueId,
  initialSupportCount,
}: IssueSupportButtonProps) {
  const router = useRouter();
  const { status } = useAuth();
  const [supportCount, setSupportCount] = useState(initialSupportCount);
  const [hasSupported, setHasSupported] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isAuthenticated = status === 'AUTHENTICATED';

  const handleSupportClick = async () => {
    if (!isAuthenticated) {
      router.push(`/login?returnTo=/issues/${issueId}`);
      return;
    }

    if (isSubmitting) return;
    setIsSubmitting(true);

    const nextSupported = !hasSupported;
    setHasSupported(nextSupported);
    setSupportCount((prev) => (nextSupported ? prev + 1 : Math.max(0, prev - 1)));

    try {
      const res = await fetch(`/api/web/issues/${issueId}/support`, {
        method: nextSupported ? 'POST' : 'DELETE',
      });

      if (!res.ok) {
        // Rollback on failure
        setHasSupported(!nextSupported);
        setSupportCount((prev) => (!nextSupported ? prev + 1 : Math.max(0, prev - 1)));
      }
    } catch {
      // Rollback on network failure
      setHasSupported(!nextSupported);
      setSupportCount((prev) => (!nextSupported ? prev + 1 : Math.max(0, prev - 1)));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'flex-start', gap: '4px' }}>
      <button
        onClick={handleSupportClick}
        disabled={isSubmitting}
        id="issue-support-button"
        className={`btn ${hasSupported ? 'btn-outline' : 'btn-primary'} btn-sm`}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          fontWeight: 600,
        }}
      >
        <span>👍</span>
        <span>{hasSupported ? 'Supported' : 'Support Report'}</span>
        <span
          style={{
            backgroundColor: hasSupported ? 'var(--color-primary-light)' : 'rgba(255, 255, 255, 0.25)',
            color: hasSupported ? 'var(--color-primary-dark)' : '#ffffff',
            padding: '1px 6px',
            borderRadius: 'var(--radius-full)',
            fontSize: '0.75rem',
            marginLeft: '2px',
          }}
        >
          {supportCount}
        </span>
      </button>

      {!isAuthenticated && (
        <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
          Continue with Google to support
        </span>
      )}
    </div>
  );
}
