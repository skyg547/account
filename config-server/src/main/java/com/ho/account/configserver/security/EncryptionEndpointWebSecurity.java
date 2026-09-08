package com.ho.account.configserver.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cloud.config.server.encryption.EncryptionController;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Authorizes the resolved crypto handler even if its URL mapping changes. */
@Configuration(proxyBeanMethods = false)
class EncryptionEndpointWebSecurity implements WebMvcConfigurer {
    private final EncryptionEndpointSecurityFilter security;

    EncryptionEndpointWebSecurity(EncryptionEndpointSecurityFilter security) {
        this.security = security;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                    Object handler) {
                return !(handler instanceof HandlerMethod method
                        && EncryptionController.class.isAssignableFrom(method.getBeanType()))
                        || security.authorize(request, response);
            }
        }).order(Ordered.HIGHEST_PRECEDENCE);
    }
}
