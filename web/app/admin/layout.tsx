'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '../../lib/context/AuthContext';

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { status, user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === 'UNAUTHENTICATED') {
      router.replace('/login?returnTo=' + encodeURIComponent(pathname));
    }
  }, [status, pathname, router]);

  if (status === 'LOADING') {
    return (
      <div style={{ minHeight: '80vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ width: '48px', height: '48px', border: '4px solid #e2e8f0', borderTopColor: '#1e40af', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px' }} />
          <p style={{ color: '#64748b', fontSize: '15px' }}>Verifying administrator credentials...</p>
        </div>
      </div>
    );
  }

  if (status === 'UNAUTHENTICATED') {
    return null;
  }

  const isPrivileged = user?.role === 'MODERATOR' || user?.role === 'ADMIN' || user?.role === 'OFFICER';

  if (!isPrivileged) {
    return (
      <main style={{ maxWidth: '640px', margin: '80px auto', padding: '32px', textAlign: 'center', background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)' }}>
        <div style={{ width: '64px', height: '64px', background: '#fee2e2', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 20px', color: '#dc2626' }}>
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
            <path d="M7 11V7a5 5 0 0 1 10 0v4" />
          </svg>
        </div>
        <h1 style={{ fontSize: '24px', fontWeight: '700', color: '#0f172a', marginBottom: '12px' }}>Access Restricted</h1>
        <p style={{ fontSize: '15px', color: '#475569', lineHeight: '1.6', marginBottom: '24px' }}>
          You are authenticated as <strong>{user?.fullName || 'Citizen'}</strong> with standard citizen permissions. The moderation dashboard requires authorized <strong>MODERATOR</strong> or <strong>ADMIN</strong> credentials.
        </p>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <Link
            href="/"
            style={{ padding: '10px 20px', background: '#1e40af', color: '#ffffff', borderRadius: '8px', textDecoration: 'none', fontWeight: '600', fontSize: '14px' }}
          >
            Return to Public Portal
          </Link>
          <button
            onClick={() => logout()}
            style={{ padding: '10px 20px', background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', borderRadius: '8px', cursor: 'pointer', fontWeight: '600', fontSize: '14px' }}
          >
            Sign Out
          </button>
        </div>
      </main>
    );
  }

  const navItems = [
    { label: 'Dashboard', href: '/admin' },
    { label: 'Moderation Queue', href: '/admin/moderation' },
    { label: 'Duplicate Review', href: '/admin/duplicates' },
    ...(user?.role === 'ADMIN' ? [{ label: 'Civic Geography', href: '/admin/geography' }] : []),
  ];

  return (
    <div style={{ minHeight: '100vh', background: '#f8fafc' }}>
      {/* Top Admin Header */}
      <header style={{ background: '#0f172a', color: '#ffffff', borderBottom: '1px solid #1e293b' }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '0 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: '64px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '32px' }}>
            <Link href="/admin" style={{ display: 'flex', alignItems: 'center', gap: '10px', textDecoration: 'none', color: '#ffffff' }}>
              <div style={{ width: '32px', height: '32px', background: '#1e40af', borderRadius: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: '800', fontSize: '16px' }}>
                N
              </div>
              <span style={{ fontWeight: '700', fontSize: '18px', letterSpacing: '-0.02em' }}>Nagrivic Admin</span>
            </Link>

            <nav aria-label="Admin Navigation" style={{ display: 'flex', gap: '8px' }}>
              {navItems.map((item) => {
                const isActive = pathname === item.href || (item.href !== '/admin' && pathname?.startsWith(item.href));
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    style={{
                      padding: '8px 16px',
                      borderRadius: '6px',
                      fontSize: '14px',
                      fontWeight: '600',
                      textDecoration: 'none',
                      color: isActive ? '#ffffff' : '#94a3b8',
                      background: isActive ? '#1e293b' : 'transparent',
                    }}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <span
              style={{
                fontSize: '12px',
                fontWeight: '700',
                padding: '4px 10px',
                borderRadius: '9999px',
                background: user?.role === 'ADMIN' ? '#7c2d12' : '#1e3a8a',
                color: user?.role === 'ADMIN' ? '#ffedd5' : '#dbeafe',
                letterSpacing: '0.05em',
              }}
            >
              {user?.role}
            </span>
            <span style={{ fontSize: '14px', color: '#cbd5e1' }}>{user?.fullName || 'Moderator'}</span>
            <Link
              href="/"
              style={{ fontSize: '13px', color: '#94a3b8', textDecoration: 'none', padding: '6px 12px', borderRadius: '6px', border: '1px solid #334155' }}
            >
              Public App →
            </Link>
            <button
              onClick={() => logout()}
              style={{ background: 'transparent', border: 'none', color: '#ef4444', fontSize: '13px', cursor: 'pointer', fontWeight: '600' }}
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      {/* Main Admin Content */}
      <main style={{ maxWidth: '1280px', margin: '0 auto', padding: '32px 24px' }}>
        {children}
      </main>
    </div>
  );
}
