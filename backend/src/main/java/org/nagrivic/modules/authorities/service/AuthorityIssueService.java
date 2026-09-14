package org.nagrivic.modules.authorities.service;

import org.nagrivic.modules.authorities.dto.*;
import org.nagrivic.modules.issues.dto.IssueResponse;
import org.nagrivic.modules.statushistory.dto.ChangeStatusRequest;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuthorityIssueService {

    AuthorityDashboardMetrics getDashboardMetrics(UserEntity user);

    Page<AuthorityIssueItemResponse> findScopedIssues(UserEntity user, AuthorityIssueFilter filter, Pageable pageable);

    AuthorityIssueDetailResponse getScopedIssueDetail(UUID issueId, UserEntity user);

    IssueResponse changeIssueStatus(UUID issueId, ChangeStatusRequest request, UserEntity user);
}
