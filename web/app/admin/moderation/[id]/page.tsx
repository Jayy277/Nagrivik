'use client';

import React, { use, useEffect, useState, useCallback } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import { useAuth } from '../../../../lib/context/AuthContext';
import {
  getModerationReportDetail,
  reviewReport,
  resolveReport,
  dismissReport,
  hideContent,
  restoreContent,
  restrictUser,
} from '../../../../lib/api/moderation';
import {
  ModerationReportDetail,
  ModerationActionType,
  MODERATION_REASON_LABELS,
} from '../../../../types/moderation';

interface ActionModalConfig {
  isOpen: boolean;
  action: ModerationActionType | 'DISMISS_ONLY';
  title: string;
  description: string;
  confirmLabel: string;
  confirmColor: string;
  requireReason: boolean;
  isRestriction?: boolean;
}

export default function ModerationReportDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const { user } = useAuth();

  const [detail, setDetail] = useState<ModerationReportDetail | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [conflictError, setConflictError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Modal State
  const [modalConfig, setModalConfig] = useState<ActionModalConfig>({
    isOpen: false,
    action: 'NO_ACTION',
    title: '',
    description: '',
    confirmLabel: 'Confirm',
    confirmColor: '#1e40af',
    requireReason: true,
  });
  const [actionReason, setActionReason] = useState<string>('');
  const [actionNotes, setActionNotes] = useState<string>('');
  const [restrictionMinutes, setRestrictionMinutes] = useState<number>(1440); // 24 hours default
  const [isSubmittingAction, setIsSubmittingAction] = useState<boolean>(false);

  const fetchDetail = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    setConflictError(null);
    try {
      const res = await getModerationReportDetail(id);
      setDetail(res);
    } catch (err: any) {
      setError(err.message || 'Unable to load report detail.');
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    fetchDetail();
  }, [fetchDetail]);

  const handleMarkInReview = async () => {
    setIsSubmittingAction(true);
    try {
      await reviewReport(id);
      setActionSuccess('Report marked as In Review.');
      await fetchDetail();
    } catch (err: any) {
      if (err.message?.includes('CONFLICT')) {
        setConflictError('Conflict: This report has already been resolved or modified by another moderator.');
      } else {
        setError(err.message || 'Failed to update report state.');
      }
    } finally {
      setIsSubmittingAction(false);
    }
  };

  const openActionModal = (
    action: ModerationActionType | 'DISMISS_ONLY',
    title: string,
    description: string,
    confirmLabel: string,
    confirmColor: string,
    isRestriction = false
  ) => {
    setActionReason('');
    setActionNotes('');
    setConflictError(null);
    setModalConfig({
      isOpen: true,
      action,
      title,
      description,
      confirmLabel,
      confirmColor,
      requireReason: true,
      isRestriction,
    });
  };

  const handleConfirmAction = async () => {
    if (!actionReason.trim()) {
      return;
    }

    setIsSubmittingAction(true);
    try {
      if (modalConfig.action === 'DISMISS_ONLY') {
        await dismissReport(id, actionReason.trim());
        setActionSuccess('Report dismissed.');
      } else if (modalConfig.isRestriction && detail?.issueTarget?.reporter?.id) {
        await restrictUser(
          detail.issueTarget.reporter.id,
          restrictionMinutes,
          actionReason.trim()
        );
        setActionSuccess('User restriction applied successfully.');
      } else if (modalConfig.isRestriction && detail?.commentTarget?.author?.id) {
        await restrictUser(
          detail.commentTarget.author.id,
          restrictionMinutes,
          actionReason.trim()
        );
        setActionSuccess('User restriction applied successfully.');
      } else {
        await resolveReport(
          id,
          modalConfig.action as ModerationActionType,
          actionReason.trim(),
          actionNotes.trim() || undefined
        );
        setActionSuccess(`Moderation action applied: ${modalConfig.action}.`);
      }

      setModalConfig((prev) => ({ ...prev, isOpen: false }));
      await fetchDetail();
    } catch (err: any) {
      if (err.message?.includes('CONFLICT')) {
        setConflictError('Conflict: Another moderator has already resolved or dismissed this report.');
        setModalConfig((prev) => ({ ...prev, isOpen: false }));
      } else {
        setError(err.message || 'Failed to execute moderation action.');
      }
    } finally {
      setIsSubmittingAction(false);
    }
  };

  if (isLoading) {
    return (
      <div style={{ padding: '80px 20px', textAlign: 'center', color: '#64748b' }}>
        <div style={{ width: '40px', height: '40px', border: '3px solid #e2e8f0', borderTopColor: '#1e40af', borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px' }} />
        <p style={{ fontSize: '15px' }}>Loading moderation report details...</p>
      </div>
    );
  }

  if (error && !detail) {
    return (
      <div style={{ maxWidth: '600px', margin: '60px auto', textAlign: 'center', padding: '32px', background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0' }}>
        <h2 style={{ fontSize: '20px', fontWeight: '700', color: '#991b1b', marginBottom: '8px' }}>Report Not Available</h2>
        <p style={{ color: '#64748b', fontSize: '14px', marginBottom: '24px' }}>{error}</p>
        <Link href="/admin/moderation" style={{ padding: '8px 18px', background: '#1e40af', color: '#ffffff', borderRadius: '6px', textDecoration: 'none', fontWeight: '600', fontSize: '13px' }}>
          ← Back to Queue
        </Link>
      </div>
    );
  }

  if (!detail) return null;

  const isResolved = detail.status === 'RESOLVED' || detail.status === 'DISMISSED';
  const isAdmin = user?.role === 'ADMIN';

  return (
    <div>
      {/* Top Breadcrumb & Status Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div>
          <Link href="/admin/moderation" style={{ fontSize: '13px', color: '#1e40af', textDecoration: 'none', fontWeight: '600', display: 'inline-flex', alignItems: 'center', gap: '4px', marginBottom: '8px' }}>
            ← Back to Queue
          </Link>
          <h1 style={{ fontSize: '24px', fontWeight: '800', color: '#0f172a', letterSpacing: '-0.02em', display: 'flex', alignItems: 'center', gap: '12px' }}>
            Report #{detail.id.substring(0, 8)}
            <span
              style={{
                fontSize: '12px',
                fontWeight: '700',
                padding: '3px 10px',
                borderRadius: '9999px',
                background:
                  detail.status === 'OPEN'
                    ? '#fef3c7'
                    : detail.status === 'IN_REVIEW'
                    ? '#e0e7ff'
                    : detail.status === 'RESOLVED'
                    ? '#d1fae5'
                    : '#f1f5f9',
                color:
                  detail.status === 'OPEN'
                    ? '#92400e'
                    : detail.status === 'IN_REVIEW'
                    ? '#3730a3'
                    : detail.status === 'RESOLVED'
                    ? '#065f46'
                    : '#475569',
              }}
            >
              {detail.status.replace('_', ' ')}
            </span>
          </h1>
        </div>

        {/* Status transition controls */}
        {detail.status === 'OPEN' && (
          <button
            onClick={handleMarkInReview}
            disabled={isSubmittingAction}
            style={{
              padding: '10px 18px',
              background: '#4f46e5',
              color: '#ffffff',
              border: 'none',
              borderRadius: '8px',
              fontWeight: '600',
              fontSize: '13px',
              cursor: isSubmittingAction ? 'not-allowed' : 'pointer',
            }}
          >
            {isSubmittingAction ? 'Updating...' : 'Mark In Review'}
          </button>
        )}
      </div>

      {/* Conflict / Alert Banners */}
      {conflictError && (
        <div style={{ padding: '16px 20px', background: '#fffbeb', border: '1px solid #fde68a', borderRadius: '10px', marginBottom: '24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', color: '#92400e', fontSize: '14px' }}>
            <span style={{ fontSize: '20px' }}>⚠️</span>
            <span>{conflictError}</span>
          </div>
          <button
            onClick={fetchDetail}
            style={{ padding: '6px 14px', background: '#d97706', color: '#ffffff', border: 'none', borderRadius: '6px', fontSize: '13px', fontWeight: '600', cursor: 'pointer' }}
          >
            Reload Latest State
          </button>
        </div>
      )}

      {actionSuccess && (
        <div style={{ padding: '14px 20px', background: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: '8px', color: '#065f46', marginBottom: '24px', fontSize: '14px' }}>
          ✓ {actionSuccess}
        </div>
      )}

      {/* Main Grid: Content Review (Left) + Actions/Info (Right) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 2fr) minmax(320px, 1fr)', gap: '28px' }}>
        {/* Left Column: Reported Content Review */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Content Preview Box */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', paddingBottom: '12px', borderBottom: '1px solid #f1f5f9' }}>
              <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a' }}>
                Reported {detail.targetType === 'ISSUE' ? 'Civic Issue' : 'Citizen Comment'}
              </h2>
              <span
                style={{
                  fontSize: '12px',
                  fontWeight: '700',
                  padding: '3px 8px',
                  borderRadius: '4px',
                  background: detail.targetType === 'ISSUE' ? '#e0f2fe' : '#ede9fe',
                  color: detail.targetType === 'ISSUE' ? '#0369a1' : '#6d28d9',
                }}
              >
                {detail.targetType} TARGET
              </span>
            </div>

            {/* Target: Issue */}
            {detail.targetType === 'ISSUE' && detail.issueTarget && (
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '10px' }}>
                  {detail.issueTarget.title}
                </h3>
                <p style={{ color: '#334155', fontSize: '14px', lineHeight: '1.7', whiteSpace: 'pre-line', marginBottom: '20px' }}>
                  {detail.issueTarget.description}
                </p>

                {/* Issue Metadata Badges */}
                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '20px', fontSize: '12px' }}>
                  {detail.issueTarget.categoryName && (
                    <span style={{ padding: '4px 10px', background: '#f1f5f9', borderRadius: '6px', color: '#475569', fontWeight: '600' }}>
                      Category: {detail.issueTarget.categoryName}
                    </span>
                  )}
                  {detail.issueTarget.status && (
                    <span style={{ padding: '4px 10px', background: '#eff6ff', borderRadius: '6px', color: '#1e40af', fontWeight: '600' }}>
                      Civic Status: {detail.issueTarget.status}
                    </span>
                  )}
                  {detail.issueTarget.priority && (
                    <span style={{ padding: '4px 10px', background: '#fef2f2', borderRadius: '6px', color: '#dc2626', fontWeight: '600' }}>
                      Priority: {detail.issueTarget.priority}
                    </span>
                  )}
                  <span
                    style={{
                      padding: '4px 10px',
                      borderRadius: '6px',
                      fontWeight: '700',
                      background: detail.issueTarget.moderationStatus === 'HIDDEN' ? '#fee2e2' : '#f0fdf4',
                      color: detail.issueTarget.moderationStatus === 'HIDDEN' ? '#991b1b' : '#166534',
                    }}
                  >
                    Visibility: {detail.issueTarget.moderationStatus}
                  </span>
                </div>

                {/* Civic Responsibility */}
                {detail.issueTarget.responsibility && (
                  <div style={{ background: '#f8fafc', padding: '14px', borderRadius: '8px', marginBottom: '20px', fontSize: '13px', color: '#475569' }}>
                    <strong>Assigned Responsibility:</strong>{' '}
                    {[
                      detail.issueTarget.responsibility.civicBodyName,
                      detail.issueTarget.responsibility.wardName,
                      detail.issueTarget.responsibility.departmentName,
                    ]
                      .filter(Boolean)
                      .join(' • ') || 'Unassigned'}
                  </div>
                )}

                {/* Media Attachments Preview (Non-autoplay, safe display) */}
                {detail.issueTarget.mediaUrls && detail.issueTarget.mediaUrls.length > 0 && (
                  <div>
                    <strong style={{ display: 'block', fontSize: '13px', color: '#475569', marginBottom: '8px' }}>
                      Attached Evidence ({detail.issueTarget.mediaUrls.length}):
                    </strong>
                    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                      {detail.issueTarget.mediaUrls.map((url, idx) => (
                        <div
                          key={idx}
                          style={{
                            width: '120px',
                            height: '90px',
                            borderRadius: '8px',
                            border: '1px solid #cbd5e1',
                            overflow: 'hidden',
                            position: 'relative',
                            background: '#0f172a',
                          }}
                        >
                          <Image
                            src={url}
                            alt={`Evidence thumbnail ${idx + 1}`}
                            fill
                            sizes="120px"
                            style={{ objectFit: 'cover' }}
                            unoptimized
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Target: Comment */}
            {detail.targetType === 'COMMENT' && detail.commentTarget && (
              <div>
                <div
                  style={{
                    background: '#f8fafc',
                    padding: '16px',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    fontSize: '15px',
                    color: '#0f172a',
                    fontStyle: 'italic',
                    marginBottom: '16px',
                  }}
                >
                  &ldquo;{detail.commentTarget.content}&rdquo;
                </div>

                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', fontSize: '12px' }}>
                  {detail.commentTarget.issueId && (
                    <span style={{ padding: '4px 10px', background: '#f1f5f9', borderRadius: '6px', color: '#475569' }}>
                      On Issue: <strong>{detail.commentTarget.issueTitle || detail.commentTarget.issueId.substring(0, 8)}</strong>
                    </span>
                  )}
                  {detail.commentTarget.isDeleted && (
                    <span style={{ padding: '4px 10px', background: '#fee2e2', borderRadius: '6px', color: '#dc2626', fontWeight: '700' }}>
                      Comment Soft-Deleted
                    </span>
                  )}
                  <span
                    style={{
                      padding: '4px 10px',
                      borderRadius: '6px',
                      fontWeight: '700',
                      background: detail.commentTarget.moderationStatus === 'HIDDEN' ? '#fee2e2' : '#f0fdf4',
                      color: detail.commentTarget.moderationStatus === 'HIDDEN' ? '#991b1b' : '#166534',
                    }}
                  >
                    Visibility: {detail.commentTarget.moderationStatus}
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Section E: Audit History */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginBottom: '16px' }}>
              Moderation Audit History ({detail.actionHistory.length})
            </h2>

            {detail.actionHistory.length === 0 ? (
              <p style={{ color: '#94a3b8', fontSize: '13px' }}>No prior moderation actions recorded for this content.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {detail.actionHistory.map((rec) => (
                  <div key={rec.id} style={{ padding: '12px 16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '13px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                      <span style={{ fontWeight: '700', color: '#1e40af' }}>{rec.action}</span>
                      <span style={{ color: '#94a3b8', fontSize: '12px' }}>
                        {new Date(rec.createdAt).toLocaleString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                    <div style={{ color: '#334155', marginBottom: '2px' }}>
                      <strong>Reason:</strong> {rec.reason}
                    </div>
                    {rec.notes && <div style={{ color: '#64748b', fontSize: '12px' }}>Notes: {rec.notes}</div>}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Report Metadata & Actions */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Report Information Card */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginBottom: '16px', borderBottom: '1px solid #f1f5f9', paddingBottom: '10px' }}>
              Report Details
            </h2>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', fontSize: '13px' }}>
              <div>
                <span style={{ color: '#64748b', display: 'block', marginBottom: '2px' }}>Reported Reason</span>
                <strong style={{ color: '#0f172a', fontSize: '14px' }}>
                  {MODERATION_REASON_LABELS[detail.reason] || detail.reason}
                </strong>
              </div>

              {detail.description && (
                <div>
                  <span style={{ color: '#64748b', display: 'block', marginBottom: '2px' }}>Reporter Explanation</span>
                  <div style={{ background: '#f8fafc', padding: '10px', borderRadius: '6px', color: '#334155' }}>
                    {detail.description}
                  </div>
                </div>
              )}

              <div>
                <span style={{ color: '#64748b', display: 'block', marginBottom: '2px' }}>Reporter Identity</span>
                <span style={{ color: '#0f172a', fontWeight: '600' }}>{detail.reporter?.fullName || 'Anonymous Citizen'}</span>
              </div>

              <div>
                <span style={{ color: '#64748b', display: 'block', marginBottom: '2px' }}>Submitted On</span>
                <span style={{ color: '#0f172a' }}>
                  {new Date(detail.createdAt).toLocaleString(undefined, {
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>
              </div>

              {detail.resolvedAt && (
                <div>
                  <span style={{ color: '#64748b', display: 'block', marginBottom: '2px' }}>Resolution Details</span>
                  <span style={{ color: '#0f172a' }}>
                    Resolved on {new Date(detail.resolvedAt).toLocaleDateString()}
                    {detail.resolvedBy && ` by ${detail.resolvedBy.fullName}`}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Section C: Available Actions Panel */}
          <div style={{ background: '#ffffff', borderRadius: '12px', border: '1px solid #e2e8f0', padding: '24px', boxShadow: '0 1px 3px rgba(0,0,0,0.05)' }}>
            <h2 style={{ fontSize: '16px', fontWeight: '700', color: '#0f172a', marginBottom: '6px' }}>
              Moderation Actions
            </h2>
            <p style={{ color: '#64748b', fontSize: '13px', marginBottom: '16px' }}>
              All actions update the report and record an immutable audit entry.
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {/* Dismiss Action */}
              {!isResolved && (
                <button
                  onClick={() =>
                    openActionModal(
                      'DISMISS_ONLY',
                      'Dismiss Report (No Violation)',
                      'The reported content will remain visible. The report will be marked as DISMISSED with a NO_ACTION audit entry.',
                      'Dismiss Report',
                      '#64748b'
                    )
                  }
                  disabled={isSubmittingAction}
                  style={{
                    padding: '10px 14px',
                    background: '#f8fafc',
                    color: '#475569',
                    border: '1px solid #cbd5e1',
                    borderRadius: '8px',
                    fontWeight: '600',
                    fontSize: '13px',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  Dismiss Report (No Violation)
                </button>
              )}

              {/* Hide Content Action */}
              <button
                onClick={() =>
                  openActionModal(
                    'HIDE_CONTENT',
                    'Hide Reported Content',
                    'The content will be hidden from all public citizen APIs and search feeds. The record is preserved for administrative audit.',
                    'Hide Content',
                    '#b45309'
                  )
                }
                disabled={isSubmittingAction}
                style={{
                  padding: '10px 14px',
                  background: '#fef3c7',
                  color: '#92400e',
                  border: '1px solid #fde68a',
                  borderRadius: '8px',
                  fontWeight: '600',
                  fontSize: '13px',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                Hide Content from Public
              </button>

              {/* Restore Content Action */}
              <button
                onClick={() =>
                  openActionModal(
                    'RESTORE_CONTENT',
                    'Restore Content Visibility',
                    'The content will be restored back to VISIBLE standing on public feeds.',
                    'Restore Visibility',
                    '#059669'
                  )
                }
                disabled={isSubmittingAction}
                style={{
                  padding: '10px 14px',
                  background: '#ecfdf5',
                  color: '#065f46',
                  border: '1px solid #a7f3d0',
                  borderRadius: '8px',
                  fontWeight: '600',
                  fontSize: '13px',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                Restore Content Visibility
              </button>

              {/* Remove Comment Action (Only for Comment targets) */}
              {detail.targetType === 'COMMENT' && (
                <button
                  onClick={() =>
                    openActionModal(
                      'REMOVE_COMMENT',
                      'Remove Comment (Soft-Delete)',
                      'The comment will be soft-deleted and permanently hidden from civic discussions while preserving audit history.',
                      'Remove Comment',
                      '#dc2626'
                    )
                  }
                  disabled={isSubmittingAction}
                  style={{
                    padding: '10px 14px',
                    background: '#fef2f2',
                    color: '#991b1b',
                    border: '1px solid #fecaca',
                    borderRadius: '8px',
                    fontWeight: '600',
                    fontSize: '13px',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  Remove Comment (Soft-Delete)
                </button>
              )}

              {/* Restrict User Action (Only for ADMIN role) */}
              {isAdmin && (
                <button
                  onClick={() =>
                    openActionModal(
                      'RESTRICT_USER',
                      'Restrict User Account',
                      'Temporarily or indefinitely prevent this user from posting new civic issues, comments, or support contributions.',
                      'Apply Restriction',
                      '#7f1d1d',
                      true
                    )
                  }
                  disabled={isSubmittingAction}
                  style={{
                    padding: '10px 14px',
                    background: '#450a0a',
                    color: '#fecaca',
                    border: 'none',
                    borderRadius: '8px',
                    fontWeight: '600',
                    fontSize: '13px',
                    cursor: 'pointer',
                    textAlign: 'left',
                    marginTop: '8px',
                  }}
                >
                  Restrict User Account (Admin Only)
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Confirmation Modal */}
      {modalConfig.isOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-title"
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(15, 23, 42, 0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            zIndex: 9999,
          }}
        >
          <div
            style={{
              background: '#ffffff',
              borderRadius: '12px',
              maxWidth: '500px',
              width: '100%',
              padding: '24px',
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)',
            }}
          >
            <h3 id="modal-title" style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', marginBottom: '8px' }}>
              {modalConfig.title}
            </h3>
            <p style={{ color: '#475569', fontSize: '14px', lineHeight: '1.6', marginBottom: '20px' }}>
              {modalConfig.description}
            </p>

            {/* Restriction Duration selection */}
            {modalConfig.isRestriction && (
              <div style={{ marginBottom: '16px' }}>
                <label htmlFor="modal-duration" style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#334155', marginBottom: '4px' }}>
                  Restriction Duration
                </label>
                <select
                  id="modal-duration"
                  value={restrictionMinutes}
                  onChange={(e) => setRestrictionMinutes(Number(e.target.value))}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px' }}
                >
                  <option value={60}>1 Hour</option>
                  <option value={1440}>24 Hours (1 Day)</option>
                  <option value={10080}>7 Days</option>
                  <option value={43200}>30 Days</option>
                </select>
              </div>
            )}

            {/* Moderation Reason input */}
            <div style={{ marginBottom: '16px' }}>
              <label htmlFor="modal-reason" style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#334155', marginBottom: '4px' }}>
                Moderation Reason (Required for Audit) *
              </label>
              <input
                id="modal-reason"
                type="text"
                placeholder="e.g. Verified commercial spam violation"
                value={actionReason}
                onChange={(e) => setActionReason(e.target.value)}
                maxLength={255}
                style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>

            {/* Internal notes input */}
            <div style={{ marginBottom: '24px' }}>
              <label htmlFor="modal-notes" style={{ display: 'block', fontSize: '12px', fontWeight: '700', color: '#334155', marginBottom: '4px' }}>
                Internal Notes (Optional)
              </label>
              <textarea
                id="modal-notes"
                placeholder="Additional context for other moderators"
                value={actionNotes}
                onChange={(e) => setActionNotes(e.target.value)}
                rows={3}
                style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '13px', boxSizing: 'border-box' }}
              />
            </div>

            {/* Buttons */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
              <button
                type="button"
                onClick={() => setModalConfig((prev) => ({ ...prev, isOpen: false }))}
                disabled={isSubmittingAction}
                style={{
                  padding: '10px 18px',
                  background: '#f1f5f9',
                  color: '#475569',
                  border: '1px solid #cbd5e1',
                  borderRadius: '6px',
                  fontSize: '13px',
                  fontWeight: '600',
                  cursor: 'pointer',
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmAction}
                disabled={isSubmittingAction || !actionReason.trim()}
                style={{
                  padding: '10px 18px',
                  background: !actionReason.trim() ? '#94a3b8' : modalConfig.confirmColor,
                  color: '#ffffff',
                  border: 'none',
                  borderRadius: '6px',
                  fontSize: '13px',
                  fontWeight: '600',
                  cursor: isSubmittingAction || !actionReason.trim() ? 'not-allowed' : 'pointer',
                }}
              >
                {isSubmittingAction ? 'Processing...' : modalConfig.confirmLabel}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
