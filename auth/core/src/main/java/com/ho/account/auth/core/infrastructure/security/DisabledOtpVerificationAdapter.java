package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.OtpVerificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fail-closed port used only while OTP integration is disabled or unspecified.
 */
@Component
@ConditionalOnProperty(prefix = "auth.otp", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledOtpVerificationAdapter implements OtpVerificationPort {

    @Override
    public boolean verifyOtp(String username, String otpCode) {
        return false;
    }

    @Override
    public boolean requiresOtp(String username) {
        return false;
    }
}
