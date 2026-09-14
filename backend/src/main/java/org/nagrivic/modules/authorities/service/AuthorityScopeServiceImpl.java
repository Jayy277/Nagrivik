package org.nagrivic.modules.authorities.service;

import jakarta.persistence.criteria.Predicate;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.authorities.dto.AuthorityScopeDto;
import org.nagrivic.modules.authorities.entity.AuthorityAssignmentEntity;
import org.nagrivic.modules.authorities.repository.AuthorityAssignmentRepository;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.ResponsibilityStatus;
import org.nagrivic.modules.users.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AuthorityScopeServiceImpl implements AuthorityScopeService {

    private final AuthorityAssignmentRepository assignmentRepository;

    public AuthorityScopeServiceImpl(AuthorityAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    public List<AuthorityScopeDto> getActiveScopes(UserEntity user) {
        if (user == null) {
            return List.of();
        }
        return assignmentRepository.findByUser_IdAndActiveTrue(user.getId())
                .stream()
                .map(AuthorityScopeDto::fromEntity)
                .toList();
    }

    @Override
    public void validateAuthorityScope(UserEntity user, IssueEntity issue) {
        if (user == null) {
            throw AuthException.unauthorized("Authentication required for authority operations");
        }

        String role = user.getRole();
        if (!"OFFICER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            throw AuthException.forbidden("Only civic authorities can access operational authority management");
        }

        if (issue == null) {
            throw new IllegalArgumentException("Issue cannot be null");
        }

        ResponsibilityStatus respStatus = issue.getResponsibilityStatus();
        if (respStatus != ResponsibilityStatus.RESOLVED) {
            throw AuthException.forbidden("Issue responsibility is unresolved; authority scope cannot be established");
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            // Administrators follow documented security policy: full oversight unless explicitly assigned
            List<AuthorityAssignmentEntity> adminAssignments = assignmentRepository.findByUser_IdAndActiveTrue(user.getId());
            if (adminAssignments.isEmpty()) {
                return;
            }
            if (matchesAnyAssignment(adminAssignments, issue)) {
                return;
            }
            // Admin with specific assignments is scoped to those assignments
            throw AuthException.forbidden("Issue falls outside your assigned authority jurisdiction or department");
        }

        // OFFICER role must have active assignments
        List<AuthorityAssignmentEntity> assignments = assignmentRepository.findByUser_IdAndActiveTrue(user.getId());
        if (assignments.isEmpty()) {
            throw AuthException.forbidden("Authority user has no active jurisdiction or department assignment");
        }

        if (!matchesAnyAssignment(assignments, issue)) {
            throw AuthException.forbidden("Issue falls outside your assigned authority jurisdiction or department");
        }
    }

    @Override
    public boolean hasScope(UserEntity user, IssueEntity issue) {
        try {
            validateAuthorityScope(user, issue);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Specification<IssueEntity> createScopeSpecification(UserEntity user) {
        if (user == null) {
            return (root, query, cb) -> cb.disjunction();
        }

        String role = user.getRole();
        if (!"OFFICER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return (root, query, cb) -> cb.disjunction();
        }

        List<AuthorityAssignmentEntity> assignments = assignmentRepository.findByUser_IdAndActiveTrue(user.getId());

        if ("ADMIN".equalsIgnoreCase(role) && assignments.isEmpty()) {
            // Global admin: show all issues with resolved responsibility
            return (root, query, cb) -> cb.equal(root.get("responsibilityStatus"), ResponsibilityStatus.RESOLVED);
        }

        if (assignments.isEmpty()) {
            // Officer without active assignments has 0 scoped issues
            return (root, query, cb) -> cb.disjunction();
        }

        return (root, query, cb) -> {
            List<Predicate> assignmentPredicates = new ArrayList<>();

            for (AuthorityAssignmentEntity a : assignments) {
                List<Predicate> conjuncts = new ArrayList<>();

                if (a.getCivicBody() != null) {
                    conjuncts.add(cb.equal(root.get("civicBody").get("id"), a.getCivicBody().getId()));
                }
                if (a.getCity() != null) {
                    conjuncts.add(cb.equal(root.get("city").get("id"), a.getCity().getId()));
                }
                if (a.getWard() != null) {
                    conjuncts.add(cb.equal(root.get("ward").get("id"), a.getWard().getId()));
                }
                if (a.getDepartment() != null) {
                    conjuncts.add(cb.equal(root.get("department").get("id"), a.getDepartment().getId()));
                }

                assignmentPredicates.add(cb.and(conjuncts.toArray(new Predicate[0])));
            }

            Predicate resolvedPredicate = cb.equal(root.get("responsibilityStatus"), ResponsibilityStatus.RESOLVED);

            return cb.and(resolvedPredicate, cb.or(assignmentPredicates.toArray(new Predicate[0])));
        };
    }

    private boolean matchesAnyAssignment(List<AuthorityAssignmentEntity> assignments, IssueEntity issue) {
        for (AuthorityAssignmentEntity a : assignments) {
            if (a.getCivicBody() != null) {
                if (issue.getCivicBody() == null || !a.getCivicBody().getId().equals(issue.getCivicBody().getId())) {
                    continue;
                }
            }
            if (a.getCity() != null) {
                if (issue.getCity() == null || !a.getCity().getId().equals(issue.getCity().getId())) {
                    continue;
                }
            }
            if (a.getWard() != null) {
                if (issue.getWard() == null || !a.getWard().getId().equals(issue.getWard().getId())) {
                    continue;
                }
            }
            if (a.getDepartment() != null) {
                if (issue.getDepartment() == null || !a.getDepartment().getId().equals(issue.getDepartment().getId())) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
}
