package org.nagrivic.modules.auth.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nagrivic.auth.otp.provider", havingValue = "DEV", matchIfMissing = true)
public class DevOtpProvider implements OtpProvider {

    private static final Logger log = LoggerFactory.getLogger(DevOtpProvider.class);

    @Override
    public void sendOtp(String phoneNumber, String otp, String purpose) {
        log.info("[DEV OTP PROVIDER] Dispatching OTP for {} (Purpose: {}): [OTP={}]", phoneNumber, purpose, otp);
    }
}
