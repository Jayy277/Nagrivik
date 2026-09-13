package org.nagrivic.modules.moderation.service;

import org.nagrivic.common.error.ConflictException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Lightweight deterministic content abuse validator for comments and issues.
 * Explicitly separates content safety / mechanical abuse from political disagreement.
 * Does NOT classify political criticism as spam or abuse.
 */
@Component
public class ContentAbuseValidator {

    // Pattern for 10 or more consecutive identical characters (e.g. "aaaaaaaaaa", "!!!!!!!!!!")
    private static final Pattern EXCESSIVE_REPEATED_CHARS = Pattern.compile("([a-z!?,.])\\1{9,}");

    // In-memory cache for recent comment hashes: key is userId:issueId, value is timestamp + content hash
    private final Map<String, RecentSubmission> recentComments = new ConcurrentHashMap<>();

    // In-memory cache for recent issue creations: key is userId:categoryId, value is timestamp + title + locationId
    private final Map<String, RecentIssueSubmission> recentIssues = new ConcurrentHashMap<>();

    private record RecentSubmission(Instant timestamp, String contentHash) {}
    private record RecentIssueSubmission(Instant timestamp, String title, UUID locationId) {}

    /**
     * Validates comment content for obvious spam/abuse characteristics.
     *
     * @param userId authenticated author
     * @param issueId target civic issue
     * @param content raw comment content
     */
    public void validateComment(UUID userId, UUID issueId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be blank");
        }

        String normalized = content.trim();

        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("Comment content cannot exceed 1000 characters");
        }

        // Detect excessive repeated characters (e.g. "aaaaaaaaaa", "!!!!!!!!!!")
        if (EXCESSIVE_REPEATED_CHARS.matcher(normalized).find()) {
            throw new IllegalArgumentException("Comment contains excessive repeated characters and cannot be posted");
        }

        // Detect rapid identical comment submissions from the same user within 60 seconds
        String key = userId.toString() + ":" + issueId.toString();
        Instant now = Instant.now();
        RecentSubmission last = recentComments.get(key);

        if (last != null && Duration.between(last.timestamp(), now).getSeconds() < 60) {
            if (last.contentHash().equalsIgnoreCase(normalized)) {
                throw new ConflictException("Duplicate comment recently submitted. Please wait before posting again.");
            }
        }

        recentComments.put(key, new RecentSubmission(now, normalized));
    }

    /**
     * Validates issue creation against rapid identical duplicate submissions from the same citizen.
     * Prevents accidental double-clicks and repeated identical submissions while allowing legitimate nearby complaints.
     *
     * @param userId author UUID
     * @param title issue title
     * @param categoryId issue category UUID
     * @param locationId issue location UUID
     */
    public void validateIssueSubmission(UUID userId, String title, UUID categoryId, UUID locationId) {
        String key = userId.toString() + ":" + categoryId.toString();
        Instant now = Instant.now();
        RecentIssueSubmission last = recentIssues.get(key);

        if (last != null && Duration.between(last.timestamp(), now).getSeconds() < 10) {
            if (last.locationId().equals(locationId) && last.title().equalsIgnoreCase(title.trim())) {
                throw new ConflictException("Identical issue was just submitted. Please wait a few seconds before submitting again.");
            }
        }

        recentIssues.put(key, new RecentIssueSubmission(now, title.trim(), locationId));
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Clears in-memory caches (for tests).
     */
    public void reset() {
        recentComments.clear();
        recentIssues.clear();
    }
}
