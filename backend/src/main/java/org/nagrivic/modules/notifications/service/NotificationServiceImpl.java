package org.nagrivic.modules.notifications.service;

import org.nagrivic.common.error.ResourceNotFoundException;
import org.nagrivic.modules.auth.exception.AuthException;
import org.nagrivic.modules.auth.service.CurrentUserService;
import org.nagrivic.modules.issues.entity.IssueEntity;
import org.nagrivic.modules.issues.model.IssueStatus;
import org.nagrivic.modules.notifications.dto.NotificationPreferenceResponse;
import org.nagrivic.modules.notifications.dto.NotificationResponse;
import org.nagrivic.modules.notifications.dto.UnreadCountResponse;
import org.nagrivic.modules.notifications.dto.UpdateNotificationPreferenceRequest;
import org.nagrivic.modules.notifications.entity.NotificationEntity;
import org.nagrivic.modules.notifications.entity.NotificationPreferenceEntity;
import org.nagrivic.modules.notifications.model.NotificationType;
import org.nagrivic.modules.notifications.repository.NotificationPreferenceRepository;
import org.nagrivic.modules.notifications.repository.NotificationRepository;
import org.nagrivic.modules.notifications.event.NotificationCreatedEvent;
import org.nagrivic.modules.priority.model.PriorityLevel;
import org.nagrivic.modules.supports.repository.SupportRepository;
import org.nagrivic.modules.users.entity.UserEntity;
import org.nagrivic.modules.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final int MAX_SUPPORTER_FANOUT = 500;

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final SupportRepository supportRepository;
    private final CurrentUserService currentUserService;
    private final NotificationTemplateBuilder templateBuilder;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository,
            SupportRepository supportRepository,
            CurrentUserService currentUserService,
            NotificationTemplateBuilder templateBuilder,
            ApplicationEventPublisher eventPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.supportRepository = supportRepository;
        this.currentUserService = currentUserService;
        this.templateBuilder = templateBuilder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public NotificationEntity createNotification(
            UserEntity user,
            NotificationType type,
            String title,
            String body,
            IssueEntity issue,
            String eventKey,
            Map<String, Object> metadata
    ) {
        if (user == null || type == null || title == null || body == null) {
            return null;
        }

        // 1. Notification Preference Check
        Optional<NotificationPreferenceEntity> prefOpt = preferenceRepository.findByUser_Id(user.getId());
        if (prefOpt.isPresent() && !prefOpt.get().isInAppEnabled()) {
            log.debug("In-app notifications disabled for user {}", user.getId());
            return null;
        }

        // 2. Idempotency / Duplicate protection check
        if (eventKey != null && !eventKey.isBlank()) {
            if (notificationRepository.existsByUser_IdAndEventKey(user.getId(), eventKey)) {
                log.debug("Duplicate notification suppressed for user {} with eventKey {}", user.getId(), eventKey);
                return null;
            }
        }

        NotificationEntity notification = new NotificationEntity(
                user,
                type,
                title,
                body,
                issue,
                eventKey,
                metadata
        );
 
        NotificationEntity saved = notificationRepository.save(notification);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(NotificationCreatedEvent.fromEntity(saved));
        }
        return saved;
    }

    @Override
    @Transactional
    public void handleStatusChange(IssueEntity issue, IssueStatus fromStatus, IssueStatus toStatus, UserEntity actor) {
        if (issue == null || toStatus == null) {
            return;
        }

        NotificationType type = mapStatusToNotificationType(toStatus);
        UUID actorId = actor != null ? actor.getId() : null;
        String eventKey = "status:" + issue.getId() + ":" + toStatus.name();

        Map<String, Object> params = Map.of(
                "from", fromStatus != null ? fromStatus.name() : "",
                "to", toStatus.name()
        );

        // 1. Notify reporter if not the actor
        UserEntity reporter = issue.getReporter();
        if (reporter != null && !reporter.getId().equals(actorId)) {
            NotificationTemplateBuilder.NotificationMessage reporterMsg =
                    templateBuilder.buildMessage(type, true, params);
            createNotification(reporter, type, reporterMsg.title(), reporterMsg.body(), issue, eventKey, params);
        }

        // 2. Notify supporters (excluding actor and reporter)
        List<UUID> supporterIds = supportRepository.findSupporterUserIdsByIssueId(issue.getId());
        if (!supporterIds.isEmpty()) {
            Set<UUID> recipientIds = new HashSet<>(supporterIds);
            if (actorId != null) recipientIds.remove(actorId);
            if (reporter != null) recipientIds.remove(reporter.getId());

            if (!recipientIds.isEmpty()) {
                NotificationTemplateBuilder.NotificationMessage supporterMsg =
                        templateBuilder.buildMessage(type, false, params);

                int count = 0;
                for (UUID supporterId : recipientIds) {
                    if (count >= MAX_SUPPORTER_FANOUT) {
                        log.warn("Supporter notification fan-out threshold reached ({}) for issue {}", MAX_SUPPORTER_FANOUT, issue.getId());
                        break;
                    }
                    userRepository.findById(supporterId).ifPresent(supporter ->
                            createNotification(supporter, type, supporterMsg.title(), supporterMsg.body(), issue, eventKey, params)
                    );
                    count++;
                }
            }
        }
    }

    @Override
    @Transactional
    public void handleResponsibilityResolved(IssueEntity issue) {
        if (issue == null || issue.getReporter() == null) {
            return;
        }

        NotificationType type = NotificationType.ISSUE_RESPONSIBILITY_RESOLVED;
        String eventKey = "responsibility:" + issue.getId();

        Map<String, Object> params = new HashMap<>();
        if (issue.getDepartment() != null) {
            params.put("departmentName", issue.getDepartment().getName());
        }
        if (issue.getCivicBody() != null) {
            params.put("civicBodyName", issue.getCivicBody().getName());
        }

        NotificationTemplateBuilder.NotificationMessage msg = templateBuilder.buildMessage(type, true, params);
        createNotification(issue.getReporter(), type, msg.title(), msg.body(), issue, eventKey, params);
    }

    @Override
    @Transactional
    public void handlePriorityLevelChanged(IssueEntity issue, PriorityLevel newLevel) {
        if (issue == null || issue.getReporter() == null || newLevel == null) {
            return;
        }

        NotificationType type = NotificationType.ISSUE_PRIORITY_CHANGED;
        String eventKey = "priority:" + issue.getId() + ":" + newLevel.name();

        Map<String, Object> params = Map.of("priorityLevel", newLevel.name());

        NotificationTemplateBuilder.NotificationMessage msg = templateBuilder.buildMessage(type, true, params);
        createNotification(issue.getReporter(), type, msg.title(), msg.body(), issue, eventKey, params);
    }

    @Override
    @Transactional
    public void handleCommentAdded(IssueEntity issue, UserEntity commenter) {
        if (issue == null || issue.getReporter() == null || commenter == null) {
            return;
        }

        // Skip notifying commenter about their own comment
        if (commenter.getId().equals(issue.getReporter().getId())) {
            return;
        }

        NotificationType type = NotificationType.ISSUE_COMMENT_ACTIVITY;
        // Comment notifications are individual events per comment
        String eventKey = "comment:" + issue.getId() + ":" + System.currentTimeMillis();

        NotificationTemplateBuilder.NotificationMessage msg = templateBuilder.buildMessage(type, true, Collections.emptyMap());
        createNotification(issue.getReporter(), type, msg.title(), msg.body(), issue, eventKey, Collections.emptyMap());
    }

    @Override
    @Transactional
    public void handleDuplicateLinked(IssueEntity sourceIssue, IssueEntity primaryIssue) {
        if (sourceIssue == null || sourceIssue.getReporter() == null || primaryIssue == null) {
            return;
        }

        NotificationType type = NotificationType.ISSUE_DUPLICATE_DETECTED;
        String eventKey = "duplicate:" + sourceIssue.getId() + ":" + primaryIssue.getId();

        Map<String, Object> params = Map.of("primaryIssueId", primaryIssue.getId().toString());

        NotificationTemplateBuilder.NotificationMessage msg = templateBuilder.buildMessage(type, true, params);
        createNotification(sourceIssue.getReporter(), type, msg.title(), msg.body(), sourceIssue, eventKey, params);
    }

    @Override
    public Page<NotificationResponse> getNotificationsForCurrentUser(Pageable pageable) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        Page<NotificationEntity> page = notificationRepository.findByUser_IdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        return page.map(NotificationResponse::fromEntity);
    }

    @Override
    public UnreadCountResponse getUnreadCountForCurrentUser() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        long count = notificationRepository.countByUser_IdAndReadAtIsNull(currentUser.getId());
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsReadForCurrentUser(UUID notificationId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw AuthException.forbidden("You cannot mark another user's notification as read");
        }

        notification.markAsRead();
        NotificationEntity saved = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void markAllAsReadForCurrentUser() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        notificationRepository.markAllAsReadForUser(currentUser.getId());
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse getPreferencesForCurrentUser() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        NotificationPreferenceEntity preference = preferenceRepository.findByUser_Id(currentUser.getId())
                .orElseGet(() -> preferenceRepository.save(new NotificationPreferenceEntity(currentUser)));
        return NotificationPreferenceResponse.fromEntity(preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse updatePreferencesForCurrentUser(UpdateNotificationPreferenceRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        NotificationPreferenceEntity preference = preferenceRepository.findByUser_Id(currentUser.getId())
                .orElseGet(() -> new NotificationPreferenceEntity(currentUser));

        if (request != null) {
            if (request.inAppEnabled() != null) {
                preference.setInAppEnabled(request.inAppEnabled());
            }
            if (request.pushEnabled() != null) {
                preference.setPushEnabled(request.pushEnabled());
            }
            if (request.emailEnabled() != null) {
                preference.setEmailEnabled(request.emailEnabled());
            }
            if (request.smsEnabled() != null) {
                preference.setSmsEnabled(request.smsEnabled());
            }
        }

        NotificationPreferenceEntity saved = preferenceRepository.save(preference);
        return NotificationPreferenceResponse.fromEntity(saved);
    }

    private NotificationType mapStatusToNotificationType(IssueStatus status) {
        return switch (status) {
            case VERIFIED -> NotificationType.ISSUE_VERIFIED;
            case ACKNOWLEDGED -> NotificationType.ISSUE_ACKNOWLEDGED;
            case IN_PROGRESS -> NotificationType.ISSUE_IN_PROGRESS;
            case RESOLVED -> NotificationType.ISSUE_RESOLVED;
            case NOT_FIXED -> NotificationType.ISSUE_NOT_FIXED;
            case CITIZEN_VERIFIED -> NotificationType.ISSUE_CITIZEN_VERIFIED;
            default -> NotificationType.ISSUE_STATUS_CHANGED;
        };
    }
}
