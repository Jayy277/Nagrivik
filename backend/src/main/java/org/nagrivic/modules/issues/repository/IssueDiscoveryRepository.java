package org.nagrivic.modules.issues.repository;

import org.nagrivic.modules.issues.dto.IssueDiscoveryFilter;
import org.nagrivic.modules.issues.dto.IssueDiscoveryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Custom repository interface for advanced public issue discovery, text search, and spatial filtering.
 */
public interface IssueDiscoveryRepository {
    Page<IssueDiscoveryItem> discoverIssues(IssueDiscoveryFilter filter, Pageable pageable);
}
