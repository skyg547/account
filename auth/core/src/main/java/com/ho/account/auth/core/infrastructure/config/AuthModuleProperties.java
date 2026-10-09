package com.ho.account.auth.core.infrastructure.config;

import com.ho.account.auth.core.infrastructure.security.PasswordEncoderPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ==============================================================================
 * Auth Module Configuration Properties (인증 모듈 환경 설정 프로퍼티)
 * ==============================================================================
 * [Architecture & Pedagogical Explanation]
 * 1. Fail-Closed Security Policy (보안 실패 기본 차단 정책)
 *    - JWT Secret Key, Internal API Token 등 민감한 인증 보안 자격증명 필드의 하드코딩된 디폴트 값을 제거합니다.
 *    - 환경변수(AUTH_JWT_SECRET, AUTH_INTERNAL_API_TOKEN)가 제공되지 않은 경우,
 *      애플리케이션 시작 시점(@PostConstruct)에 Fail-Closed 예외(IllegalStateException)를 발생시켜
 *      보안이 취약한 상태로 서비스가 작동되는 위험을 원천 차단합니다.
 *    - 설정된 사용자의 비밀번호(auth.users[].password)가 PasswordEncoderPolicy를 준수하지 않는 경우
 *      (예: 평문, noop, 잘못된 BCrypt 페이로드, cost factor 위반) 시작 시점에 즉시 실패 처리합니다.
 *
 * 2. Runtime Credential Isolation (실행 시점 민감 정보 격리)
 *    - local을 포함한 모든 프로파일은 JWT secret, internal token, 사용자 비밀번호 기본값을 포함하지 않습니다.
 *    - local 실행자는 매번 임시 값을 생성해 현재 프로세스에만 주입하고 dev/prod도 외부 입력을 강제합니다.
 * ==============================================================================
 */
@ConfigurationProperties(prefix = "auth")
public class AuthModuleProperties {

    private Jwt jwt = new Jwt();
    private MasterData masterData = new MasterData();
    private Persistence persistence = new Persistence();
    private InternalApi internalApi = new InternalApi();
    private LoginSecurity loginSecurity = new LoginSecurity();
    private Otp otp = new Otp();
    private Sso sso = new Sso();
    private List<User> users = new ArrayList<>();

    @PostConstruct
    public void validateFailClosedPolicy() {
        if (jwt.getSecret() == null || jwt.getSecret().isBlank()) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.jwt.secret' must be provided via AUTH_JWT_SECRET environment variable.");
        }
        if (jwt.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.jwt.secret' must be at least 32 UTF-8 bytes for secure HS256 signing.");
        }
        if (jwt.getIssuer() == null || jwt.getIssuer().isBlank()
                || jwt.getAudience() == null || jwt.getAudience().isBlank()) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: JWT issuer and audience must be configured.");
        }
        // A one-second configured TTL can round down to less than one usable JWT second.
        if (jwt.getExpirationSeconds() < 2) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.jwt.expiration-seconds' must be at least 2.");
        }
        if (internalApi.getToken() == null || internalApi.getToken().isBlank()) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.internal-api.token' must be provided via AUTH_INTERNAL_API_TOKEN environment variable.");
        }
        masterData.validateTimeouts();
        validateConfiguredUsers();
    }

    /**
     * Configured user 목록 전체를 저장소 접근 전에 검증합니다.
     *
     * <p>오류는 입력값이나 사용자 식별자를 반영하지 않고 설정 필드 경로만 제공합니다.</p>
     */
    public void validateConfiguredUsers() {
        if (users == null) {
            throw failClosed("auth.users must not be null.");
        }
        for (int index = 0; index < users.size(); index++) {
            String userPath = "auth.users[" + index + "]";
            User user = users.get(index);
            if (user == null) {
                throw failClosed(userPath + " must not be null.");
            }
            if (user.getUsername() == null || user.getUsername().isBlank()) {
                throw failClosed(userPath + ".username must not be blank.");
            }
            validateConfiguredUserPassword(user.getPassword(), userPath + ".password");
        }
        validateOtpConfiguration();
        validateSsoConfiguration();
    }

    private void validateOtpConfiguration() {
        if (!"HmacSHA1".equals(otp.getAlgorithm())) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.otp.algorithm' must be HmacSHA1.");
        }
        if (otp.getDigits() != 6) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.otp.digits' must be 6.");
        }
        if (otp.getPeriodSeconds() <= 0) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.otp.period-seconds' must be greater than zero.");
        }
        if (otp.getToleranceSteps() < 0 || otp.getToleranceSteps() > 1) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.otp.tolerance-steps' must be 0 or 1.");
        }
        otp.getUserSecrets().forEach((username, secret) -> {
            if (username == null || username.isBlank() || secret == null || secret.isBlank()) {
                throw new IllegalStateException(
                        "Fail-Closed Security Violation: configured OTP users and secrets must not be blank.");
            }
            String normalizedSecret = secret.trim().toUpperCase(Locale.ROOT);
            if (!normalizedSecret.matches("[A-Z2-7]+=*")) {
                throw new IllegalStateException(
                        "Fail-Closed Security Violation: configured OTP secrets must use Base32 encoding.");
            }
        });
    }

    private void validateSsoConfiguration() {
        if (sso.getDefaultProvider() == null || sso.getDefaultProvider().isBlank()) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.sso.default-provider' must not be blank.");
        }
        Set<String> configuredCredentials = new HashSet<>();
        for (SsoIdentity identity : sso.getIdentities()) {
            if (identity == null
                    || identity.getProvider() == null || identity.getProvider().isBlank()
                    || identity.getCredential() == null || identity.getCredential().isBlank()
                    || identity.getSubject() == null || identity.getSubject().isBlank()) {
                throw new IllegalStateException(
                        "Fail-Closed Security Violation: configured SSO identities require provider, credential, and subject.");
            }
            String credentialKey = identity.getProvider().trim().toLowerCase(Locale.ROOT)
                    + '\u0000' + identity.getCredential();
            if (!configuredCredentials.add(credentialKey)) {
                throw new IllegalStateException(
                        "Fail-Closed Security Violation: duplicate SSO credentials are not allowed for a provider.");
            }
        }
    }

    public static void validateConfiguredUserPassword(String password, String fieldPath) {
        try {
            PasswordEncoderPolicy.validate(password, fieldPath);
        } catch (IllegalArgumentException ex) {
            throw failClosed(ex.getMessage());
        }
    }

    private static IllegalStateException failClosed(String message) {
        return new IllegalStateException("Fail-Closed Security Violation: " + message);
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public MasterData getMasterData() {
        return masterData;
    }

    public void setMasterData(MasterData masterData) {
        this.masterData = masterData;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public InternalApi getInternalApi() {
        return internalApi;
    }

    public void setInternalApi(InternalApi internalApi) {
        this.internalApi = internalApi;
    }

    public LoginSecurity getLoginSecurity() {
        return loginSecurity;
    }

    public void setLoginSecurity(LoginSecurity loginSecurity) {
        this.loginSecurity = loginSecurity;
    }

    public Otp getOtp() {
        return otp;
    }

    public void setOtp(Otp otp) {
        this.otp = otp == null ? new Otp() : otp;
    }

    public Sso getSso() {
        return sso;
    }

    public void setSso(Sso sso) {
        this.sso = sso == null ? new Sso() : sso;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public static class Jwt {
        private String secret;
        private String issuer = "auth-service";
        private String audience = "account-api";
        private long expirationSeconds = 3600L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }
    }

    public static class MasterData {
        private String baseUrl = "http://localhost:8082";
        // Bind text first so even malformed external values produce only a field-path validation error.
        private String connectTimeoutMillis = "1000";
        private String readTimeoutMillis = "3000";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(String connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public String getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(String readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }

        public int validatedConnectTimeoutMillis() {
            return boundedTimeout(connectTimeoutMillis, "auth.master-data.connect-timeout-millis");
        }

        public int validatedReadTimeoutMillis() {
            return boundedTimeout(readTimeoutMillis, "auth.master-data.read-timeout-millis");
        }

        private void validateTimeouts() {
            validatedConnectTimeoutMillis();
            validatedReadTimeoutMillis();
        }

        private static int boundedTimeout(String value, String fieldPath) {
            try {
                int millis = Integer.parseInt(value);
                if (millis > 0 && millis <= 10_000) {
                    return millis;
                }
            } catch (NumberFormatException ignored) {
                // Keep the supplied text out of the startup error, including nested causes.
            }
            throw failClosed(fieldPath + " must be between 1 and 10000 milliseconds.");
        }
    }

    public static class Persistence {
        private String mode = "jpa";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class InternalApi {
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class LoginSecurity {
        private int maxFailures = 5;
        private long lockDurationMinutes = 15L;
        private String store = "memory";

        public int getMaxFailures() {
            return maxFailures;
        }

        public void setMaxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
        }

        public long getLockDurationMinutes() {
            return lockDurationMinutes;
        }

        public void setLockDurationMinutes(long lockDurationMinutes) {
            this.lockDurationMinutes = lockDurationMinutes;
        }

        public String getStore() {
            return store;
        }

        public void setStore(String store) {
            this.store = store;
        }
    }

    public static class Otp {
        private boolean enabled;
        private String algorithm = "HmacSHA1";
        private int digits = 6;
        private long periodSeconds = 30L;
        private int toleranceSteps = 1;
        private Map<String, String> userSecrets = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public int getDigits() {
            return digits;
        }

        public void setDigits(int digits) {
            this.digits = digits;
        }

        public long getPeriodSeconds() {
            return periodSeconds;
        }

        public void setPeriodSeconds(long periodSeconds) {
            this.periodSeconds = periodSeconds;
        }

        public int getToleranceSteps() {
            return toleranceSteps;
        }

        public void setToleranceSteps(int toleranceSteps) {
            this.toleranceSteps = toleranceSteps;
        }

        public Map<String, String> getUserSecrets() {
            return userSecrets;
        }

        public void setUserSecrets(Map<String, String> userSecrets) {
            this.userSecrets = userSecrets == null ? new HashMap<>() : new HashMap<>(userSecrets);
        }
    }

    public static class Sso {
        private boolean enabled;
        private String defaultProvider = "local";
        private List<SsoIdentity> identities = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public List<SsoIdentity> getIdentities() {
            return identities;
        }

        public void setIdentities(List<SsoIdentity> identities) {
            this.identities = identities == null ? new ArrayList<>() : new ArrayList<>(identities);
        }
    }

    public static class SsoIdentity {
        private String provider = "local";
        private String credential;
        private String subject;
        private String email;
        private String name;
        private String departmentCode;
        private List<String> roles = new ArrayList<>();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getCredential() {
            return credential;
        }

        public void setCredential(String credential) {
            this.credential = credential;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDepartmentCode() {
            return departmentCode;
        }

        public void setDepartmentCode(String departmentCode) {
            this.departmentCode = departmentCode;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles == null ? new ArrayList<>() : new ArrayList<>(roles);
        }
    }

    public static class User {
        private String username;
        private String password;
        private String departmentCode;
        private boolean active = true;
        private boolean locked = false;
        private List<String> roles = List.of("ROLE_USER");

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDepartmentCode() {
            return departmentCode;
        }

        public void setDepartmentCode(String departmentCode) {
            this.departmentCode = departmentCode;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }
}
