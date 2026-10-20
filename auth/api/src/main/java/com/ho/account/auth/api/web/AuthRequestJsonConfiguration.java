package com.ho.account.auth.api.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthRequestJsonConfiguration {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer rejectScalarCoercion() {
        // Preserve JSON field types so malformed authentication inputs fail before use-case calls.
        return builder -> {
            builder.featuresToDisable(
                    MapperFeature.ALLOW_COERCION_OF_SCALARS,
                    DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            // Jackson still coerces numeric and boolean JSON into String with the mapper flag disabled.
            builder.postConfigurer(mapper -> mapper.coercionConfigFor(LogicalType.Textual)
                    .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                    .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
        };
    }
}
