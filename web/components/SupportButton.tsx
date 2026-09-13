'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../lib/context/AuthContext';

interface SupportButtonProps {
  issueId: string;
  initialSupported?: boolean;
  initialCount: number;
}

export function SupportButton({
  issueId,
  initialSupported = false,
  initialCount = 0,
}: SupportButtonProps) {
  const router = useRouter();
  const { status, user } = useAuth();
  const [supported, setSupported] = useState(initialSupported);
  const [count, setCount] = useState(initialCount);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const isAuthenticated = status === 'AUTHENTICATED' && user;

  const handleToggle = async () => {
    if (loading) return;

    if (!isAuthenticated) {
      // Direct unauthenticated users to Google login and return
      router.push(`/login?returnUrl=${encodeURIComponent(`/issues/${issueId}`)}&action=support`);
      return;
    }

    const prevSupported = supported;
    const prevCount = count;
    const nextSupported = !prevSupported;
    const nextCount = nextSupported ? prevCount + 1 : Math.max(0, prevCount - 1);

    // Optimistic update
    setSupported(nextSupported);
    setCount(nextCount);
    setLoading(true);
    setErrorMsg(null);

    try {
      const res = await fetch(`/api/web/issues/${issueId}/support`, {
        method: nextSupported ? 'POST' : 'DELETE',
      });

      if (!res.ok) {
        // Rollback
        setSupported(prevSupported);
        setCount(prevCount);
        if (res.status === 401) {
          router.push(`/login?returnUrl=${encodeURIComponent(`/issues/${issueId}`)}`);
        } else {
          setErrorMsg('Unable to update support. Please try again.');
        }
      }
    } catch {
      setSupported(prevSupported);
      setCount(prevCount);
      setErrorMsg('Connection error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'flex-start' }}>
      <button
        type="button"
        onClick={handleToggle}
        disabled={loading}
        className={supported ? 'btn btn-primary btn-sm' : 'btn btn-outline btn-sm'}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          fontWeight: 600,
          cursor: loading ? 'wait' : 'pointer',
        }}
        aria-label={supported ? 'Remove support for this issue' : 'Support this civic issue'}
      >
        <span>{supported ? '✓ Supported' : '👍 Support Issue'}</span>
        <span
          style={{
            backgroundColor: supported ? 'rgba(255,255,255,0.25)' : 'var(--color-surface-muted, #f1f5f9)',
            padding: '2px 7px',
            borderRadius: '12px',
            fontSize: '0.8rem',
            marginLeft: '4px',
          }}
        >
          {count}
        </span>
      </button>

      {errorMsg && (
        <span style={{ fontSize: '0.75rem', color: 'var(--color-error, #DC2626)', marginTop: '4px' }}>
          {errorMsg}
        </span>
      )}
    </div>
  );
}
