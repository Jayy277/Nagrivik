package org.nagrivic.modules.priority.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.repository.IssueRepository;
import org.nagrivic.modules.priority.entity.IssuePriorityEntity;
import org.nagrivic.modules.priority.model.IssueSeverity;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.priority.model.PublicImpact;
import org.nagrivic.modules.priority.model.SafetyImpact;
import org.nagrivic.modules.priority.repository.IssuePriorityRepository;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class IssuePriorityServiceImpl implements IssuePriorityService {

    private static final Logger log = LoggerFactory.getLogger(IssuePriorityServiceImpl.class);
    private static final String CALCULATION_VERSION = "v1";

    private final IssuePriorityRepository issuePriorityRepository;
    private final IssueRepository issueRepository;
    private final SupportRepository supportRepository;
    private final org.nagrivic.modules.activity.service.IssueActivityService issueActivityService;
    private final org.nagrivic.modules.notifications.service.NotificationService notificationService;
    private final org.nagrivic.modules.priority.ai.service.PriorityAiService priorityAiService;

    public IssuePriorityServiceImpl(
            IssuePriorityRepository issuePriorityRepository,
            IssueRepository issueRepository,
            SupportRepository supportRepository,
            org.nagrivic.modules.activity.service.IssueActivityService issueActivityService,
            @org.springframework.context.annotation.Lazy org.nagrivic.modules.notifications.service.NotificationService notificationService,
            @org.springframework.context.annotation.Lazy org.nagrivic.modules.priority.ai.service.PriorityAiService priorityAiService
    ) {
        this.issuePriorityRepository = issuePriorityRepository;
        this.issueRepository = issueRepository;
        this.supportRepository = supportRepository;
        this.issueActivityService = issueActivityService;
        this.notificationService = notificationService;
        this.priorityAiService = priorityAiService;
    }

    @Override
    public IssuePriorityEntity calculatePriority(IssueEntity issue) {
        if (issue == null) {
            return null;
        }

        // 1. Resolve canonical issue if duplicate
        IssueEntity canonical = issue.isDuplicate() && issue.getDuplicateOf() != null
                ? issue.getDuplicateOf()
                : issue;

        // 2. Component Scores
        // Severity score (0-30)
        IssueSeverity severity = canonical.getSeverity() != null ? canonical.getSeverity() : IssueSeverity.MEDIUM;
        int severityScore = severity.getScore();

        // Public impact score (0-25)
        PublicImpact impact = canonical.getPublicImpact() != null ? canonical.getPublicImpact() : PublicImpact.LOW;
        int impactScore = impact.getScore();

        // Safety impact score (0-25)
        SafetyImpact safety = canonical.getSafetyImpact() != null ? canonical.getSafetyImpact() : SafetyImpact.LOW;
        int safetyScore = safety.getScore();

        // Age score (0-10)
        int ageScore = calculateAgeScore(canonical.getCreatedAt());

        // Support score (0-10)
        long supportCount = supportRepository.countByIssue_Id(canonical.getId());
        int supportScore = calculateSupportScore(supportCount);

        // 3. Total Deterministic Baseline Score (0-100)
        int deterministicScore = severityScore + impactScore + safetyScore + ageScore + supportScore;
        deterministicScore = Math.min(100, Math.max(0, deterministicScore));

        int finalScore = deterministicScore;
        String calculationVersion = CALCULATION_VERSION;

        // Optional server-side AI blended calculation policy (strict bounds, never forces CRITICAL)
        if (priorityAiService != null && priorityAiService.isEnabled()
                && priorityAiService.getInfluenceMode() == org.nagrivic.modules.priority.ai.model.PriorityInfluenceMode.BLENDED) {
            Optional<org.nagrivic.modules.priority.ai.entity.IssueAiPriorityEntity> aiRecOpt = priorityAiService.getExistingRecommendation(canonical.getId());
            if (aiRecOpt.isPresent()) {
                finalScore = priorityAiService.calculateBlendedScore(deterministicScore, severityScore, impactScore, safetyScore, aiRecOpt.get());
                if (finalScore != deterministicScore) {
                    calculationVersion = "v1-ai-blended";
                }
            }
        }

        // 4. Map to Priority Level
        PriorityLevel level = mapScoreToLevel(finalScore);

        log.debug("Calculated priority for canonical issue {}: total={}, level={}, breakdown=[sev={}, imp={}, saf={}, age={}, sup={}], version={}",
                canonical.getId(), finalScore, level, severityScore, impactScore, safetyScore, ageScore, supportScore, calculationVersion);

        // 5. Idempotent persistence
        Optional<IssuePriorityEntity> existingOpt = issuePriorityRepository.findByIssue_Id(canonical.getId());
        PriorityLevel previousLevel = existingOpt.map(IssuePriorityEntity::getPriorityLevel).orElse(null);

        IssuePriorityEntity entity;
        boolean priorityChanged;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            priorityChanged = !entity.getPriorityLevel().equals(level) || entity.getScore() != finalScore;
            entity.setPriorityLevel(level);
            entity.setScore(finalScore);
            entity.setSeverityScore(severityScore);
            entity.setImpactScore(impactScore);
            entity.setSafetyScore(safetyScore);
            entity.setAgeScore(ageScore);
            entity.setSupportScore(supportScore);
            entity.setCalculationVersion(calculationVersion);
            entity.setCalculatedAt(Instant.now());
            canonical.setPriority(entity);
        } else {
            priorityChanged = true;
            entity = new IssuePriorityEntity(
                    canonical,
                    level,
                    finalScore,
                    severityScore,
                    impactScore,
                    safetyScore,
                    ageScore,
                    supportScore,
                    calculationVersion
            );
            canonical.setPriority(entity);
        }

        IssuePriorityEntity saved = issuePriorityRepository.save(entity);

        if (priorityChanged) {
            issueActivityService.recordActivity(
                    canonical,
                    org.nagrivic.modules.activity.model.IssueActivityType.PRIORITY_RECALCULATED,
                    null,
                    java.util.Map.of(
                            "level", level.name(),
                            "score", finalScore,
                            "calculationVersion", calculationVersion
                    )
            );
        }

        if (previousLevel != null && !previousLevel.equals(level)) {
            notificationService.handlePriorityLevelChanged(canonical, level);
        }

        return saved;
    }

    @Override
    public IssuePriorityEntity recalculatePriority(UUID issueId) {
        if (issueId == null) {
            return null;
        }

        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", issueId));

        return calculatePriority(issue);
    }

    private int calculateAgeScore(Instant createdAt) {
        if (createdAt == null) {
            return 0;
        }
        long days = Duration.between(createdAt, Instant.now()).toDays();
        if (days < 2) {
            return 0;      // 0–1 days
        } else if (days <= 7) {
            return 2;      // 2–7 days
        } else if (days <= 30) {
            return 5;      // 8–30 days
        } else if (days <= 90) {
            return 8;      // 31–90 days
        } else {
            return 10;     // 90+ days (capped at 10)
        }
    }

    private int calculateSupportScore(long supportCount) {
        if (supportCount <= 0) {
            return 0;
        } else if (supportCount <= 2) {
            return 2;
        } else if (supportCount <= 5) {
            return 4;
        } else if (supportCount <= 10) {
            return 6;
        } else if (supportCount <= 20) {
            return 8;
        } else {
            return 10;     // Capped at 10
        }
    }

    private PriorityLevel mapScoreToLevel(int score) {
        if (score < 25) {
            return PriorityLevel.LOW;
        } else if (score < 50) {
            return PriorityLevel.MEDIUM;
        } else if (score < 75) {
            return PriorityLevel.HIGH;
        } else {
            return PriorityLevel.CRITICAL;
        }
    }
}
