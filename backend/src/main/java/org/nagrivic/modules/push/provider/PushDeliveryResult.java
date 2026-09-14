package org.nagrivic.modules.push.provider;

/**
 * Result of a push delivery attempt through a provider.
 */
public record PushDeliveryResult(
    Status status,
    String providerMessageId,
    String errorCode,
    String errorMessage
) {
    public enum Status {
        SUCCESS,
        FAILED,
        INVALID_TOKEN,
        SKIPPED
    }

    public static PushDeliveryResult success(String messageId) {
        return new PushDeliveryResult(Status.SUCCESS, messageId, null, null);
    }

    public static PushDeliveryResult invalidToken(String errorCode, String message) {
        return new PushDeliveryResult(Status.INVALID_TOKEN, null, errorCode, message);
    }

    public static PushDeliveryResult failed(String errorCode, String message) {
        return new PushDeliveryResult(Status.FAILED, null, errorCode, message);
    }

    public static PushDeliveryResult skipped(String reason) {
        return new PushDeliveryResult(Status.SKIPPED, null, "SKIPPED", reason);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isInvalidToken() {
        return status == Status.INVALID_TOKEN;
    }
}
