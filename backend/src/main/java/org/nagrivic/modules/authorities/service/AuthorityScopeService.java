package org.nagrivic.modules.authorities.service;

import org.nagrivic.modules.authorities.dto.AuthorityScopeDto;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface AuthorityScopeService {

    List<AuthorityScopeDto> getActiveScopes(UserEntity user);

    void validateAuthorityScope(UserEntity user, IssueEntity issue);

    boolean hasScope(UserEntity user, IssueEntity issue);

    Specification<IssueEntity> createScopeSpecification(UserEntity user);
}
