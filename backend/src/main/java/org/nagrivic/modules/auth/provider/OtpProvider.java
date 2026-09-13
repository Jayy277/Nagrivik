package org.nagrivic.modules.auth.provider;

public interface OtpProvider {
    void sendOtp(String phoneNumber, String otp, String purpose);
}
