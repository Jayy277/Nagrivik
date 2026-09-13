import type { MetadataRoute } from 'next';
import { getPublicSiteUrl } from '../lib/seo';

export default function robots(): MetadataRoute.Robots {
  const baseUrl = getPublicSiteUrl();

  return {
    rules: {
      userAgent: '*',
      allow: ['/', '/issues', '/issues/*'],
      disallow: ['/api/', '/_next/'],
    },
    sitemap: `${baseUrl}/sitemap.xml`,
  };
}
