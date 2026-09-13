'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '../lib/context/AuthContext';

export function Header() {
  const pathname = usePathname();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const { user, status, logout } = useAuth();

  const isActive = (path: string) => pathname === path;
  const isAuthenticated = status === 'AUTHENTICATED' && user;

  return (
    <header className="site-header" role="banner">
      <div className="container header-inner">
        <Link href="/" className="brand-link" aria-label="Nagrivic Home">
          <div className="brand-logo-badge" aria-hidden="true">
            N
          </div>
          <div>
            <span className="brand-title">Nagrivic</span>
            <span className="brand-subtitle">Civic Transparency Platform</span>
          </div>
        </Link>

        {/* Desktop Navigation */}
        <nav className="nav-links" aria-label="Main Navigation">
          <Link
            href="/"
            className={`nav-link ${isActive('/') ? 'active' : ''}`}
          >
            Home
          </Link>
          <Link
            href="/issues"
            className={`nav-link ${isActive('/issues') ? 'active' : ''}`}
          >
            Explore Issues
          </Link>
          <Link
            href="/map"
            className={`nav-link ${isActive('/map') ? 'active' : ''}`}
          >
            Civic Map
          </Link>
          <Link
            href="/#how-it-works"
            className="nav-link"
          >
            How it Works
          </Link>

          {/* Auth State in Header */}
          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginLeft: '0.5rem' }}>
              <Link
                href="/profile"
                className={`nav-link ${isActive('/profile') ? 'active' : ''}`}
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontWeight: 600,
                }}
              >
                {user.profilePictureUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img
                    src={user.profilePictureUrl}
                    alt=""
                    style={{ width: '24px', height: '24px', borderRadius: '50%', objectFit: 'cover' }}
                    onError={(e) => {
                      (e.target as HTMLElement).style.display = 'none';
                    }}
                  />
                ) : (
                  <span>👤</span>
                )}
                <span>{user.fullName || 'Citizen'}</span>
              </Link>
              <button
                onClick={logout}
                className="btn btn-outline btn-sm"
                style={{
                  padding: '4px 10px',
                  fontSize: '0.8rem',
                  borderColor: 'var(--color-border)',
                }}
              >
                Log out
              </button>
            </div>
          ) : (
            <Link
              href="/login"
              className="btn btn-outline btn-sm"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                padding: '6px 12px',
                fontSize: '0.85rem',
              }}
            >
              {/* Google G Icon */}
              <svg width="14" height="14" viewBox="0 0 48 48" aria-hidden="true">
                <path fill="#4285F4" d="M46.145 24.536c0-1.637-.146-3.21-.418-4.726H24.5v8.945h12.146c-.524 2.825-2.118 5.218-4.517 6.825v5.673h7.318c4.28-3.94 6.698-9.743 6.698-16.717z"/>
                <path fill="#34A853" d="M24.5 46.5c6.21 0 11.417-2.062 15.223-5.582l-7.318-5.673c-2.06 1.38-4.695 2.195-7.905 2.195-6.079 0-11.224-4.103-13.064-9.636H3.84v5.864C7.653 41.228 15.485 46.5 24.5 46.5z"/>
                <path fill="#FBBC05" d="M11.436 27.804c-.464-1.38-.727-2.85-.727-4.304s.263-2.924.727-4.304V13.332H3.84C2.302 16.388 1.41 19.84 1.41 23.5s.892 7.112 2.43 10.168l7.596-5.864z"/>
                <path fill="#EA4335" d="M24.5 9.56c3.377 0 6.409 1.162 8.795 3.44l6.59-6.59C35.91 2.59 30.703.5 24.5.5 15.485.5 7.653 5.772 3.84 13.332l7.596 5.864c1.84-5.533 6.985-9.636 13.064-9.636z"/>
              </svg>
              <span>Continue with Google</span>
            </Link>
          )}

          <Link href="/#report" className="btn btn-primary btn-sm">
            Report an Issue
          </Link>
        </nav>

        {/* Mobile Hamburger Button */}
        <button
          className="nav-mobile-toggle"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          aria-expanded={mobileMenuOpen}
          aria-label={mobileMenuOpen ? 'Close Navigation Menu' : 'Open Navigation Menu'}
          style={{
            display: 'none',
            flexDirection: 'column',
            gap: '5px',
            padding: '8px',
            minWidth: '44px',
            minHeight: '44px',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <span style={{ width: '22px', height: '2px', backgroundColor: '#0f172a', display: 'block' }} />
          <span style={{ width: '22px', height: '2px', backgroundColor: '#0f172a', display: 'block' }} />
          <span style={{ width: '22px', height: '2px', backgroundColor: '#0f172a', display: 'block' }} />
        </button>
      </div>

      {/* Mobile Dropdown */}
      {mobileMenuOpen && (
        <div
          style={{
            backgroundColor: '#ffffff',
            borderBottom: '1px solid var(--color-border)',
            padding: '1.25rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '1rem',
          }}
        >
          <Link
            href="/"
            onClick={() => setMobileMenuOpen(false)}
            className={`nav-link ${isActive('/') ? 'active' : ''}`}
          >
            Home
          </Link>
          <Link
            href="/issues"
            onClick={() => setMobileMenuOpen(false)}
            className={`nav-link ${isActive('/issues') ? 'active' : ''}`}
          >
            Explore Issues
          </Link>
          <Link
            href="/map"
            onClick={() => setMobileMenuOpen(false)}
            className={`nav-link ${isActive('/map') ? 'active' : ''}`}
          >
            Civic Map
          </Link>
          <Link
            href="/#how-it-works"
            onClick={() => setMobileMenuOpen(false)}
            className="nav-link"
          >
            How it Works
          </Link>

          {isAuthenticated ? (
            <>
              <Link
                href="/profile"
                onClick={() => setMobileMenuOpen(false)}
                className={`nav-link ${isActive('/profile') ? 'active' : ''}`}
              >
                👤 {user.fullName || 'Citizen Profile'}
              </Link>
              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  logout();
                }}
                className="btn btn-outline"
                style={{ width: '100%', textAlign: 'center' }}
              >
                Log out
              </button>
            </>
          ) : (
            <Link
              href="/login"
              onClick={() => setMobileMenuOpen(false)}
              className="btn btn-outline"
              style={{ width: '100%', textAlign: 'center' }}
            >
              Continue with Google
            </Link>
          )}

          <Link
            href="/#report"
            onClick={() => setMobileMenuOpen(false)}
            className="btn btn-primary"
            style={{ width: '100%', textAlign: 'center' }}
          >
            Report an Issue
          </Link>
        </div>
      )}
    </header>
  );
}
