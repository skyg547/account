package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FrontendNginxRoutingPolicyTest {

    @Test
    @DisplayName("frontend-nginx/nginx.conf는 6대 서브패스 라우팅 및 보안 헤더를 모두 포함한다")
    void nginxConfigContainsAllRequiredSubPathRoutesAndSecurityHeaders() throws IOException {
        Path nginxConfPath = resolveFromRepositoryRoot("frontend-nginx", "nginx.conf");
        String content = Files.readString(nginxConfPath);

        // Security headers
        assertThat(content)
                .contains("X-Frame-Options")
                .contains("X-XSS-Protection")
                .contains("X-Content-Type-Options");

        // Sub-path routes
        assertThat(content)
                .contains("location /_next/static/")
                .contains("proxy_pass http://frontend:3000;")
                .contains("location /api/")
                .contains("proxy_pass http://gateway:8000;")
                .contains("location /grafana/")
                .contains("proxy_pass http://grafana:3000;")
                .contains("location /zipkin/")
                .contains("proxy_pass http://zipkin:9411/zipkin/;")
                .contains("location /kibana/")
                .contains("proxy_pass http://kibana:5601;")
                .contains("location /pgadmin/")
                .contains("proxy_pass http://pgadmin:80/;")
                .contains("proxy_set_header X-Script-Name /pgadmin;")
                .contains("location /");
    }

    @Test
    @DisplayName("Grafana 및 Kibana Compose 설정에 Nginx 서브패스 Base URL 환경변수가 동기화되어 있다")
    void observabilityComposeConfigsContainSubPathEnvironmentVariables() throws IOException {
        Path grafanaCompose = resolveFromRepositoryRoot("grafana", "docker-compose.yml");
        String grafanaContent = Files.readString(grafanaCompose);
        assertThat(grafanaContent)
                .contains("GF_SERVER_ROOT_URL")
                .contains("/grafana/")
                .contains("GF_SERVER_SERVE_FROM_SUB_PATH");

        Path kibanaCompose = resolveFromRepositoryRoot("kibana", "docker-compose.yml");
        String kibanaContent = Files.readString(kibanaCompose);
        assertThat(kibanaContent)
                .contains("SERVER_BASEPATH=/kibana")
                .contains("SERVER_REWRITEBASEPATH=false");

        Path kibanaYml = resolveFromRepositoryRoot("kibana", "kibana.yml");
        String kibanaYmlContent = Files.readString(kibanaYml);
        assertThat(kibanaYmlContent)
                .contains("server.basePath: \"/kibana\"")
                .contains("server.rewriteBasePath: false");
    }

    @Test
    @DisplayName("frontend-nginx의 Dockerfile 및 docker-compose.yml이 정상 구성되어 있다")
    void frontendNginxContainerConfigIsValid() throws IOException {
        Path dockerfile = resolveFromRepositoryRoot("frontend-nginx", "Dockerfile");
        String dockerfileContent = Files.readString(dockerfile);
        assertThat(dockerfileContent)
                .contains("FROM nginx:alpine")
                .contains("COPY nginx.conf /etc/nginx/conf.d/default.conf");

        Path compose = resolveFromRepositoryRoot("frontend-nginx", "docker-compose.yml");
        String composeContent = Files.readString(compose);
        assertThat(composeContent)
                .contains("frontend-nginx:")
                .contains("account-network");
    }

    private Path resolveFromRepositoryRoot(String... pathParts) {
        Path root = Files.exists(Path.of("settings.gradle")) ? Path.of("") : Path.of("..");
        Path resolved = root;
        for (String pathPart : pathParts) {
            resolved = resolved.resolve(pathPart);
        }
        if (!Files.exists(resolved)) {
            throw new IllegalStateException("configuration file was not found: " + resolved);
        }
        return resolved;
    }
}