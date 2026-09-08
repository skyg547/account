package com.ho.account.configserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/**
 * Denies crypto HTTP requests before body parsing. The enable flag is a local HTTP
 * policy as well as a framework setting; it alone does not unregister the endpoints.
 * Authentication uses an externally supplied token, with no generated/default user.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EncryptionEndpointSecurityFilter extends OncePerRequestFilter {

    public static final String CONFIG_TOKEN_HEADER = "X-Config-Token";
    public static final String INTERNAL_CONFIG_TOKEN_HEADER = "X-Config-Internal-Token";
    private static final List<String> CREDENTIAL_HEADERS = List.of(
            CONFIG_TOKEN_HEADER, INTERNAL_CONFIG_TOKEN_HEADER, HttpHeaders.AUTHORIZATION);

    private final boolean encryptEnabled;
    private final String cryptoToken;
    private final String endpointPrefix;
    private final UrlPathHelper pathHelper = new UrlPathHelper();

    public EncryptionEndpointSecurityFilter(
            @Value("${spring.cloud.config.server.encrypt.enabled:false}") boolean encryptEnabled,
            @Value("${config.crypto.endpoint.token:${config-server.internal-crypto-token:}}") String cryptoToken,
            @Value("${spring.cloud.config.server.prefix:}") String configPrefix,
            @Value("${spring.mvc.servlet.path:}") String servletPrefix) {
        this.encryptEnabled = encryptEnabled;
        this.cryptoToken = cryptoToken;
        this.endpointPrefix = normalizePrefix(servletPrefix) + normalizePrefix(configPrefix);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Decode escapes and remove matrix parameters like MVC, excluding context path.
        String path = pathHelper.getPathWithinApplication(request);
        if (isEncryptionPath(path) && !authorize(request, response)) {
            return;
        }
        filterChain.doFilter(request, response);
    }

    boolean authorize(HttpServletRequest request, HttpServletResponse response) {
        // Never sendError: an error dispatch can echo the request path or input.
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (!encryptEnabled) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        if (!isTokenValue(cryptoToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        String provided = extractProvidedToken(request);
        if (!isTokenValue(provided) || !MessageDigest.isEqual(
                cryptoToken.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        return true;
    }

    private boolean isEncryptionPath(String path) {
        return matchesTree(path, endpointPrefix + "/encrypt")
                || matchesTree(path, endpointPrefix + "/decrypt");
    }

    private static boolean matchesTree(String path, String root) {
        return path.equals(root) || path.startsWith(root + "/");
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty() || prefix.equals("/")) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String extractProvidedToken(HttpServletRequest request) {
        String provided = null;
        for (String header : CREDENTIAL_HEADERS) {
            List<String> values = Collections.list(request.getHeaders(header));
            if (values.isEmpty()) {
                continue;
            }
            // Reject ambiguity instead of choosing one of conflicting credentials.
            if (values.size() != 1 || provided != null) {
                return null;
            }
            provided = values.get(0);
            if (header.equals(HttpHeaders.AUTHORIZATION)) {
                if (!provided.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    return null;
                }
                provided = provided.substring(7);
            }
        }
        return provided;
    }

    private static boolean isTokenValue(String value) {
        return value != null && !value.isEmpty()
                && value.chars().allMatch(c -> c > 32 && c < 127 && c != ',');
    }
}
