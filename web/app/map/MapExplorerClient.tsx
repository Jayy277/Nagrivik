'use client';

import React, { useState, useEffect, useCallback, useRef } from 'react';
import dynamic from 'next/dynamic';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { IssueResponse, CategorySummary } from '../../types/issue';
import { getIssues } from '../../lib/api/issues';
import { IssueCard } from '../../components/IssueCard';
import { LoadingSpinner, EmptyState, ErrorState } from '../../components/FeedbackStates';

// Dynamic import of Leaflet CivicMap to prevent SSR document/window errors
const CivicMap = dynamic(
  () => import('../../components/map/CivicMap').then((mod) => mod.CivicMap),
  {
    ssr: false,
    loading: () => (
      <div className="map-loading-placeholder">
        <LoadingSpinner />
        <p>Initializing interactive civic map...</p>
      </div>
    ),
  }
);

interface MapExplorerClientProps {
  initialIssues: IssueResponse[];
  categories: CategorySummary[];
  defaultCenter: { latitude: number; longitude: number };
  initialRadius: number;
  initialFilters: {
    category?: string;
    status?: string;
    priority?: string;
    search?: string;
  };
}

export default function MapExplorerClient({
  initialIssues,
  categories,
  defaultCenter,
  initialRadius,
  initialFilters,
}: MapExplorerClientProps) {
  const router = useRouter();

  // Geographic & Issues State
  const [center, setCenter] = useState(defaultCenter);
  const [radiusMeters, setRadiusMeters] = useState(initialRadius);
  const [issues, setIssues] = useState<IssueResponse[]>(initialIssues);
  const [selectedIssue, setSelectedIssue] = useState<IssueResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Filters State
  const [selectedCategory, setSelectedCategory] = useState(initialFilters.category || '');
  const [selectedStatus, setSelectedStatus] = useState(initialFilters.status || '');
  const [selectedPriority, setSelectedPriority] = useState(initialFilters.priority || '');
  const [searchQuery, setSearchQuery] = useState(initialFilters.search || '');

  // Debounced search input
  const [searchInput, setSearchInput] = useState(initialFilters.search || '');

  // Stale Request Protection
  const requestIdRef = useRef<number>(0);
  const abortControllerRef = useRef<AbortController | null>(null);

  // Sync filters to URL using replaceState (avoids polluting browser history)
  const syncUrlParams = useCallback(() => {
    const params = new URLSearchParams();
    if (selectedCategory) params.set('category', selectedCategory);
    if (selectedStatus) params.set('status', selectedStatus);
    if (selectedPriority) params.set('priority', selectedPriority);
    if (searchQuery) params.set('search', searchQuery);

    const queryString = params.toString();
    const newUrl = queryString ? `/map?${queryString}` : '/map';
    window.history.replaceState({}, '', newUrl);
  }, [selectedCategory, selectedStatus, selectedPriority, searchQuery]);

  // Fetch issues for current viewport and filters
  const fetchMapIssues = useCallback(
    async (lat: number, lng: number, rad: number) => {
      const currentReqId = ++requestIdRef.current;

      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
      const controller = new AbortController();
      abortControllerRef.current = controller;

      setLoading(true);
      setError(null);

      try {
        const res = await getIssues(
          {
            latitude: lat,
            longitude: lng,
            radiusMeters: rad,
            categoryId: selectedCategory || undefined,
            status: (selectedStatus as any) || undefined,
            priority: (selectedPriority as any) || undefined,
            q: searchQuery || undefined,
            sort: 'NEAREST',
            page: 0,
            size: 50,
          },
          controller.signal
        );

        if (currentReqId === requestIdRef.current) {
          setIssues(res.content || []);
        }
      } catch (err: any) {
        if (err?.name !== 'AbortError' && currentReqId === requestIdRef.current) {
          setError(err?.message || 'Failed to load issues for this geographic area.');
        }
      } finally {
        if (currentReqId === requestIdRef.current) {
          setLoading(false);
        }
      }
    },
    [selectedCategory, selectedStatus, selectedPriority, searchQuery]
  );

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setSearchQuery(searchInput.trim());
    }, 400);
    return () => clearTimeout(timer);
  }, [searchInput]);

  // Re-fetch when filters change or center/radius changes
  useEffect(() => {
    syncUrlParams();
    fetchMapIssues(center.latitude, center.longitude, radiusMeters);
  }, [center.latitude, center.longitude, radiusMeters, selectedCategory, selectedStatus, selectedPriority, searchQuery, syncUrlParams, fetchMapIssues]);

  // Handle map movement from CivicMap
  const handleViewportChange = useCallback((lat: number, lng: number, rad: number) => {
    setCenter({ latitude: lat, longitude: lng });
    setRadiusMeters(rad);
  }, []);

  return (
    <div className="map-explorer-layout">
      {/* MAP VIEW CONTAINER (MAIN) */}
      <div className="map-explorer-main">
        <CivicMap
          issues={issues}
          selectedIssue={selectedIssue}
          onSelectIssue={setSelectedIssue}
          onViewportChange={handleViewportChange}
          isLoading={loading}
          onSwitchToList={() => router.push('/issues')}
        />
      </div>

      {/* SIDEBAR CONTAINER (DESKTOP LIST + FILTERS) */}
      <aside className="map-explorer-sidebar">
        {/* Filters Bar */}
        <div className="map-sidebar-filters">
          <div className="map-filter-group">
            <input
              type="text"
              placeholder="Search map issues..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="civic-input map-search-input"
              aria-label="Search map issues"
            />
          </div>

          <div className="map-filter-selects">
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="civic-select"
              aria-label="Filter by category"
            >
              <option value="">All Categories</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>

            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="civic-select"
              aria-label="Filter by status"
            >
              <option value="">All Statuses</option>
              <option value="REPORTED">Reported</option>
              <option value="VERIFIED">Verified</option>
              <option value="ACKNOWLEDGED">Acknowledged</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CITIZEN_VERIFIED">Citizen Verified</option>
              <option value="NOT_FIXED">Not Fixed</option>
            </select>

            <select
              value={selectedPriority}
              onChange={(e) => setSelectedPriority(e.target.value)}
              className="civic-select"
              aria-label="Filter by priority"
            >
              <option value="">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
        </div>

        {/* Issues List Header */}
        <div className="map-sidebar-list-header">
          <h3>
            Visible Issues <span className="count-tag">({issues.length})</span>
          </h3>
          <span className="radius-indicator">
            Within ~{Math.round(radiusMeters / 1000)} km
          </span>
        </div>

        {/* Issues Feed in Visible Area */}
        <div className="map-sidebar-feed">
          {loading && issues.length === 0 ? (
            <div className="map-sidebar-loading">
              <LoadingSpinner />
              <p>Scanning visible coordinates...</p>
            </div>
          ) : error ? (
            <ErrorState
              message={error}
              onRetry={() => fetchMapIssues(center.latitude, center.longitude, radiusMeters)}
            />
          ) : issues.length === 0 ? (
            <EmptyState
              title="No Issues in this Area"
              description="Pan the map, increase your zoom, or clear filters to discover civic reports in other municipal wards."
            />
          ) : (
            <div className="map-issues-stack">
              {issues.map((iss) => {
                const isSelected = selectedIssue?.id === iss.id;
                return (
                  <div
                    key={iss.id}
                    className={`map-sidebar-card-wrap ${isSelected ? 'highlighted' : ''}`}
                    onClick={() => setSelectedIssue(iss)}
                  >
                    <IssueCard issue={iss} />
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </aside>
    </div>
  );
}
