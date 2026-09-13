'use client';

import React, { useState } from 'react';

interface ShareButtonProps {
  title: string;
  url?: string;
  categoryName?: string;
}

export function ShareButton({ title, url, categoryName }: ShareButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleShare = async () => {
    const shareUrl = url || (typeof window !== 'undefined' ? window.location.href : '');
    const shareText = `[Nagrivic] ${title}${categoryName ? ` • ${categoryName}` : ''} — View public civic report:`;

    if (typeof navigator !== 'undefined' && navigator.share) {
      try {
        await navigator.share({
          title: `Nagrivic: ${title}`,
          text: shareText,
          url: shareUrl,
        });
        return;
      } catch {
        // Fallback to clipboard if share cancelled or rejected
      }
    }

    // Clipboard fallback
    if (typeof navigator !== 'undefined' && navigator.clipboard) {
      try {
        await navigator.clipboard.writeText(shareUrl);
        setCopied(true);
        setTimeout(() => setCopied(false), 2500);
      } catch {
        // Fail gracefully
      }
    }
  };

  return (
    <button
      onClick={handleShare}
      className="btn btn-secondary btn-sm"
      aria-label={copied ? 'Link copied to clipboard' : 'Share this civic issue'}
      style={{ position: 'relative' }}
    >
      <span>{copied ? '✓' : '↗'}</span>
      <span>{copied ? 'Link Copied!' : 'Share Issue'}</span>
    </button>
  );
}
