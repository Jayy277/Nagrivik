'use client';

import React, { useEffect, useState, useCallback, use } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  getAuthorityIssueDetail,
  changeAuthorityIssueStatus,
  uploadResolutionEvidence,
  ConcurrencyConflictError,
} from '../../../../lib/api/authority';
import {
  AuthorityIssueDetailResponse,
  ResolutionEvidenceResponse,
  ResolutionEvidenceType,
} from '../../../../types/authority';
import { IssueStatus, PriorityLevel } from '../../../../types/issue';
import { ApiClientError } from '../../../../types/api';
import ImageAiAnalysisCard from '../../../../components/ImageAiAnalysisCard';
import AiPriorityCard from '../../../../components/AiPriorityCard';

const STATUS_BADGE_STYLES: Record<IssueStatus, { bg: string; text: string; border: string }> = {
  REPORTED: { bg: '#fef3c7', text: '#92400e', border: '#fcd34d' },
  VERIFIED: { bg: '#e0f2fe', text: '#075985', border: '#7dd3fc' },
  ACKNOWLEDGED: { bg: '#ede9fe', text: '#5b21b6', border: '#c4b5fd' },
  IN_PROGRESS: { bg: '#fce7f3', text: '#9d174d', border: '#f472b6' },
  RESOLVED: { bg: '#dcfce7', text: '#166534', border: '#86efac' },
  CITIZEN_VERIFIED: { bg: '#ccfbf1', text: '#115e59', border: '#5eead4' },
  NOT_FIXED: { bg: '#fee2e2', text: '#991b1b', border: '#fca5a5' },
};

const PRIORITY_BADGE_STYLES: Record<PriorityLevel, { bg: string; text: string }> = {
  LOW: { bg: '#f1f5f9', text: '#475569' },
  MEDIUM: { bg: '#e0f2fe', text: '#0369a1' },
  HIGH: { bg: '#ffedd5', text: '#c2410c' },
  CRITICAL: { bg: '#fee2e2', text: '#b91c1c' },
};

const STATUS_ACTION_LABELS: Record<IssueStatus, string> = {
  REPORTED: 'Mark as Reported',
  VERIFIED: 'Verify Issue (Inspection Confirmed)',
  ACKNOWLEDGED: 'Acknowledge (Create Work Order)',
  IN_PROGRESS: 'Dispatch Field Crew (In Progress)',
  RESOLVED: 'Mark as Resolved',
  CITIZEN_VERIFIED: 'Citizen Verified',
  NOT_FIXED: 'Citizen Reported Not Fixed',
};

export default function AuthorityIssueDetailPage({
  params,
}: {
  params: Promise<{ issueId: string }>;
}) {
  const resolvedParams = use(params);
  const issueId = resolvedParams.issueId;
  const router = useRouter();

  const [issue, setIssue] = useState<AuthorityIssueDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isScopeForbidden, setIsScopeForbidden] = useState(false);

  // Transition modal state
  const [targetStatus, setTargetStatus] = useState<IssueStatus | null>(null);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);
  const [conflictWarning, setConflictWarning] = useState<string | null>(null);

  // Evidence upload modal state
  const [isEvidenceModalOpen, setIsEvidenceModalOpen] = useState(false);
  const [evidenceType, setEvidenceType] = useState<ResolutionEvidenceType>('COMPLETION_PHOTO');
  const [evidenceFile, setEvidenceFile] = useState<File | null>(null);
  const [evidenceNote, setEvidenceNote] = useState('');
  const [capturedAt, setCapturedAt] = useState('');
  const [uploadingEvidence, setUploadingEvidence] = useState(false);
  const [evidenceModalError, setEvidenceModalError] = useState<string | null>(null);

  const fetchIssue = useCallback(async () => {
    setLoading(true);
    setError(null);
    setIsScopeForbidden(false);
    try {
      const data = await getAuthorityIssueDetail(issueId);
      setIssue(data);
    } catch (err: unknown) {
      if (err instanceof ApiClientError && err.status === 403) {
        setIsScopeForbidden(true);
      } else {
        const msg = err instanceof Error ? err.message : 'Unable to load issue details';
        setError(msg);
      }
    } finally {
      setLoading(false);
    }
  }, [issueId]);

  useEffect(() => {
    fetchIssue();
  }, [fetchIssue]);

  function handleOpenTransitionModal(status: IssueStatus) {
    setTargetStatus(status);
    setReason('');
    setModalError(null);
    setConflictWarning(null);
  }

  function handleCloseModal() {
    if (submitting) return;
    setTargetStatus(null);
    setReason('');
    setModalError(null);
  }

  async function handleExecuteTransition() {
    if (!targetStatus || !issue) return;

    if (targetStatus === 'RESOLVED' && (!reason || reason.trim() === '')) {
      setModalError('A reason/resolution report is mandatory when marking an issue as RESOLVED.');
      return;
    }

    setSubmitting(true);
    setModalError(null);
    setConflictWarning(null);

    try {
      await changeAuthorityIssueStatus(issue.id, {
        status: targetStatus,
        reason: reason.trim() || undefined,
        version: issue.version,
      });

      await fetchIssue();
      setTargetStatus(null);
      setReason('');
    } catch (err: unknown) {
      if (err instanceof ConcurrencyConflictError) {
        setConflictWarning(
          'Optimistic Locking Notice: This issue was modified by another officer while you were reviewing it. Reloading latest issue state...'
        );
        await fetchIssue();
      } else {
        const msg = err instanceof Error ? err.message : 'Failed to update issue status';
        setModalError(msg);
      }
    } finally {
      setSubmitting(false);
    }
  }

  function handleOpenEvidenceModal() {
    setIsEvidenceModalOpen(true);
    setEvidenceType('COMPLETION_PHOTO');
    setEvidenceFile(null);
    setEvidenceNote('');
    setCapturedAt('');
    setEvidenceModalError(null);
  }

  function handleCloseEvidenceModal() {
    if (uploadingEvidence) return;
    setIsEvidenceModalOpen(false);
    setEvidenceFile(null);
    setEvidenceNote('');
    setCapturedAt('');
    setEvidenceModalError(null);
  }

  async function handleUploadEvidence(e: React.FormEvent) {
    e.preventDefault();
    if (!issue) return;

    if (evidenceType === 'COMPLETION_NOTE' && !evidenceNote.trim()) {
      setEvidenceModalError('A note is mandatory for COMPLETION_NOTE evidence');
      return;
    }

    if ((evidenceType === 'COMPLETION_PHOTO' || evidenceType === 'BEFORE_AFTER_PHOTO') && !evidenceFile) {
      setEvidenceModalError(`An image file is required for ${evidenceType} evidence`);
      return;
    }

    setUploadingEvidence(true);
    setEvidenceModalError(null);

    const formData = new FormData();
    formData.append('evidenceType', evidenceType);
    if (evidenceFile) {
      formData.append('file', evidenceFile);
    }
    if (evidenceNote.trim()) {
      formData.append('note', evidenceNote.trim());
    }
    if (capturedAt) {
      formData.append('capturedAt', new Date(capturedAt).toISOString());
    }

    try {
      await uploadResolutionEvidence(issue.id, formData);
      await fetchIssue();
      setIsEvidenceModalOpen(false);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to upload resolution evidence';
      setEvidenceModalError(msg);
    } finally {
      setUploadingEvidence(false);
    }
  }

  if (loading) {
    return (
      <div style={{ padding: '64px 0', textAlign: 'center' }}>
        <div style={{ width: '40px', height: '40px', border: '3px solid #e2e8f0', borderTopColor: '#0284c7', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px' }} />
        <p style={{ color: '#64748b', fontSize: '15px' }}>Loading jurisdictional issue details...</p>
      </div>
    );
  }

  if (isScopeForbidden) {
    return (
      <div style={{ maxWidth: '640px', margin: '48px auto', padding: '32px', background: '#ffffff', borderRadius: '12px', border: '1px solid #fee2e2', textAlign: 'center', boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)' }}>
        <div style={{ width: '60px', height: '60px', background: '#fee2e2', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 16px', color: '#dc2626', fontSize: '28px' }}>
          🛡️
        </div>
        <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#991b1b', marginBottom: '8px' }}>
          Outside Authority Jurisdiction
        </h2>
        <p style={{ fontSize: '14px', color: '#475569', lineHeight: '1.6', marginBottom: '24px' }}>
          This civic issue is outside your designated ward, department, or civic body authority scope. Server-side jurisdictional policies prevent unauthorized inspection or operational modification.
        </p>
        <Link
          href="/authority/issues"
          style={{ display: 'inline-block', padding: '10px 20px', background: '#0284c7', color: '#ffffff', borderRadius: '6px', textDecoration: 'none', fontWeight: '600', fontSize: '14px' }}
        >
          ← Return to Assigned Issues
        </Link>
      </div>
    );
  }

  if (error || !issue) {
    return (
      <div style={{ padding: '32px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#991b1b' }}>
        <h3 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '8px' }}>Error Loading Issue</h3>
        <p style={{ fontSize: '14px', marginBottom: '16px' }}>{error || 'Issue not found'}</p>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={fetchIssue}
            style={{ padding: '8px 16px', background: '#dc2626', color: '#ffffff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: '500' }}
          >
            Retry
          </button>
          <Link
            href="/authority/issues"
            style={{ padding: '8px 16px', background: '#f1f5f9', color: '#475569', border: '1px solid #cbd5e1', borderRadius: '6px', textDecoration: 'none', fontSize: '13px' }}
          >
            Back to Queue
          </Link>
        </div>
      </div>
    );
  }

  const statusStyle = STATUS_BADGE_STYLES[issue.status] || { bg: '#f1f5f9', text: '#475569', border: '#cbd5e1' };
  const priorityStyle = PRIORITY_BADGE_STYLES[issue.priorityLevel] || { bg: '#f1f5f9', text: '#475569' };

  const canAttachEvidence = issue.status === 'IN_PROGRESS' || issue.status === 'RESOLVED';
  const evidenceList = issue.resolutionEvidence || [];

  return (
    <div>
      {/* Breadcrumb Navigation */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '20px', fontSize: '13px', color: '#64748b' }}>
        <Link href="/authority" style={{ color: '#0284c7', textDecoration: 'none' }}>Authority</Link>
        <span>/</span>
        <Link href="/authority/issues" style={{ color: '#0284c7', textDecoration: 'none' }}>Jurisdictional Issues</Link>
        <span>/</span>
        <span style={{ color: '#0f172a', fontWeight: '500' }}>#{issue.id.slice(0, 8)}</span>
      </div>

      {/* Concurrency Conflict Alert */}
      {conflictWarning && (
        <div style={{ padding: '16px 20px', background: '#fffbeb', border: '1px solid #fde68a', borderRadius: '8px', color: '#92400e', marginBottom: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <strong>⚠️ Concurrency Conflict Resolved:</strong> {conflictWarning}
          </div>
          <button
            onClick={() => setConflictWarning(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#92400e', fontWeight: '700' }}
          >
            ✕
          </button>
        </div>
      )}

      {/* Main Layout Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '24px', alignItems: 'start' }}>
        {/* Left Column: Details, Resolution Evidence, Media, Comments, Timeline */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Issue Header Card */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '12px', flexWrap: 'wrap' }}>
              <span style={{ fontSize: '12px', fontWeight: '700', padding: '3px 10px', borderRadius: '12px', background: statusStyle.bg, color: statusStyle.text, border: `1px solid ${statusStyle.border}` }}>
                {issue.status}
              </span>
              <span style={{ fontSize: '12px', fontWeight: '600', padding: '3px 10px', borderRadius: '4px', background: priorityStyle.bg, color: priorityStyle.text }}>
                {issue.priorityLevel} Priority ({issue.priorityScore}/100)
              </span>
              <span style={{ fontSize: '12px', color: '#475569', background: '#f1f5f9', padding: '3px 10px', borderRadius: '4px' }}>
                {issue.category.name}
              </span>
            </div>

            <h1 style={{ fontSize: '22px', fontWeight: '700', color: '#0f172a', margin: '0 0 12px 0', lineHeight: '1.3' }}>
              {issue.title}
            </h1>

            <p style={{ fontSize: '15px', color: '#334155', lineHeight: '1.6', whiteSpace: 'pre-line', margin: '0 0 16px 0' }}>
              {issue.description}
            </p>

            <div style={{ display: 'flex', gap: '20px', fontSize: '13px', color: '#64748b', borderTop: '1px solid #f1f5f9', paddingTop: '16px', flexWrap: 'wrap' }}>
              <span>📍 Location: <strong>{issue.locationSummary}</strong></span>
              <span>👍 <strong>{issue.supportCount}</strong> Citizen Supports</span>
              <span>💬 <strong>{issue.commentCount}</strong> Citizen Comments</span>
              <span>Reported: <strong>{new Date(issue.createdAt).toLocaleString()}</strong></span>
            </div>
          </div>

          {/* Resolution Evidence Card */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
              <div>
                <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span>📸</span> Resolution Evidence ({evidenceList.length})
                </h2>
                <span style={{ fontSize: '12px', color: '#64748b' }}>
                  Authority completion photos, before/after evidence, and operational notes.
                </span>
              </div>
              {canAttachEvidence && (
                <button
                  id="attach-resolution-evidence-btn"
                  onClick={handleOpenEvidenceModal}
                  style={{
                    padding: '8px 14px',
                    background: '#0284c7',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '6px',
                    cursor: 'pointer',
                    fontSize: '13px',
                    fontWeight: '600',
                  }}
                >
                  + Add Resolution Evidence
                </button>
              )}
            </div>

            {evidenceList.length === 0 ? (
              <div style={{ padding: '20px', background: '#f8fafc', borderRadius: '8px', textAlign: 'center', color: '#64748b', fontSize: '13px' }}>
                No resolution evidence attached yet.
                {canAttachEvidence ? ' You may upload completion photos or notes using the button above.' : ' Evidence can be attached once the issue reaches IN_PROGRESS.'}
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: '16px' }}>
                {evidenceList.map((ev) => (
                  <div key={ev.id} style={{ border: '1px solid #e2e8f0', borderRadius: '8px', overflow: 'hidden', background: '#f8fafc' }}>
                    {ev.mediaUrl && (
                      <div style={{ width: '100%', height: '160px', background: '#e2e8f0', overflow: 'hidden' }}>
                        <img
                          src={ev.mediaUrl}
                          alt="Resolution evidence"
                          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                        />
                      </div>
                    )}
                    <div style={{ padding: '12px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                        <span style={{ fontSize: '11px', fontWeight: '700', padding: '2px 6px', borderRadius: '4px', background: '#e0f2fe', color: '#0369a1' }}>
                          {ev.evidenceType}
                        </span>
                        <span style={{ fontSize: '11px', color: '#94a3b8' }}>
                          {new Date(ev.createdAt).toLocaleDateString()}
                        </span>
                      </div>
                      {ev.note && (
                        <p style={{ fontSize: '13px', color: '#334155', margin: '6px 0', lineHeight: '1.4' }}>
                          {ev.note}
                        </p>
                      )}
                      <div style={{ fontSize: '11px', color: '#64748b', marginTop: '8px' }}>
                        Submitted by: <strong>{ev.submittedByName}</strong> ({ev.submittedByRole})
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Citizen Media Attachments */}
          {issue.media && issue.media.length > 0 && (
            <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
              <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', marginBottom: '16px' }}>
                📷 Citizen Report Evidence ({issue.media.length})
              </h2>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '16px' }}>
                {issue.media.map((m) => (
                  <div key={m.id} style={{ borderRadius: '8px', overflow: 'hidden', border: '1px solid #e2e8f0', background: '#f8fafc', display: 'flex', flexDirection: 'column' }}>
                    <img
                      src={m.mediaUrl}
                      alt="Citizen report evidence"
                      style={{ width: '100%', height: '140px', objectFit: 'cover', display: 'block' }}
                    />
                    <div style={{ padding: '8px', fontSize: '11px', color: '#64748b', textAlign: 'center' }}>
                      {m.mediaType}
                    </div>
                    {m.mediaType === 'IMAGE' && (
                      <div style={{ padding: '0 12px 12px 12px' }}>
                        <ImageAiAnalysisCard issueId={issue.id} mediaId={m.id} />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Citizen Comments (Sanitized for Privacy) */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', marginBottom: '16px' }}>
              💬 Citizen Comments ({issue.comments.length})
            </h2>
            {issue.comments.length === 0 ? (
              <p style={{ color: '#64748b', fontSize: '14px', margin: 0 }}>No citizen comments posted on this issue.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {issue.comments.map((c) => (
                  <div key={c.id} style={{ padding: '12px 16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <span style={{ width: '24px', height: '24px', borderRadius: '50%', background: '#0284c7', color: '#ffffff', fontSize: '11px', fontWeight: '700', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          {c.authorInitials || 'C'}
                        </span>
                        <span style={{ fontSize: '13px', fontWeight: '600', color: '#334155' }}>
                          {c.authorRole}
                        </span>
                      </div>
                      <span style={{ fontSize: '12px', color: '#94a3b8' }}>
                        {new Date(c.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    <p style={{ fontSize: '14px', color: '#1e293b', margin: 0, lineHeight: '1.4' }}>
                      {c.commentText}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Status History Audit Trail */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <h2 style={{ fontSize: '16px', fontWeight: '600', color: '#0f172a', marginBottom: '16px' }}>
              📜 Status Workflow History ({issue.statusHistory.length})
            </h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {issue.statusHistory.map((sh, idx) => (
                <div key={sh.id || idx} style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                  <div style={{ marginTop: '4px', width: '10px', height: '10px', borderRadius: '50%', background: '#0284c7' }} />
                  <div style={{ flex: 1 }}>
                    <div style={{ fontSize: '13px', fontWeight: '600', color: '#0f172a' }}>
                      {sh.fromStatus ? `${sh.fromStatus} → ` : 'Initial: '}{sh.toStatus}
                    </div>
                    {sh.reason && (
                      <p style={{ fontSize: '13px', color: '#475569', margin: '2px 0 4px 0', fontStyle: 'italic' }}>
                        &ldquo;{sh.reason}&rdquo;
                      </p>
                    )}
                    <div style={{ fontSize: '11px', color: '#94a3b8' }}>
                      By {sh.changedBy?.displayName || 'Authority Officer'} • {new Date(sh.createdAt).toLocaleString()}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column: Operational Controls & Civic Responsibility Summary */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Operational Workflow Action Box */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #0284c7', padding: '20px', boxShadow: '0 2px 4px rgba(2, 132, 199, 0.08)' }}>
            <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0369a1', margin: '0 0 8px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>⚡</span> Operational Workflow
            </h2>
            <p style={{ fontSize: '13px', color: '#475569', margin: '0 0 16px 0' }}>
              Authorized state machine transitions for current status (<strong>{issue.status}</strong>):
            </p>

            {/* Resolution Evidence Notice before resolving */}
            {issue.allowedTransitions.includes('RESOLVED') && (
              evidenceList.length === 0 ? (
                <div style={{ padding: '10px 12px', background: '#fffbeb', border: '1px solid #fde68a', borderRadius: '6px', fontSize: '12px', color: '#92400e', marginBottom: '14px', lineHeight: '1.4' }}>
                  ⚠️ <strong>Notice:</strong> No resolution evidence has been attached yet. You may still mark as resolved, or attach evidence above.
                </div>
              ) : (
                <div style={{ padding: '10px 12px', background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '6px', fontSize: '12px', color: '#166534', marginBottom: '14px' }}>
                  ✓ <strong>{evidenceList.length}</strong> resolution evidence item(s) attached.
                </div>
              )
            )}

            {issue.allowedTransitions.length === 0 ? (
              <div style={{ padding: '12px', background: '#f8fafc', borderRadius: '6px', fontSize: '13px', color: '#64748b' }}>
                No further authority actions available. Issue is either resolved, citizen-verified, or terminal.
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {issue.allowedTransitions.map((target) => (
                  <button
                    key={target}
                    onClick={() => handleOpenTransitionModal(target)}
                    style={{
                      padding: '10px 14px',
                      background: target === 'RESOLVED' ? '#166534' : '#0284c7',
                      color: '#ffffff',
                      border: 'none',
                      borderRadius: '8px',
                      cursor: 'pointer',
                      fontSize: '13px',
                      fontWeight: '600',
                      textAlign: 'left',
                      transition: 'opacity 0.15s ease',
                    }}
                  >
                    {STATUS_ACTION_LABELS[target] || `Advance to ${target}`} →
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* AI Priority Recommendation Card */}
          <AiPriorityCard
            issueId={issue.id}
            canReassess={true}
            onReassessed={fetchIssue}
          />

          {/* Civic Responsibility Details Card */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '20px', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
            <h2 style={{ fontSize: '15px', fontWeight: '600', color: '#0f172a', marginBottom: '12px' }}>
              🏛️ Civic Responsibility
            </h2>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', fontSize: '13px' }}>
              <div>
                <span style={{ color: '#64748b' }}>Civic Body:</span>
                <div style={{ fontWeight: '600', color: '#0f172a' }}>
                  {issue.civicResponsibility.civicBody?.name || 'Unassigned'}
                </div>
              </div>
              <div>
                <span style={{ color: '#64748b' }}>City:</span>
                <div style={{ fontWeight: '600', color: '#0f172a' }}>
                  {issue.civicResponsibility.city?.name || 'Unassigned'}
                </div>
              </div>
              <div>
                <span style={{ color: '#64748b' }}>Ward:</span>
                <div style={{ fontWeight: '600', color: '#0f172a' }}>
                  {issue.civicResponsibility.ward?.name ? `${issue.civicResponsibility.ward.name} (${issue.civicResponsibility.ward.code || 'N/A'})` : 'City-wide (No ward)'}
                </div>
              </div>
              <div>
                <span style={{ color: '#64748b' }}>Department:</span>
                <div style={{ fontWeight: '600', color: '#0f172a' }}>
                  {issue.civicResponsibility.department?.name ? `${issue.civicResponsibility.department.name} (${issue.civicResponsibility.department.code || 'N/A'})` : 'Unassigned'}
                </div>
              </div>
              <div>
                <span style={{ color: '#64748b' }}>Resolution Source:</span>
                <div style={{ fontWeight: '500', color: '#475569' }}>
                  {issue.civicResponsibility.source || 'Standard Rule'}
                </div>
              </div>
              <div>
                <span style={{ color: '#64748b' }}>Concurrency Version:</span>
                <div style={{ fontWeight: '600', color: '#0f172a' }}>
                  v{issue.version}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation / Status Change Modal */}
      {targetStatus && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '20px' }}>
          <div style={{ background: '#ffffff', borderRadius: '12px', maxWidth: '540px', width: '100%', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)' }}>
            <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', margin: '0 0 8px 0' }}>
              Confirm Status Transition: {targetStatus}
            </h3>
            <p style={{ fontSize: '14px', color: '#64748b', margin: '0 0 16px 0' }}>
              Advancing issue from <strong>{issue.status}</strong> to <strong>{targetStatus}</strong>.
            </p>

            {modalError && (
              <div style={{ padding: '12px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', color: '#991b1b', fontSize: '13px', marginBottom: '16px' }}>
                {modalError}
              </div>
            )}

            <div style={{ marginBottom: '16px' }}>
              <label htmlFor="transition-reason-input" style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '6px' }}>
                Operational Notes / Reason {targetStatus === 'RESOLVED' && <span style={{ color: '#dc2626' }}>* (Mandatory)</span>}
              </label>
              <textarea
                id="transition-reason-input"
                rows={4}
                maxLength={1000}
                placeholder={targetStatus === 'RESOLVED' ? 'Provide a detailed report of actions taken to resolve this issue...' : 'Optional operational context, work order number, or field notes...'}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', resize: 'vertical', boxSizing: 'border-box' }}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#94a3b8', marginTop: '4px' }}>
                <span>{targetStatus === 'RESOLVED' ? 'Resolution summary required for audit' : 'Max 1000 characters'}</span>
                <span>{reason.length}/1000</span>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                disabled={submitting}
                onClick={handleCloseModal}
                style={{ padding: '8px 16px', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: '500', color: '#475569' }}
              >
                Cancel
              </button>
              <button
                disabled={submitting || (targetStatus === 'RESOLVED' && !reason.trim())}
                onClick={handleExecuteTransition}
                style={{
                  padding: '8px 20px',
                  background: targetStatus === 'RESOLVED' ? '#166534' : '#0284c7',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: submitting || (targetStatus === 'RESOLVED' && !reason.trim()) ? 'not-allowed' : 'pointer',
                  opacity: submitting || (targetStatus === 'RESOLVED' && !reason.trim()) ? 0.6 : 1,
                  fontSize: '13px',
                  fontWeight: '600',
                }}
              >
                {submitting ? 'Executing Transition...' : `Confirm → ${targetStatus}`}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Attach Resolution Evidence Modal */}
      {isEvidenceModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(15, 23, 42, 0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '20px' }}>
          <form
            onSubmit={handleUploadEvidence}
            style={{ background: '#ffffff', borderRadius: '12px', maxWidth: '520px', width: '100%', padding: '24px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)' }}
          >
            <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', margin: '0 0 6px 0' }}>
              Attach Resolution Evidence
            </h3>
            <p style={{ fontSize: '13px', color: '#64748b', margin: '0 0 16px 0' }}>
              Upload photographic evidence or completion notes documenting municipal repair work.
            </p>

            {evidenceModalError && (
              <div style={{ padding: '12px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', color: '#991b1b', fontSize: '13px', marginBottom: '16px' }}>
                {evidenceModalError}
              </div>
            )}

            <div style={{ marginBottom: '14px' }}>
              <label htmlFor="evidence-type-select" style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                Evidence Type *
              </label>
              <select
                id="evidence-type-select"
                value={evidenceType}
                onChange={(e) => setEvidenceType(e.target.value as ResolutionEvidenceType)}
                style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
              >
                <option value="COMPLETION_PHOTO">Completion Photo (Work Finished)</option>
                <option value="BEFORE_AFTER_PHOTO">Before / After Evidence</option>
                <option value="COMPLETION_NOTE">Completion Note (Text Report)</option>
              </select>
            </div>

            {evidenceType !== 'COMPLETION_NOTE' && (
              <div style={{ marginBottom: '14px' }}>
                <label htmlFor="evidence-file-input" style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                  Photo File * (JPEG, PNG, WebP &le; 10MB)
                </label>
                <input
                  id="evidence-file-input"
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={(e) => setEvidenceFile(e.target.files?.[0] || null)}
                  style={{ width: '100%', fontSize: '13px', color: '#475569' }}
                />
              </div>
            )}

            <div style={{ marginBottom: '14px' }}>
              <label htmlFor="evidence-note-input" style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                Resolution Note {evidenceType === 'COMPLETION_NOTE' && <span style={{ color: '#dc2626' }}>* (Mandatory)</span>}
              </label>
              <textarea
                id="evidence-note-input"
                rows={3}
                maxLength={1000}
                placeholder="Describe actions taken, work completed, or materials used..."
                value={evidenceNote}
                onChange={(e) => setEvidenceNote(e.target.value)}
                style={{ width: '100%', padding: '8px 10px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', resize: 'vertical', boxSizing: 'border-box' }}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#94a3b8', marginTop: '4px' }}>
                <span>Max 1000 characters</span>
                <span>{evidenceNote.length}/1000</span>
              </div>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label htmlFor="evidence-captured-input" style={{ display: 'block', fontSize: '13px', fontWeight: '600', color: '#334155', marginBottom: '4px' }}>
                Captured Timestamp (Optional)
              </label>
              <input
                id="evidence-captured-input"
                type="datetime-local"
                value={capturedAt}
                onChange={(e) => setCapturedAt(e.target.value)}
                style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                type="button"
                disabled={uploadingEvidence}
                onClick={handleCloseEvidenceModal}
                style={{ padding: '8px 16px', background: '#f1f5f9', border: '1px solid #cbd5e1', borderRadius: '6px', cursor: 'pointer', fontSize: '13px', fontWeight: '500', color: '#475569' }}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={uploadingEvidence}
                style={{
                  padding: '8px 20px',
                  background: '#0284c7',
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  cursor: uploadingEvidence ? 'not-allowed' : 'pointer',
                  opacity: uploadingEvidence ? 0.6 : 1,
                  fontSize: '13px',
                  fontWeight: '600',
                }}
              >
                {uploadingEvidence ? 'Uploading...' : 'Upload Evidence'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
