'use client';

import React from 'react';

interface GoogleSignInButtonProps {
  onPress?: () => void;
  onClick?: () => void;
  loading?: boolean;
  disabled?: boolean;
  text?: string;
  style?: React.CSSProperties;
  className?: string;
}

export function GoogleSignInButton({
  onPress,
  onClick,
  loading = false,
  disabled = false,
  text = 'Continue with Google',
  style,
  className = '',
}: GoogleSignInButtonProps) {
  const handleClick = () => {
    if (disabled || loading) return;
    if (onClick) onClick();
    if (onPress) onPress();
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={disabled || loading}
      className={`btn-google ${className}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: '#FFFFFF',
        color: '#1F1F1F',
        border: '1px solid #DADCE0',
        borderRadius: 'var(--radius-md, 8px)',
        padding: '0.625rem 1.25rem',
        fontSize: '0.9375rem',
        fontWeight: 600,
        fontFamily: 'inherit',
        cursor: disabled || loading ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.6 : 1,
        transition: 'background-color 0.2s, box-shadow 0.2s',
        minHeight: '44px',
        boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
        ...style,
      }}
      aria-label={text}
    >
      {loading ? (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          <span
            style={{
              width: '16px',
              height: '16px',
              border: '2px solid #DADCE0',
              borderTopColor: '#4285F4',
              borderRadius: '50%',
              display: 'inline-block',
              animation: 'spin 1s linear infinite',
            }}
          />
          Signing you in...
        </span>
      ) : (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '12px' }}>
          {/* Official Google "G" Vector Icon */}
          <svg width="20" height="20" viewBox="0 0 48 48" aria-hidden="true" style={{ display: 'block' }}>
            <path
              fill="#4285F4"
              d="M46.145 24.536c0-1.637-.146-3.21-.418-4.726H24.5v8.945h12.146c-.524 2.825-2.118 5.218-4.517 6.825v5.673h7.318c4.28-3.94 6.698-9.743 6.698-16.717z"
            />
            <path
              fill="#34A853"
              d="M24.5 46.5c6.21 0 11.417-2.062 15.223-5.582l-7.318-5.673c-2.06 1.38-4.695 2.195-7.905 2.195-6.079 0-11.224-4.103-13.064-9.636H3.84v5.864C7.653 41.228 15.485 46.5 24.5 46.5z"
            />
            <path
              fill="#FBBC05"
              d="M11.436 27.804c-.464-1.38-.727-2.85-.727-4.304s.263-2.924.727-4.304V13.332H3.84C2.302 16.388 1.41 19.84 1.41 23.5s.892 7.112 2.43 10.168l7.596-5.864z"
            />
            <path
              fill="#EA4335"
              d="M24.5 9.56c3.377 0 6.409 1.162 8.795 3.44l6.59-6.59C35.91 2.59 30.703.5 24.5.5 15.485.5 7.653 5.772 3.84 13.332l7.596 5.864c1.84-5.533 6.985-9.636 13.064-9.636z"
            />
          </svg>
          {text}
        </span>
      )}
    </button>
  );
}
