'use client';

import React, { Suspense, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { GoogleSignInButton } from '../../components/GoogleSignInButton';
import { useAuth } from '../../lib/context/AuthContext';

function LoginContent() {
  const searchParams = useSearchParams();
  const returnUrl = searchParams.get('returnUrl') || '/';
  const errorParam = searchParams.get('error');

  const { status, user, logout } = useAuth();
  const [loading, setLoading] = useState(false);

  const handleGoogleLogin = () => {
    setLoading(true);
    // Redirect through secure server-side OAuth flow
    window.location.href = `/api/auth/google/login?returnUrl=${encodeURIComponent(returnUrl)}`;
  };

  const getErrorMessage = (code: string | null) => {
    if (!code) return null;
    switch (code) {
      case 'session_expired':
      case 'invalid_state':
        return 'Your sign-in attempt timed out. Please try again.';
      case 'access_denied':
        return 'Google sign-in was cancelled.';
      case 'auth_failed':
      case 'token_exchange_failed':
        return 'Unable to sign in with Google. Please try again.';
      default:
        return 'An error occurred during sign-in. Please try again.';
    }
  };

  const errorMessage = getErrorMessage(errorParam);

  return (
    <div className="container" style={{ padding: '3rem 1.25rem 6rem', maxWidth: '540px' }}>
      <div
        className="card"
        style={{
          padding: '2.5rem 2rem',
          textAlign: 'center',
          boxShadow: 'var(--shadow-md)',
          borderRadius: 'var(--radius-xl)',
        }}
      >
        {/* Brand Badge */}
        <div
          style={{
            width: '64px',
            height: '64px',
            backgroundColor: 'var(--color-primary)',
            color: '#FFFFFF',
            borderRadius: '16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '1.75rem',
            fontWeight: 800,
            margin: '0 auto 1.25rem',
            boxShadow: 'var(--shadow-sm)',
          }}
          aria-hidden="true"
        >
          N
        </div>

        <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--color-text)', marginBottom: '0.5rem' }}>
          Nagrivic
        </h1>

        <p style={{ fontSize: '1.05rem', color: 'var(--color-primary-dark)', fontWeight: 600, marginBottom: '0.75rem' }}>
          Report civic issues. Support your community.
        </p>

        <p style={{ fontSize: '0.925rem', color: 'var(--color-text-secondary)', lineHeight: 1.6, marginBottom: '2rem' }}>
          Sign in with Google to support issues in your area, participate in community discussions, and track municipal resolution.
        </p>

        {errorMessage && (
          <div
            style={{
              padding: '0.75rem 1rem',
              backgroundColor: 'var(--color-status-reported-bg)',
              border: '1px solid var(--color-status-reported-border)',
              borderRadius: 'var(--radius-md)',
              color: 'var(--color-error, #DC2626)',
              fontSize: '0.875rem',
              marginBottom: '1.5rem',
              textAlign: 'left',
            }}
          >
            ⚠️ {errorMessage}
          </div>
        )}

        {status === 'AUTHENTICATED' && user ? (
          <div style={{ marginBottom: '1.5rem' }}>
            <p style={{ fontSize: '0.95rem', color: 'var(--color-text)', marginBottom: '1rem' }}>
              You are signed in as <strong>{user.fullName || user.email || 'Citizen'}</strong>
            </p>
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
              <Link href={returnUrl} className="btn btn-primary">
                Continue to App
              </Link>
              <button onClick={() => logout()} className="btn btn-outline">
                Sign Out
              </button>
            </div>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <GoogleSignInButton
              onClick={handleGoogleLogin}
              loading={loading}
              disabled={loading}
              style={{ width: '100%' }}
            />

            <p style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', marginTop: '0.5rem' }}>
              By continuing, you agree to Nagrivic&apos;s Terms and Privacy Policy.
            </p>
          </div>
        )}
      </div>

      <div style={{ textAlign: 'center', marginTop: '1.5rem' }}>
        <Link href="/" style={{ fontSize: '0.9rem', color: 'var(--color-primary)', fontWeight: 500 }}>
          ← Back to Public Issues Feed
        </Link>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="container" style={{ padding: '4rem', textAlign: 'center' }}>Loading...</div>}>
      <LoginContent />
    </Suspense>
  );
}
