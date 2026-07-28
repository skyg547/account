package com.ho.account.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import java.io.IOException;
import java.util.Locale;

/**
 * {@link Masked}가 붙은 문자열을 외부 JSON으로 내보내기 직전에 마스킹합니다.
 *
 * <p>원본 엔티티 값은 바꾸지 않고 직렬화 결과만 가립니다. 패턴이 없거나 알 수 없는 경우에는
 * 원문을 그대로 보내지 않고 전체 마스킹하여 fail-closed로 처리합니다.</p>
 */
public class MaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private static final String DEFAULT_PATTERN = "DEFAULT";
    private static final String DEFAULT_MASK = "**********";

    private final String pattern;

    public MaskingSerializer() {
        this(DEFAULT_PATTERN);
    }

    public MaskingSerializer(String pattern) {
        this.pattern = normalizePattern(pattern);
    }

    @Override
    public void serialize(
            String value,
            JsonGenerator generator,
            SerializerProvider serializers) throws IOException {
        if (value == null) {
            generator.writeNull();
            return;
        }

        String maskedValue = switch (pattern) {
            case "REG_NO" -> maskMiddle(value, 5, 0);
            case "ACCOUNT" -> maskMiddle(value, 3, 4);
            case "EMAIL" -> maskEmail(value);
            default -> DEFAULT_MASK;
        };
        generator.writeString(maskedValue);
    }

    @Override
    public JsonSerializer<?> createContextual(
            SerializerProvider provider,
            BeanProperty property) throws JsonMappingException {
        if (property == null) {
            return this;
        }
        Masked masked = property.getAnnotation(Masked.class);
        if (masked != null) {
            return new MaskingSerializer(masked.pattern());
        }
        return provider.findValueSerializer(property.getType(), property);
    }

    /**
     * 문자와 숫자만 노출 개수에 포함하고 하이픈 같은 구분자는 그대로 둡니다.
     */
    private String maskMiddle(String value, int visiblePrefix, int visibleSuffix) {
        int sensitiveCharacterCount = (int) value.chars()
                .filter(Character::isLetterOrDigit)
                .count();
        if (sensitiveCharacterCount <= visiblePrefix + visibleSuffix) {
            return DEFAULT_MASK;
        }

        StringBuilder masked = new StringBuilder(value.length());
        int sensitiveIndex = 0;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!Character.isLetterOrDigit(character)) {
                masked.append(character);
                continue;
            }

            boolean visible = sensitiveIndex < visiblePrefix
                    || sensitiveIndex >= sensitiveCharacterCount - visibleSuffix;
            masked.append(visible ? character : '*');
            sensitiveIndex++;
        }
        return masked.toString();
    }

    private String maskEmail(String value) {
        int atIndex = value.lastIndexOf('@');
        if (atIndex <= 0 || atIndex == value.length() - 1) {
            return DEFAULT_MASK;
        }

        String localPart = value.substring(0, atIndex);
        int visibleLength = Math.min(2, localPart.length());
        int maskedLength = Math.max(4, localPart.length() - visibleLength);
        return localPart.substring(0, visibleLength)
                + "*".repeat(maskedLength)
                + value.substring(atIndex);
    }

    private static String normalizePattern(String value) {
        return value == null || value.isBlank()
                ? DEFAULT_PATTERN
                : value.trim().toUpperCase(Locale.ROOT);
    }
}