package com.ho.account.auth.core.application.port.out;

/**
 * OTP verification boundary. Implementations own secret storage and verification algorithms.
 */
public interface OtpVerificationPort {

    boolean verifyOtp(String username, String otpCode);

    /**
     * Returns whether the authoritative enrollment policy requires OTP for this user.
     */
    boolean requiresOtp(String username);
}
