package com.ho.account.closing.infrastructure.external;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.ho.account.contracts.journal.JournalPeriodCheckpoint;
import com.ho.account.contracts.journal.JournalPeriodFenceCommand;
import com.ho.account.contracts.journal.JournalPeriodFencePort;
import com.ho.account.contracts.journal.JournalPeriodFenceResult;
import com.ho.account.contracts.journal.JournalPeriodScope;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Private authenticated Journal control plane; ambiguous calls retain the caller's durable intent. */
@Component
@ConditionalOnProperty(prefix = "closing.journal.period-fence", name = "enabled", havingValue = "true")
public class HttpJournalPeriodFenceAdapter implements JournalPeriodFencePort {
    private final RestClient client;

    @Autowired
    public HttpJournalPeriodFenceAdapter(RestClient.Builder builder,
            @Value("${closing.journal.base-url}") String baseUrl,
            @Value("${closing.journal.period-fence.service-token}") String serviceToken,
            @Value("${closing.journal.period-fence.connect-timeout:2s}") String connectTimeout,
            @Value("${closing.journal.period-fence.read-timeout:5s}") String readTimeout) {
        this(builder, baseUrl, serviceToken, DurationStyle.detectAndParse(connectTimeout),
                DurationStyle.detectAndParse(readTimeout));
    }

    public HttpJournalPeriodFenceAdapter(RestClient.Builder builder, String baseUrl, String serviceToken,
            Duration connectTimeout, Duration readTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("Journal base URL is required");
        if (serviceToken == null || serviceToken.getBytes(StandardCharsets.UTF_8).length < 32
                || serviceToken.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Journal service token must contain at least 32 bytes without whitespace");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                // Never forward the credential, or interpret a redirect as acknowledgement.
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(timeout(connectTimeout));
        factory.setReadTimeout(timeout(readTimeout));
        client = builder.clone().baseUrl(baseUrl.trim()).requestFactory(factory)
                .messageConverters(converters -> {
                    for (int index = 0; index < converters.size(); index++) {
                        if (converters.get(index) instanceof MappingJackson2HttpMessageConverter json) {
                            var strict = new MappingJackson2HttpMessageConverter(json.getObjectMapper().copy()
                                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                                    .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                                    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                                    .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                                    .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                                    .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION));
                            strict.setSupportedMediaTypes(json.getSupportedMediaTypes());
                            converters.set(index, strict);
                        }
                    }
                })
                .defaultHeader("X-Service-Identity", "closing")
                .defaultHeader("Authorization", "Bearer " + serviceToken)
                .defaultStatusHandler(status -> status.value() != 200, (request, response) -> {
                    throw new IllegalStateException("Journal period control rejected the request (HTTP "
                            + response.getStatusCode().value() + ")");
                }).build();
    }

    @Override
    public JournalPeriodCheckpoint checkpoint(JournalPeriodScope scope) {
        JournalPeriodCheckpoint result = post("checkpoint", scope, JournalPeriodCheckpoint.class);
        if (result == null || !scope.equals(result.scope())) {
            throw new IllegalStateException("Journal checkpoint acknowledgement does not match the requested scope");
        }
        return result;
    }

    @Override
    public JournalPeriodFenceResult fence(JournalPeriodFenceCommand command) {
        return command("fence", command, JournalPeriodFenceResult.State.ACTIVE);
    }

    @Override
    public JournalPeriodFenceResult release(JournalPeriodFenceCommand command) {
        return command("release", command, JournalPeriodFenceResult.State.RELEASED);
    }

    private JournalPeriodFenceResult command(String operation, JournalPeriodFenceCommand command,
            JournalPeriodFenceResult.State expected) {
        JournalPeriodFenceResult result = post(operation, command, JournalPeriodFenceResult.class);
        if (result == null || !command.equals(result.command()) || result.state() != expected) {
            throw new IllegalStateException("Journal fence acknowledgement does not match the exact command and state");
        }
        return result;
    }

    private <T> T post(String operation, Object request, Class<T> type) {
        try {
            return client.post().uri("/api/internal/journal-periods/" + operation).body(request)
                    .retrieve().body(type);
        } catch (RestClientException failure) {
            // Transport exceptions may contain secrets or endpoint details; keep only a fixed message.
            throw new IllegalStateException("Journal period control unavailable; retry the same durable operation");
        }
    }

    private static int timeout(Duration value) {
        if (value == null || value.compareTo(Duration.ofMillis(1)) < 0
                || value.compareTo(Duration.ofSeconds(60)) > 0) {
            throw new IllegalArgumentException("Journal period control timeout must be between 1ms and 60s");
        }
        return (int) value.toMillis();
    }
}
