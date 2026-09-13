'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { CommentResponse } from '../types/issue';
import { formatRelativeTime } from '../lib/formatters';
import { useAuth } from '../lib/context/AuthContext';

interface CommentListProps {
  comments: CommentResponse[];
  issueId: string;
}

export function CommentList({ comments: initialComments, issueId }: CommentListProps) {
  const { user, status } = useAuth();
  const [comments, setComments] = useState<CommentResponse[]>(initialComments);
  const [commentText, setCommentText] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isAuthenticated = status === 'AUTHENTICATED';

  const handlePostComment = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = commentText.trim();
    if (!trimmed || isSubmitting) return;

    setIsSubmitting(true);
    setError(null);

    try {
      const res = await fetch(`/api/web/issues/${issueId}/comments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: trimmed }),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setError(err.message || 'Could not post comment. Please try again.');
        return;
      }

      const newComment = await res.json();
      setComments((prev) => [newComment, ...prev]);
      setCommentText('');
    } catch {
      setError('Network error while posting comment. Please check your connection.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
      {/* Auth State / Comment Participation Box */}
      {isAuthenticated ? (
        <form
          onSubmit={handlePostComment}
          style={{
            backgroundColor: 'var(--color-surface-muted)',
            padding: '1.25rem',
            borderRadius: 'var(--radius-lg)',
            border: '1px solid var(--color-border)',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.75rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {user?.profilePictureUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={user.profilePictureUrl}
                alt=""
                style={{ width: '24px', height: '24px', borderRadius: '50%' }}
              />
            ) : (
              <span>👤</span>
            )}
            <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--color-text)' }}>
              Comment as {user?.fullName || 'Citizen'}
            </span>
          </div>

          <textarea
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            placeholder="Add verified community updates or corroborating context..."
            rows={3}
            disabled={isSubmitting}
            style={{
              width: '100%',
              padding: '0.75rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--color-border)',
              backgroundColor: '#ffffff',
              fontFamily: 'inherit',
              fontSize: '0.9rem',
              resize: 'vertical',
            }}
          />

          {error && (
            <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', margin: 0 }}>
              {error}
            </p>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button
              type="submit"
              disabled={isSubmitting || !commentText.trim()}
              className="btn btn-primary btn-sm"
            >
              {isSubmitting ? 'Posting...' : 'Post Comment'}
            </button>
          </div>
        </form>
      ) : (
        <div
          style={{
            padding: '1rem 1.25rem',
            backgroundColor: 'var(--color-surface-muted)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--color-border)',
            fontSize: '0.875rem',
            color: 'var(--color-text-secondary)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '0.75rem',
          }}
        >
          <span>
            💬 <strong>Community Discussion:</strong> Continue with Google to join the discussion and post updates.
          </span>
          <Link
            href={`/login?returnTo=/issues/${issueId}`}
            className="btn btn-outline btn-sm"
            style={{
              backgroundColor: '#fff',
              fontSize: '0.8rem',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
            }}
          >
            <svg width="14" height="14" viewBox="0 0 48 48" aria-hidden="true">
              <path fill="#4285F4" d="M46.145 24.536c0-1.637-.146-3.21-.418-4.726H24.5v8.945h12.146c-.524 2.825-2.118 5.218-4.517 6.825v5.673h7.318c4.28-3.94 6.698-9.743 6.698-16.717z"/>
              <path fill="#34A853" d="M24.5 46.5c6.21 0 11.417-2.062 15.223-5.582l-7.318-5.673c-2.06 1.38-4.695 2.195-7.905 2.195-6.079 0-11.224-4.103-13.064-9.636H3.84v5.864C7.653 41.228 15.485 46.5 24.5 46.5z"/>
              <path fill="#FBBC05" d="M11.436 27.804c-.464-1.38-.727-2.85-.727-4.304s.263-2.924.727-4.304V13.332H3.84C2.302 16.388 1.41 19.84 1.41 23.5s.892 7.112 2.43 10.168l7.596-5.864z"/>
              <path fill="#EA4335" d="M24.5 9.56c3.377 0 6.409 1.162 8.795 3.44l6.59-6.59C35.91 2.59 30.703.5 24.5.5 15.485.5 7.653 5.772 3.84 13.332l7.596 5.864c1.84-5.533 6.985-9.636 13.064-9.636z"/>
            </svg>
            <span>Continue with Google</span>
          </Link>
        </div>
      )}

      {comments.length === 0 ? (
        <p style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem', fontStyle: 'italic', padding: '0.5rem 0' }}>
          No community comments yet on this report.
        </p>
      ) : (
        comments.map((c) => {
          const author = c.author?.displayName || 'Citizen';
          return (
            <div
              key={c.id}
              style={{
                backgroundColor: 'var(--color-surface-muted)',
                padding: '1rem',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--color-border-light)',
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  marginBottom: '0.35rem',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      backgroundColor: 'var(--color-border)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.75rem',
                    }}
                    aria-hidden="true"
                  >
                    👤
                  </div>
                  <span style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--color-text)' }}>
                    {author}
                  </span>
                </div>
                <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                  {formatRelativeTime(c.createdAt)}
                </span>
              </div>

              <p
                style={{
                  fontSize: '0.9rem',
                  color: c.deleted ? 'var(--color-text-muted)' : 'var(--color-text-secondary)',
                  lineHeight: 1.5,
                  fontStyle: c.deleted ? 'italic' : 'normal',
                }}
              >
                {c.content}
              </p>
            </div>
          );
        })
      )}
    </div>
  );
}
