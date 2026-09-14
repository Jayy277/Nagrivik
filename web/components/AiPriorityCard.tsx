'use client';

import React, { useState, useEffect } from 'react';
import { AiPriorityRecommendation } from '../types/priority';
import { getAiPriorityRecommendation, triggerAiPriorityAssessment } from '../lib/api/priority';

interface AiPriorityCardProps {
  issueId: string;
  autoLoad?: boolean;
  canReassess?: boolean;
  onReassessed?: () => void;
}

export default function AiPriorityCard({
  issueId,
  autoLoad = true,
  canReassess = false,
  onReassessed,
}: AiPriorityCardProps) {
  const [recommendation, setRecommendation] = useState<AiPriorityRecommendation | null>(null);
  const [loading, setLoading] = useState(false);
  const [assessing, setAssessing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!autoLoad) return;
    let isMounted = true;
    setLoading(true);

    getAiPriorityRecommendation(issueId)
      .then((data) => {
        if (isMounted) {
          setRecommendation(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err?.message || 'Failed to load priority recommendation');
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [issueId, autoLoad]);

  async function handleAssess() {
    try {
      setAssessing(true);
      setError(null);
      const res = await triggerAiPriorityAssessment(issueId);
      setRecommendation(res);
      if (onReassessed) {
        onReassessed();
      }
    } catch (err: any) {
      setError(err?.message || 'Failed to trigger AI priority assessment');
    } finally {
      setAssessing(false);
    }
  }

  return (
    <div
      style={{
        background: '#ffffff',
        borderRadius: '12px',
        border: '1px solid #e2e8f0',
        padding: '20px',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.04)',
      }}
    >
      {/* Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '12px',
          flexWrap: 'wrap',
          gap: '8px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span style={{ fontSize: '18px' }}>⚡</span>
          <h3 style={{ fontSize: '15px', fontWeight: '700', color: '#0f172a', margin: 0 }}>
            AI Priority Assistance
          </h3>
          <span
            style={{
              fontSize: '10px',
              padding: '2px 6px',
              borderRadius: '4px',
              background: '#fef3c7',
              color: '#92400e',
              fontWeight: '700',
              textTransform: 'uppercase',
              letterSpacing: '0.5px',
            }}
          >
            Advisory Signal Only
          </span>
        </div>

        {recommendation && (
          <span
            style={{
              fontSize: '11px',
              padding: '2px 8px',
              borderRadius: '9999px',
              fontWeight: '600',
              background:
                recommendation.status === 'COMPLETED'
                  ? '#dcfce7'
                  : recommendation.status === 'UNAVAILABLE'
                  ? '#f1f5f9'
                  : '#fee2e2',
              color:
                recommendation.status === 'COMPLETED'
                  ? '#166534'
                  : recommendation.status === 'UNAVAILABLE'
                  ? '#64748b'
                  : '#991b1b',
            }}
          >
            {recommendation.status}
          </span>
        )}
      </div>

      <p style={{ fontSize: '12px', color: '#64748b', margin: '0 0 14px 0', lineHeight: '1.4' }}>
        AI provides bounded recommendations for severity, impact, and safety. Authoritative priority calculation remains strictly deterministic.
      </p>

      {loading && (
        <div style={{ padding: '12px 0', color: '#64748b', fontSize: '13px', textAlign: 'center' }}>
          Loading priority assessment signals...
        </div>
      )}

      {error && (
        <div
          style={{
            padding: '10px 14px',
            background: '#fef2f2',
            border: '1px solid #fecaca',
            borderRadius: '6px',
            color: '#991b1b',
            fontSize: '12px',
            marginBottom: '12px',
          }}
        >
          {error}
        </div>
      )}

      {!loading && !recommendation && (
        <div>
          <p style={{ color: '#64748b', fontSize: '13px', margin: '0 0 12px 0' }}>
            No AI priority recommendation recorded yet for this issue.
          </p>
          {canReassess && (
            <button
              type="button"
              onClick={handleAssess}
              disabled={assessing}
              style={{
                padding: '7px 14px',
                background: '#0284c7',
                color: '#ffffff',
                border: 'none',
                borderRadius: '6px',
                fontSize: '12px',
                fontWeight: '600',
                cursor: assessing ? 'not-allowed' : 'pointer',
                opacity: assessing ? 0.7 : 1,
              }}
            >
              {assessing ? 'Assessing Priority...' : 'Generate AI Assessment'}
            </button>
          )}
        </div>
      )}

      {recommendation && recommendation.status === 'UNAVAILABLE' && (
        <div style={{ padding: '12px', background: '#f8fafc', borderRadius: '8px', color: '#64748b', fontSize: '12px' }}>
          AI priority assistance is disabled in server configuration. Deterministic priority engine is operating normally.
        </div>
      )}

      {recommendation && recommendation.status === 'COMPLETED' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {/* Bounded Component Suggestions */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '10px' }}>
            <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '10px' }}>
              <div style={{ fontSize: '11px', color: '#64748b', fontWeight: '500' }}>Suggested Severity</div>
              <div style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', margin: '2px 0' }}>
                {recommendation.suggestedSeverity ?? '—'}<span style={{ fontSize: '12px', color: '#94a3b8' }}> / 30</span>
              </div>
              <div style={{ fontSize: '10px', color: '#64748b' }}>
                {recommendation.severityConfidence ? `${recommendation.severityConfidence}% confidence` : 'Bounded signal'}
              </div>
            </div>

            <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '10px' }}>
              <div style={{ fontSize: '11px', color: '#64748b', fontWeight: '500' }}>Suggested Impact</div>
              <div style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', margin: '2px 0' }}>
                {recommendation.suggestedImpact ?? '—'}<span style={{ fontSize: '12px', color: '#94a3b8' }}> / 25</span>
              </div>
              <div style={{ fontSize: '10px', color: '#64748b' }}>
                {recommendation.impactConfidence ? `${recommendation.impactConfidence}% confidence` : 'Bounded signal'}
              </div>
            </div>

            <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '10px' }}>
              <div style={{ fontSize: '11px', color: '#64748b', fontWeight: '500' }}>Suggested Safety</div>
              <div style={{ fontSize: '18px', fontWeight: '700', color: '#0f172a', margin: '2px 0' }}>
                {recommendation.suggestedSafety ?? '—'}<span style={{ fontSize: '12px', color: '#94a3b8' }}> / 25</span>
              </div>
              <div style={{ fontSize: '10px', color: '#64748b' }}>
                {recommendation.safetyConfidence ? `${recommendation.safetyConfidence}% confidence` : 'Bounded signal'}
              </div>
            </div>
          </div>

          {/* Overall Confidence Meter */}
          {recommendation.confidence !== null && recommendation.confidence !== undefined && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', marginBottom: '4px' }}>
                <span style={{ color: '#475569', fontWeight: '600' }}>AI Confidence Rating</span>
                <span style={{ color: '#0284c7', fontWeight: '700' }}>{recommendation.confidence}%</span>
              </div>
              <div style={{ width: '100%', height: '6px', background: '#e2e8f0', borderRadius: '3px', overflow: 'hidden' }}>
                <div
                  style={{
                    width: `${Math.min(100, Math.max(0, recommendation.confidence))}%`,
                    height: '100%',
                    background:
                      recommendation.confidence >= 80
                        ? '#0284c7'
                        : recommendation.confidence >= 60
                        ? '#ca8a04'
                        : '#94a3b8',
                    borderRadius: '3px',
                  }}
                />
              </div>
            </div>
          )}

          {/* Explainable Signals */}
          {recommendation.signals && recommendation.signals.length > 0 && (
            <div>
              <div style={{ fontSize: '11px', color: '#475569', fontWeight: '600', marginBottom: '6px' }}>
                Explainable Assessment Factors:
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                {recommendation.signals.map((sig, idx) => (
                  <span
                    key={idx}
                    style={{
                      background: '#eff6ff',
                      color: '#1e40af',
                      fontSize: '11px',
                      padding: '3px 8px',
                      borderRadius: '4px',
                      border: '1px solid #dbeafe',
                    }}
                  >
                    • {sig}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Action Footer */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              borderTop: '1px dashed #e2e8f0',
              paddingTop: '10px',
              fontSize: '11px',
              color: '#94a3b8',
              flexWrap: 'wrap',
              gap: '8px',
            }}
          >
            <span>
              Model: {recommendation.model} ({recommendation.calculationVersion})
            </span>
            {canReassess && (
              <button
                type="button"
                onClick={handleAssess}
                disabled={assessing}
                style={{
                  padding: '4px 10px',
                  background: '#f1f5f9',
                  color: '#334155',
                  border: '1px solid #cbd5e1',
                  borderRadius: '4px',
                  fontSize: '11px',
                  fontWeight: '600',
                  cursor: assessing ? 'not-allowed' : 'pointer',
                }}
              >
                {assessing ? 'Re-assessing...' : '↻ Re-assess'}
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
