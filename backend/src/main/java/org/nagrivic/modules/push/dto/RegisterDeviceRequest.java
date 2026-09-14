package org.nagrivic.modules.push.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.nagrivic.modules.push.model.PlatformType;

public record RegisterDeviceRequest(
    @NotBlank(message = "Device token is required")
    @Size(max = 512, message = "Device token must not exceed 512 characters")
    String token,

    @NotNull(message = "Platform is required")
    PlatformType platform,

    @Size(max = 64, message = "App version must not exceed 64 characters")
    String appVersion
) {}
