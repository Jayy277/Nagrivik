'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import {
  AiDuplicateSuggestion,
  DuplicateConfidence,
  DuplicateSuggestionStatus,
} from '../../../types/duplicate';
import {
  getDuplicateSuggestions,
  linkDuplicateSuggestion,
  dismissDuplicateSuggestion,
} from '../../../lib/api/duplicates';

export default function AdminDuplicatesPage() {
  const [statusFilter, setStatusFilter] = useState<DuplicateSuggestionStatus>('SUGGESTED');
  const [confidenceFilter, setConfidenceFilter] = useState<string>('ALL');
  const [suggestions, setSuggestions] = useState<AiDuplicateSuggestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Modals state
  const [linkingSuggestion, setLinkingSuggestion] = useState<AiDuplicateSuggestion | null>(null);
  const [dismissingSuggestion, setDismissingSuggestion] = useState<AiDuplicateSuggestion | null>(null);
  const [dismissReason, setDismissReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await getDuplicateSuggestions({
        status: statusFilter,
        confidence: confidenceFilter !== 'ALL' ? (confidenceFilter as DuplicateConfidence) : undefined,
      });
      setSuggestions(res.content || []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to load duplicate suggestions');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, confidenceFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleLink = async () => {
    if (!linkingSuggestion) return;
    setSubmitting(true);
    try {
      await linkDuplicateSuggestion(linkingSuggestion.id);
      setActionSuccess(`Successfully linked "${linkingSuggestion.sourceIssue.title}" as duplicate.`);
      setLinkingSuggestion(null);
      await loadData();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to link duplicate suggestion');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDismiss = async () => {
    if (!dismissingSuggestion) return;
    setSubmitting(true);
    try {
      await dismissDuplicateSuggestion(dismissingSuggestion.id, dismissReason);
      setActionSuccess(`Dismissed suggestion for "${dismissingSuggestion.sourceIssue.title}".`);
      setDismissingSuggestion(null);
      setDismissReason('');
      await loadData();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to dismiss duplicate suggestion');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto', padding: '32px 24px' }}>
      {/* Header */}
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: '800', color: '#0f172a', marginBottom: '8px' }}>
          AI Duplicate Suggestion Review
        </h1>
        <p style={{ fontSize: '15px', color: '#64748b', lineHeight: '1.5' }}>
          Review candidate duplicate pairs flagged by semantic and spatial signals across Ahmedabad.
        </p>
      </div>

      {/* Advisory Notice */}
      <div
        style={{
          background: '#eff6ff',
          border: '1px solid #bfdbfe',
          borderRadius: '12px',
          padding: '16px 20px',
          marginBottom: '28px',
          display: 'flex',
          gap: '12px',
          alignItems: 'flex-start',
        }}
      >
        <span style={{ fontSize: '20px' }}>ℹ️</span>
        <div>
          <h2 style={{ fontSize: '14px', fontWeight: '700', color: '#1e40af', margin: '0 0 4px 0' }}>
            Advisory Signal Only — Human Review Required
          </h2>
          <p style={{ fontSize: '13px', color: '#1e3a8a', margin: 0, lineHeight: '1.5' }}>
            AI similarity scores and signals are heuristic recommendations. AI will never automatically merge, hide, or delete issues. Linking an issue as duplicate requires explicit administrative confirmation, normalizes to the canonical root primary, and preserves all citizen comments, supporters, and media.
          </p>
        </div>
      </div>

      {/* Alerts */}
      {actionSuccess && (
        <div style={{ background: '#ecfdf5', border: '1px solid #a7f3d0', color: '#065f46', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px' }}>
          ✓ {actionSuccess}
        </div>
      )}
      {error && (
        <div style={{ background: '#fef2f2', border: '1px solid #fecaca', color: '#991b1b', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px' }}>
          ⚠️ {error}
        </div>
      )}

      {/* Filters Bar */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        {/* Status Tabs */}
        <div style={{ display: 'flex', background: '#e2e8f0', borderRadius: '10px', padding: '4px', gap: '4px' }}>
          {(['SUGGESTED', 'LINKED', 'DISMISSED'] as DuplicateSuggestionStatus[]).map((tab) => (
            <button
              key={tab}
              onClick={() => setStatusFilter(tab)}
              style={{
                padding: '8px 16px',
                borderRadius: '8px',
                border: 'none',
                background: statusFilter === tab ? '#ffffff' : 'transparent',
                color: statusFilter === tab ? '#0f172a' : '#64748b',
                fontWeight: '600',
                fontSize: '13px',
                cursor: 'pointer',
                boxShadow: statusFilter === tab ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                transition: 'all 0.15s ease',
              }}
            >
              {tab === 'SUGGESTED' ? 'Pending Review' : tab === 'LINKED' ? 'Linked as Duplicate' : 'Dismissed'}
            </button>
          ))}
        </div>

        {/* Confidence Filter */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '13px', fontWeight: '600', color: '#64748b' }}>Confidence:</span>
          <select
            value={confidenceFilter}
            onChange={(e) => setConfidenceFilter(e.target.value)}
            style={{
              padding: '8px 14px',
              borderRadius: '8px',
              border: '1px solid #cbd5e1',
              background: '#ffffff',
              fontSize: '13px',
              color: '#0f172a',
              cursor: 'pointer',
            }}
          >
            <option value="ALL">All Confidence Levels</option>
            <option value="HIGH">High (85%+)</option>
            <option value="LIKELY">Likely (70-84%)</option>
            <option value="POSSIBLE">Possible (40-69%)</option>
            <option value="LOW">Low (&lt;40%)</option>
          </select>
        </div>
      </div>

      {/* Main Content */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '64px 0', color: '#64748b' }}>
          <div style={{ width: '40px', height: '40px', border: '4px solid #e2e8f0', borderTopColor: '#1e40af', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px' }} />
          Loading duplicate suggestions...
        </div>
      ) : suggestions.length === 0 ? (
        <div style={{ background: '#ffffff', border: '1px solid #e2e8f0', borderRadius: '12px', padding: '48px 24px', textAlign: 'center', color: '#64748b' }}>
          <span style={{ fontSize: '32px', display: 'block', marginBottom: '12px' }}>🎯</span>
          <h3 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginBottom: '6px' }}>No Suggestions Found</h3>
          <p style={{ fontSize: '14px', color: '#64748b', margin: 0 }}>
            No duplicate suggestions match the selected status ({statusFilter}) and confidence ({confidenceFilter}).
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {suggestions.map((item) => {
            const isHigh = item.confidence === 'HIGH';
            const isLikely = item.confidence === 'LIKELY';

            const badgeBg = isHigh ? '#ecfdf5' : isLikely ? '#eff6ff' : '#fffbeb';
            const badgeColor = isHigh ? '#065f46' : isLikely ? '#1e40af' : '#92400e';
            const badgeBorder = isHigh ? '#a7f3d0' : isLikely ? '#bfdbfe' : '#fde68a';

            return (
              <div
                key={item.id}
                style={{
                  background: '#ffffff',
                  border: '1px solid #e2e8f0',
                  borderRadius: '12px',
                  boxShadow: '0 2px 4px rgba(0,0,0,0.03)',
                  overflow: 'hidden',
                }}
              >
                {/* Card Header Bar */}
                <div
                  style={{
                    padding: '14px 20px',
                    background: '#f8fafc',
                    borderBottom: '1px solid #e2e8f0',
                    display: 'flex',
                    flexWrap: 'wrap',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    gap: '12px',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <span
                      style={{
                        background: badgeBg,
                        color: badgeColor,
                        border: `1px solid ${badgeBorder}`,
                        padding: '4px 10px',
                        borderRadius: '9999px',
                        fontSize: '12px',
                        fontWeight: '700',
                        letterSpacing: '0.02em',
                      }}
                    >
                      {item.confidence} CONFIDENCE ({item.score}%)
                    </span>
                    <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                      Model: {item.model} ({item.calculationVersion})
                    </span>
                  </div>

                  <span
                    style={{
                      fontSize: '12px',
                      fontWeight: '600',
                      padding: '3px 8px',
                      borderRadius: '6px',
                      background: item.status === 'LINKED' ? '#dcfce7' : item.status === 'DISMISSED' ? '#fee2e2' : '#f1f5f9',
                      color: item.status === 'LINKED' ? '#166534' : item.status === 'DISMISSED' ? '#991b1b' : '#475569',
                    }}
                  >
                    {item.status}
                  </span>
                </div>

                {/* Comparison Grid */}
                <div style={{ padding: '20px', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '24px' }}>
                  {/* Source Issue */}
                  <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                    <div style={{ fontSize: '11px', fontWeight: '700', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '8px' }}>
                      Reported Issue (Potential Duplicate)
                    </div>
                    <h4 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', margin: '0 0 6px 0' }}>
                      {item.sourceIssue.title}
                    </h4>
                    <p style={{ fontSize: '13px', color: '#475569', margin: '0 0 12px 0', lineHeight: '1.4' }}>
                      {item.sourceIssue.description || 'No description provided.'}
                    </p>
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', fontSize: '12px', color: '#64748b' }}>
                      <span style={{ background: '#e2e8f0', padding: '2px 8px', borderRadius: '4px' }}>
                        {item.sourceIssue.categoryName || 'General'}
                      </span>
                      <span style={{ background: '#e2e8f0', padding: '2px 8px', borderRadius: '4px' }}>
                        Status: {item.sourceIssue.status}
                      </span>
                      <Link
                        href={`/issues/${item.sourceIssue.id}`}
                        target="_blank"
                        style={{ color: '#1e40af', fontWeight: '600', textDecoration: 'none', marginLeft: 'auto' }}
                      >
                        Inspect Issue ↗
                      </Link>
                    </div>
                  </div>

                  {/* Candidate Primary */}
                  <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '10px', border: '1px solid #e2e8f0' }}>
                    <div style={{ fontSize: '11px', fontWeight: '700', color: '#166534', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '8px' }}>
                      Candidate Primary Issue
                    </div>
                    <h4 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', margin: '0 0 6px 0' }}>
                      {item.candidateIssue.title}
                    </h4>
                    <p style={{ fontSize: '13px', color: '#475569', margin: '0 0 12px 0', lineHeight: '1.4' }}>
                      {item.candidateIssue.description || 'No description provided.'}
                    </p>
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', fontSize: '12px', color: '#64748b' }}>
                      <span style={{ background: '#e2e8f0', padding: '2px 8px', borderRadius: '4px' }}>
                        {item.candidateIssue.categoryName || 'General'}
                      </span>
                      <span style={{ background: '#e2e8f0', padding: '2px 8px', borderRadius: '4px' }}>
                        Status: {item.candidateIssue.status}
                      </span>
                      <Link
                        href={`/issues/${item.candidateIssue.id}`}
                        target="_blank"
                        style={{ color: '#166534', fontWeight: '600', textDecoration: 'none', marginLeft: 'auto' }}
                      >
                        Inspect Primary ↗
                      </Link>
                    </div>
                  </div>
                </div>

                {/* Signals Bar & Actions */}
                <div
                  style={{
                    padding: '16px 20px',
                    borderTop: '1px solid #e2e8f0',
                    background: '#ffffff',
                    display: 'flex',
                    flexWrap: 'wrap',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    gap: '16px',
                  }}
                >
                  {/* Explainable Signals */}
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }}>
                    <span style={{ fontSize: '12px', fontWeight: '600', color: '#64748b' }}>Detected Signals:</span>
                    {item.signals && item.signals.length > 0 ? (
                      item.signals.map((sig, idx) => (
                        <span
                          key={idx}
                          style={{
                            background: '#f1f5f9',
                            color: '#334155',
                            border: '1px solid #cbd5e1',
                            padding: '3px 10px',
                            borderRadius: '9999px',
                            fontSize: '12px',
                            fontWeight: '500',
                          }}
                        >
                          {sig}
                        </span>
                      ))
                    ) : (
                      <span style={{ fontSize: '12px', color: '#94a3b8' }}>Standard geographic proximity</span>
                    )}
                  </div>

                  {/* Actions */}
                  {item.status === 'SUGGESTED' ? (
                    <div style={{ display: 'flex', gap: '10px' }}>
                      <button
                        onClick={() => setDismissingSuggestion(item)}
                        style={{
                          padding: '8px 16px',
                          borderRadius: '8px',
                          border: '1px solid #cbd5e1',
                          background: '#ffffff',
                          color: '#475569',
                          fontWeight: '600',
                          fontSize: '13px',
                          cursor: 'pointer',
                        }}
                      >
                        Not a Duplicate
                      </button>
                      <button
                        onClick={() => setLinkingSuggestion(item)}
                        style={{
                          padding: '8px 18px',
                          borderRadius: '8px',
                          border: 'none',
                          background: '#166534',
                          color: '#ffffff',
                          fontWeight: '600',
                          fontSize: '13px',
                          cursor: 'pointer',
                          boxShadow: '0 1px 2px rgba(0,0,0,0.05)',
                        }}
                      >
                        Link as Duplicate
                      </button>
                    </div>
                  ) : item.status === 'LINKED' ? (
                    <span style={{ fontSize: '12px', color: '#166534', fontWeight: '600' }}>
                      ✓ Linked to Primary Issue
                    </span>
                  ) : (
                    <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                      Dismissed {item.dismissReason ? `("${item.dismissReason}")` : ''}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Link Confirmation Modal */}
      {linkingSuggestion && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(15, 23, 42, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '20px' }}>
          <div style={{ background: '#ffffff', borderRadius: '16px', maxWidth: '520px', width: '100%', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}>
            <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
              Confirm Duplicate Linking
            </h3>
            <p style={{ fontSize: '14px', color: '#475569', lineHeight: '1.5', marginBottom: '16px' }}>
              Are you sure you want to mark <strong>&quot;{linkingSuggestion.sourceIssue.title}&quot;</strong> as a duplicate of <strong>&quot;{linkingSuggestion.candidateIssue.title}&quot;</strong>?
            </p>
            <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '12px', marginBottom: '20px', fontSize: '13px', color: '#475569' }}>
              ℹ️ The reported issue will point to the primary issue via canonical normalization. Citizen comments, media, and supporters are completely preserved.
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                onClick={() => setLinkingSuggestion(null)}
                disabled={submitting}
                style={{ padding: '8px 16px', borderRadius: '8px', border: '1px solid #cbd5e1', background: '#ffffff', color: '#475569', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
              >
                Cancel
              </button>
              <button
                onClick={handleLink}
                disabled={submitting}
                style={{ padding: '8px 18px', borderRadius: '8px', border: 'none', background: '#166534', color: '#ffffff', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
              >
                {submitting ? 'Linking...' : 'Confirm & Link Duplicate'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Dismiss Modal */}
      {dismissingSuggestion && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(15, 23, 42, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '20px' }}>
          <div style={{ background: '#ffffff', borderRadius: '16px', maxWidth: '520px', width: '100%', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}>
            <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
              Dismiss Duplicate Suggestion
            </h3>
            <p style={{ fontSize: '14px', color: '#475569', lineHeight: '1.5', marginBottom: '16px' }}>
              Dismiss suggestion between <strong>&quot;{dismissingSuggestion.sourceIssue.title}&quot;</strong> and <strong>&quot;{dismissingSuggestion.candidateIssue.title}&quot;</strong>. This records your decision and prevents repeated prompts.
            </p>
            <div style={{ marginBottom: '20px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '6px' }}>
                Reason for Dismissal (Optional)
              </label>
              <textarea
                value={dismissReason}
                onChange={(e) => setDismissReason(e.target.value)}
                placeholder="e.g. Distinct physical pothole 20m north on opposite lane"
                rows={3}
                maxLength={255}
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '13px',
                  color: '#0f172a',
                  resize: 'none',
                }}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                onClick={() => {
                  setDismissingSuggestion(null);
                  setDismissReason('');
                }}
                disabled={submitting}
                style={{ padding: '8px 16px', borderRadius: '8px', border: '1px solid #cbd5e1', background: '#ffffff', color: '#475569', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
              >
                Cancel
              </button>
              <button
                onClick={handleDismiss}
                disabled={submitting}
                style={{ padding: '8px 18px', borderRadius: '8px', border: 'none', background: '#dc2626', color: '#ffffff', fontWeight: '600', fontSize: '13px', cursor: 'pointer' }}
              >
                {submitting ? 'Dismissing...' : 'Dismiss Suggestion'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
