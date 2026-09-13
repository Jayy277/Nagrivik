import type { MetadataRoute } from 'next';
import { getPublicSiteUrl } from '../lib/seo';
import { getIssues } from '../lib/api/issues';

export const revalidate = 3600; // Revalidate sitemap hourly

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = getPublicSiteUrl();

  const staticRoutes: MetadataRoute.Sitemap = [
    {
      url: baseUrl,
      lastModified: new Date(),
      changeFrequency: 'daily',
      priority: 1.0,
    },
    {
      url: `${baseUrl}/issues`,
      lastModified: new Date(),
      changeFrequency: 'hourly',
      priority: 0.9,
    },
  ];

  try {
    const issuesResponse = await getIssues({ size: 100, sort: 'NEWEST' });
    const issueRoutes: MetadataRoute.Sitemap = (issuesResponse.content || []).map(
      (issue) => ({
        url: `${baseUrl}/issues/${issue.id}`,
        lastModified: new Date(issue.updatedAt || issue.createdAt),
        changeFrequency: 'daily',
        priority: 0.8,
      })
    );

    return [...staticRoutes, ...issueRoutes];
  } catch {
    return staticRoutes;
  }
}
