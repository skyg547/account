package com.ho.account.auth.core.application.port.out;

public interface PasswordVerifierPort {

    boolean matches(String rawPassword, String storedPassword);
}

