import { IssueResponse } from '../types/issue';

export function getPublicSiteUrl(): string {
  return (
    process.env.NEXT_PUBLIC_SITE_URL ||
    process.env.SITE_URL ||
    'https://nagrivic.org'
  ).replace(/\/+$/, '');
}

export function generateIssueJsonLd(issue: IssueResponse): string {
  const siteUrl = getPublicSiteUrl();
  const issueUrl = `${siteUrl}/issues/${issue.id}`;
  const locationName =
    issue.civicResponsibility?.ward?.name ||
    issue.civicResponsibility?.city?.name ||
    issue.civicArea?.ward?.name ||
    issue.civicArea?.city ||
    'Ahmedabad';

  const cleanDescription = (issue.description || '')
    .slice(0, 250)
    .replace(/["\\]/g, '');

  const jsonLd = {
    '@context': 'https://schema.org',
    '@type': 'WebPage',
    name: `${issue.title} | Nagrivic`,
    description: `${cleanDescription} - ${issue.category?.name || 'Civic issue'} in ${locationName}`,
    url: issueUrl,
    datePublished: issue.createdAt,
    dateModified: issue.updatedAt || issue.createdAt,
    breadcrumb: {
      '@type': 'BreadcrumbList',
      itemListElement: [
        {
          '@type': 'ListItem',
          position: 1,
          name: 'Home',
          item: siteUrl,
        },
        {
          '@type': 'ListItem',
          position: 2,
          name: 'Issues',
          item: `${siteUrl}/issues`,
        },
        {
          '@type': 'ListItem',
          position: 3,
          name: issue.title,
          item: issueUrl,
        },
      ],
    },
    mainEntity: {
      '@type': 'Thing',
      name: issue.title,
      description: cleanDescription,
      category: issue.category?.name,
    },
  };

  return JSON.stringify(jsonLd);
}
