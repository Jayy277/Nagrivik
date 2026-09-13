'use client';

import React from 'react';
import Link from 'next/link';
import { useAuth } from '../../lib/context/AuthContext';

export default function ProfilePage() {
  const { user, status, isLoading, logout } = useAuth();

  if (isLoading) {
    return (
      <div className="container" style={{ padding: '4rem 1.25rem', textAlign: 'center' }}>
        <p style={{ color: 'var(--color-text-secondary)' }}>Loading citizen profile...</p>
      </div>
    );
  }

  if (status !== 'AUTHENTICATED' || !user) {
    return (
      <div className="container" style={{ padding: '4rem 1.25rem', textAlign: 'center' }}>
        <div className="card" style={{ maxWidth: '440px', margin: '0 auto', padding: '2rem' }}>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '1rem' }}>
            Sign In Required
          </h1>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: '1.5rem' }}>
            Please sign in with your Google account to view your citizen profile.
          </p>
          <Link href="/login?returnTo=/profile" className="btn btn-primary" style={{ width: '100%' }}>
            Continue with Google
          </Link>
        </div>
      </div>
    );
  }

  const displayName = user.fullName || 'Citizen';
  const displayEmail = user.email || 'No email provided';
  const roleDisplay = user.role === 'CITIZEN' ? 'Citizen' : user.role;

  return (
    <div className="container" style={{ padding: '3rem 1.25rem 5rem' }}>
      <div style={{ maxWidth: '640px', margin: '0 auto' }}>
        <div className="card" style={{ padding: '2.5rem', textAlign: 'center', marginBottom: '2rem' }}>
          {/* Avatar / Profile Picture */}
          <div
            style={{
              width: '88px',
              height: '88px',
              borderRadius: '50%',
              margin: '0 auto 1.25rem',
              overflow: 'hidden',
              backgroundColor: 'var(--color-primary-light)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '2.25rem',
              color: 'var(--color-primary)',
              border: '3px solid #ffffff',
              boxShadow: 'var(--shadow-md)',
            }}
          >
            {user.profilePictureUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={user.profilePictureUrl}
                alt={displayName}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => {
                  (e.target as HTMLElement).style.display = 'none';
                }}
              />
            ) : (
              <span>👤</span>
            )}
          </div>

          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--color-text)', marginBottom: '0.25rem' }}>
            {displayName}
          </h1>

          <p style={{ fontSize: '1rem', color: 'var(--color-text-secondary)', marginBottom: '1rem' }}>
            {displayEmail}
          </p>

          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              backgroundColor: 'var(--color-primary-light)',
              color: 'var(--color-primary-dark)',
              padding: '6px 14px',
              borderRadius: 'var(--radius-full)',
              fontSize: '0.85rem',
              fontWeight: 600,
              marginBottom: '2rem',
            }}
          >
            <span>🛡️</span>
            <span>Verified {roleDisplay}</span>
          </div>

          {/* Account Details Box */}
          <div
            style={{
              backgroundColor: 'var(--color-surface-muted)',
              border: '1px solid var(--color-border-light)',
              borderRadius: 'var(--radius-lg)',
              padding: '1.25rem',
              textAlign: 'left',
              marginBottom: '2rem',
              display: 'flex',
              flexDirection: 'column',
              gap: '0.75rem',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
              <span style={{ color: 'var(--color-text-secondary)' }}>Account Type:</span>
              <strong style={{ color: 'var(--color-text)' }}>Citizen Account</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
              <span style={{ color: 'var(--color-text-secondary)' }}>Authentication:</span>
              <strong style={{ color: 'var(--color-text)' }}>Google Verified</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem' }}>
              <span style={{ color: 'var(--color-text-secondary)' }}>Status:</span>
              <strong style={{ color: 'var(--color-success)' }}>Active</strong>
            </div>
          </div>

          {/* Log Out Action */}
          <button
            onClick={logout}
            className="btn btn-outline"
            style={{
              borderColor: 'var(--color-danger)',
              color: 'var(--color-danger)',
              width: '100%',
              padding: '12px',
            }}
          >
            Log out
          </button>
        </div>
      </div>
    </div>
  );
}
