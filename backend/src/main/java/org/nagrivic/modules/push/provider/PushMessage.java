package org.nagrivic.modules.push.provider;

import org.nagrivic.modules.push.model.PlatformType;

import java.util.Map;

/**
 * Provider-independent push notification delivery message.
 */
public record PushMessage(
    String deviceToken,
    String title,
    String body,
    Map<String, String> data,
    PlatformType platform
) {
    public PushMessage {
        if (data == null) {
            data = Map.of();
        }
    }
}
