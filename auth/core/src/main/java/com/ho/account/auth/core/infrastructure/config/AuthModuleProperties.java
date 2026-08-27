package com.ho.account.auth.core.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
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
        if (internalApi.getToken() == null || internalApi.getToken().isBlank()) {
            throw new IllegalStateException(
                    "Fail-Closed Security Violation: 'auth.internal-api.token' must be provided via AUTH_INTERNAL_API_TOKEN environment variable.");
        }
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

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public static class Jwt {
        private String secret;
        private String issuer = "auth-service";
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

        public long getExpirationSeconds() {
            return expirationSeconds;
        }

        public void setExpirationSeconds(long expirationSeconds) {
            this.expirationSeconds = expirationSeconds;
        }
    }

    public static class MasterData {
        private String baseUrl = "http://localhost:8082";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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

