'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '../../lib/context/AuthContext';

export default function AuthorityLayout({
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
          <div style={{ width: '48px', height: '48px', border: '4px solid #e2e8f0', borderTopColor: '#0284c7', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px' }} />
          <p style={{ color: '#64748b', fontSize: '15px' }}>Verifying authority credentials...</p>
        </div>
      </div>
    );
  }

  if (status === 'UNAUTHENTICATED') {
    return null;
  }

  const isAuthority = user?.role === 'OFFICER' || user?.role === 'ADMIN';

  if (!isAuthority) {
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
          You are authenticated as <strong>{user?.fullName || 'Citizen'}</strong> with role <strong>{user?.role || 'CITIZEN'}</strong>. The Authority Operations Portal requires authorized <strong>OFFICER</strong> or <strong>ADMIN</strong> credentials.
        </p>
        <div style={{ display: 'flex', justifyContent: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <Link
            href="/"
            style={{ padding: '10px 20px', background: '#0284c7', color: '#ffffff', borderRadius: '8px', textDecoration: 'none', fontWeight: '600', fontSize: '14px' }}
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
    { label: 'Overview', href: '/authority' },
    { label: 'Scoped Issues', href: '/authority/issues' },
  ];

  return (
    <div style={{ minHeight: '100vh', background: '#f8fafc' }}>
      {/* Authority Operations Header */}
      <header style={{ background: '#0c4a6e', color: '#ffffff', borderBottom: '1px solid #0369a1' }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '0 24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: '64px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
              <Link href="/authority" style={{ display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none', color: '#ffffff' }}>
                <span style={{ fontSize: '20px' }}>🏛️</span>
                <span style={{ fontWeight: '700', fontSize: '18px', letterSpacing: '-0.025em' }}>Nagrivic Authority</span>
                <span style={{ background: '#0369a1', color: '#e0f2fe', fontSize: '11px', fontWeight: '600', padding: '2px 8px', borderRadius: '4px', textTransform: 'uppercase' }}>
                  {user?.role === 'ADMIN' ? 'Admin Scope' : 'Authority Scope'}
                </span>
              </Link>
              <nav style={{ display: 'flex', gap: '4px' }}>
                {navItems.map((item) => {
                  const isActive = pathname === item.href || (item.href !== '/authority' && pathname.startsWith(item.href));
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      style={{
                        padding: '8px 14px',
                        borderRadius: '6px',
                        fontSize: '14px',
                        fontWeight: '500',
                        textDecoration: 'none',
                        color: isActive ? '#ffffff' : '#bae6fd',
                        background: isActive ? '#0369a1' : 'transparent',
                        transition: 'background 0.15s ease',
                      }}
                    >
                      {item.label}
                    </Link>
                  );
                })}
              </nav>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '14px', fontWeight: '600', color: '#f0f9ff' }}>{user?.fullName || 'Authority Officer'}</div>
                <div style={{ fontSize: '12px', color: '#7dd3fc' }}>{user?.role}</div>
              </div>
              <Link
                href="/"
                style={{
                  fontSize: '13px',
                  color: '#bae6fd',
                  textDecoration: 'none',
                  padding: '6px 12px',
                  borderRadius: '6px',
                  border: '1px solid #0369a1',
                }}
              >
                Exit to Citizen Portal
              </Link>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '24px' }}>
        {children}
      </div>
    </div>
  );
}
