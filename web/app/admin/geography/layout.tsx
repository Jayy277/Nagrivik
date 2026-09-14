'use client';

import React from 'react';
import Link from 'next/link';
import { useAuth } from '../../../lib/context/AuthContext';

export default function AdminGeographyLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user } = useAuth();

  if (user && user.role !== 'ADMIN') {
    return (
      <main
        style={{
          maxWidth: '640px',
          margin: '80px auto',
          padding: '32px',
          textAlign: 'center',
          background: '#ffffff',
          borderRadius: '12px',
          border: '1px solid #e2e8f0',
          boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)',
        }}
      >
        <div
          style={{
            width: '64px',
            height: '64px',
            background: '#fee2e2',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px',
            color: '#dc2626',
          }}
        >
          <svg
            width="32"
            height="32"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>
        <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a', marginBottom: '12px' }}>
          Admin Role Required
        </h1>
        <p style={{ fontSize: '15px', color: '#475569', lineHeight: '1.6', marginBottom: '24px' }}>
          Civic geography, ward delimitation, and departmental responsibility configuration is strictly restricted to platform <strong>ADMIN</strong> users. Moderators have authority over the moderation queue only.
        </p>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <Link
            href="/admin/moderation"
            style={{
              padding: '10px 20px',
              background: '#1e40af',
              color: '#ffffff',
              borderRadius: '8px',
              textDecoration: 'none',
              fontWeight: '600',
              fontSize: '14px',
            }}
          >
            Go to Moderation Queue
          </Link>
          <Link
            href="/admin"
            style={{
              padding: '10px 20px',
              background: '#f1f5f9',
              color: '#475569',
              border: '1px solid #cbd5e1',
              borderRadius: '8px',
              textDecoration: 'none',
              fontWeight: '600',
              fontSize: '14px',
            }}
          >
            Admin Dashboard
          </Link>
        </div>
      </main>
    );
  }

  return <>{children}</>;
}
