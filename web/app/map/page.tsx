import React, { Suspense } from 'react';
import type { Metadata } from 'next';
import dynamic from 'next/dynamic';
import Link from 'next/link';
import { getIssues, getCategories } from '../../lib/api/issues';
import { IssueResponse } from '../../types/issue';
import { getDefaultCityConfig } from '../../lib/config/city';
import MapExplorerClient from './MapExplorerClient';

export const metadata: Metadata = {
  title: 'Public Civic Map | Nagrivic',
  description:
    'Explore reported civic issues geographically on the interactive Nagrivic map. Track potholes, sanitation hazards, and infrastructure progress in your neighborhood.',
  openGraph: {
    title: 'Public Civic Map | Nagrivic',
    description:
      'Explore reported civic issues geographically on the interactive Nagrivic map. Track potholes, sanitation hazards, and infrastructure progress in your neighborhood.',
    url: '/map',
    type: 'website',
  },
};

interface MapPageProps {
  searchParams: Promise<{
    category?: string;
    status?: string;
    priority?: string;
    search?: string;
    lat?: string;
    lng?: string;
    radius?: string;
  }>;
}

export default async function MapPage({ searchParams }: MapPageProps) {
  const params = await searchParams;
  const defaultCity = getDefaultCityConfig();

  const lat = params.lat ? parseFloat(params.lat) : defaultCity.latitude;
  const lng = params.lng ? parseFloat(params.lng) : defaultCity.longitude;
  const radius = params.radius ? parseFloat(params.radius) : defaultCity.defaultRadiusMeters;

  // Fetch initial category list & initial visible issues
  const [categories, initialIssuesData] = await Promise.all([
    getCategories(),
    getIssues({
      latitude: lat,
      longitude: lng,
      radiusMeters: radius,
      sort: 'NEAREST',
      status: params.status as any,
      priority: params.priority as any,
      q: params.search,
      page: 0,
      size: 50,
    }).catch(() => ({
      content: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
      empty: true,
    })),
  ]);

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'Place',
    name: `${defaultCity.name} Civic Map`,
    geo: {
      '@type': 'GeoCoordinates',
      latitude: defaultCity.latitude,
      longitude: defaultCity.longitude,
    },
    description: 'Interactive civic issue discovery map for municipal transparency.',
  };

  return (
    <div className="map-page-shell">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />

      <div className="map-page-header-bar">
        <div>
          <h1 className="map-page-title">Civic Issue Map</h1>
          <p className="map-page-subtitle">
            Live geographic discovery powered by authoritative municipal PostGIS queries
          </p>
        </div>
        <div className="map-page-nav-actions">
          <Link href="/issues" className="civic-button civic-button-outline">
            📋 Switch to List View
          </Link>
        </div>
      </div>

      <Suspense fallback={<div className="map-loading-container">Loading map...</div>}>
        <MapExplorerClient
          initialIssues={initialIssuesData.content}
          categories={categories}
          defaultCenter={{ latitude: lat, longitude: lng }}
          initialRadius={radius}
          initialFilters={{
            category: params.category,
            status: params.status,
            priority: params.priority,
            search: params.search,
          }}
        />
      </Suspense>
    </div>
  );
}
