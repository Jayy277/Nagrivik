'use client';

import React, { useState, useEffect } from 'react';
import { ImageAiAnalysis } from '../types/image-understanding';
import { getImageAnalysis, triggerImageAnalysis } from '../lib/api/image-understanding';

interface ImageAiAnalysisCardProps {
  issueId: string;
  mediaId: string;
  autoLoad?: boolean;
}

export default function ImageAiAnalysisCard({
  issueId,
  mediaId,
  autoLoad = true,
}: ImageAiAnalysisCardProps) {
  const [analysis, setAnalysis] = useState<ImageAiAnalysis | null>(null);
  const [loading, setLoading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!autoLoad) return;
    let isMounted = true;
    setLoading(true);

    getImageAnalysis(issueId, mediaId)
      .then((data) => {
        if (isMounted) {
          setAnalysis(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (isMounted) {
          setError(err?.message || 'Failed to fetch image analysis');
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [issueId, mediaId, autoLoad]);

  async function handleAnalyze() {
    try {
      setAnalyzing(true);
      setError(null);
      const res = await triggerImageAnalysis(issueId, mediaId);
      setAnalysis(res);
    } catch (err: any) {
      setError(err?.message || 'Failed to analyze image visual signals');
    } finally {
      setAnalyzing(false);
    }
  }

  const formatEnum = (str?: string | null) => {
    if (!str) return '—';
    return str.replace(/_/g, ' ');
  };

  return (
    <div
      style={{
        marginTop: '12px',
        padding: '14px',
        background: '#f8fafc',
        border: '1px solid #e2e8f0',
        borderRadius: '8px',
        fontSize: '13px',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '10px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span style={{ fontSize: '15px' }}>🤖</span>
          <span style={{ fontWeight: '700', color: '#0f172a' }}>
            AI-Assisted Image Observations
          </span>
          <span
            style={{
              fontSize: '10px',
              padding: '2px 6px',
              borderRadius: '4px',
              background: '#e0f2fe',
              color: '#0369a1',
              fontWeight: '600',
              textTransform: 'uppercase',
            }}
          >
            Advisory Only
          </span>
        </div>

        {analysis && (
          <span
            style={{
              fontSize: '11px',
              padding: '2px 8px',
              borderRadius: '9999px',
              fontWeight: '600',
              background:
                analysis.status === 'COMPLETED'
                  ? '#dcfce7'
                  : analysis.status === 'UNAVAILABLE'
                  ? '#f1f5f9'
                  : '#fee2e2',
              color:
                analysis.status === 'COMPLETED'
                  ? '#166534'
                  : analysis.status === 'UNAVAILABLE'
                  ? '#64748b'
                  : '#991b1b',
            }}
          >
            {analysis.status}
          </span>
        )}
      </div>

      {loading && (
        <div style={{ padding: '8px 0', color: '#64748b', fontSize: '12px' }}>
          Checking visual analysis signals...
        </div>
      )}

      {error && (
        <div
          style={{
            padding: '8px',
            background: '#fef2f2',
            border: '1px solid #fecaca',
            borderRadius: '6px',
            color: '#991b1b',
            fontSize: '12px',
            marginBottom: '8px',
          }}
        >
          {error}
        </div>
      )}

      {!loading && !analysis && (
        <div>
          <p style={{ color: '#64748b', fontSize: '12px', margin: '4px 0 10px 0' }}>
            No visual analysis on file for this photo.
          </p>
          <button
            type="button"
            onClick={handleAnalyze}
            disabled={analyzing}
            style={{
              padding: '6px 12px',
              background: '#0284c7',
              color: '#ffffff',
              border: 'none',
              borderRadius: '6px',
              fontSize: '12px',
              fontWeight: '600',
              cursor: analyzing ? 'not-allowed' : 'pointer',
              opacity: analyzing ? 0.7 : 1,
            }}
          >
            {analyzing ? 'Analyzing Image...' : 'Run Visual Understanding'}
          </button>
        </div>
      )}

      {analysis && analysis.status === 'UNAVAILABLE' && (
        <div style={{ color: '#64748b', fontSize: '12px' }}>
          Image AI understanding is disabled in server configuration. Issue workflow is unaffected.
        </div>
      )}

      {analysis && analysis.status === 'COMPLETED' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {/* Category & Confidence */}
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '8px' }}>
            <span style={{ color: '#475569', fontWeight: '500' }}>Likely Category:</span>
            <span
              style={{
                fontWeight: '700',
                color: '#0369a1',
                background: '#e0f2fe',
                padding: '2px 8px',
                borderRadius: '4px',
              }}
            >
              {formatEnum(analysis.likelyCategory)}
            </span>
            {analysis.categoryConfidence !== undefined && analysis.categoryConfidence !== null && (
              <span style={{ fontSize: '11px', color: '#64748b' }}>
                ({analysis.categoryConfidence}% visual confidence)
              </span>
            )}
          </div>

          {/* Observed Problems */}
          {analysis.visualProblemTypes && analysis.visualProblemTypes.length > 0 && (
            <div>
              <div style={{ color: '#475569', fontWeight: '500', marginBottom: '4px' }}>
                Detected Visual Problem Types:
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px' }}>
                {analysis.visualProblemTypes.map((pt) => (
                  <span
                    key={pt}
                    style={{
                      background: '#e2e8f0',
                      color: '#1e293b',
                      fontSize: '11px',
                      fontWeight: '600',
                      padding: '2px 6px',
                      borderRadius: '4px',
                    }}
                  >
                    {formatEnum(pt)}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Quality & Relevance */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', fontSize: '12px' }}>
            {analysis.imageQuality && (
              <div>
                <span style={{ color: '#64748b' }}>Quality: </span>
                <strong style={{ color: '#0f172a' }}>{analysis.imageQuality}</strong>
              </div>
            )}
            {analysis.relevance && (
              <div>
                <span style={{ color: '#64748b' }}>Civic Relevance: </span>
                <strong
                  style={{
                    color:
                      analysis.relevance === 'LIKELY_RELEVANT'
                        ? '#166534'
                        : analysis.relevance === 'UNCERTAIN'
                        ? '#ca8a04'
                        : '#991b1b',
                  }}
                >
                  {formatEnum(analysis.relevance)}
                </strong>
              </div>
            )}
            {analysis.safetyConcern && analysis.safetyConcern !== 'NONE' && (
              <div>
                <span style={{ color: '#64748b' }}>Safety Indicator: </span>
                <strong style={{ color: '#dc2626' }}>
                  {analysis.safetyConcern}
                </strong>
              </div>
            )}
          </div>

          {/* Severity Signals */}
          {analysis.visualSeveritySignals && analysis.visualSeveritySignals.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', alignItems: 'center' }}>
              <span style={{ fontSize: '11px', color: '#991b1b', fontWeight: '600' }}>
                ⚠️ Severity Signals:
              </span>
              {analysis.visualSeveritySignals.map((sig) => (
                <span
                  key={sig}
                  style={{
                    background: '#fee2e2',
                    color: '#991b1b',
                    fontSize: '11px',
                    fontWeight: '600',
                    padding: '2px 6px',
                    borderRadius: '4px',
                  }}
                >
                  {formatEnum(sig)}
                </span>
              ))}
            </div>
          )}

          {/* Summary */}
          {analysis.summary && (
            <p
              style={{
                margin: '4px 0 0 0',
                color: '#334155',
                fontSize: '12px',
                fontStyle: 'italic',
                lineHeight: '1.4',
              }}
            >
              &ldquo;{analysis.summary}&rdquo;
            </p>
          )}

          <div
            style={{
              fontSize: '10px',
              color: '#94a3b8',
              marginTop: '4px',
              borderTop: '1px dashed #cbd5e1',
              paddingTop: '6px',
            }}
          >
            Model: {analysis.model || 'heuristic-vision'} ({analysis.calculationVersion}) • Purely advisory; human verification is authoritative.
          </div>
        </div>
      )}
    </div>
  );
}
