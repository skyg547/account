package com.ho.account.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * @Masked 어노테이션이 붙은 필드를 마스킹 처리하는 Jackson Serializer
 */
public class MaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private String pattern;

    public MaskingSerializer() {}

    public MaskingSerializer(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        switch (pattern) {
            case "REG_NO": // 사업자번호 마스킹 (예: 123-45-***** )
                gen.writeString(maskRegistrationNumber(value));
                break;
            case "ACCOUNT": // 계좌번호 마스킹 (예: 110-***-123456)
                gen.writeString(maskAccountNumber(value));
                break;
            case "EMAIL": // 이메일 마스킹 (예: te**@example.com)
                gen.writeString(maskEmail(value));
                break;
            default:
                gen.writeString("**********");
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        Masked masked = property.getAnnotation(Masked.class);
        if (masked != null) {
            return new MaskingSerializer(masked.pattern());
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    private String maskRegistrationNumber(String val) {
        if (val.length() < 7) return "*****";
        return val.substring(0, 7) + "-***-**"; // 단순화된 예시
    }

    private String maskAccountNumber(String val) {
        if (val.length() < 6) return "******";
        return val.substring(0, 4) + "-***-" + val.substring(Math.min(val.length(), 10));
    }

    private String maskEmail(String val) {
        int atIndex = val.indexOf("@");
        if (atIndex <= 1) return "****@****";
        return val.substring(0, 2) + "****" + val.substring(atIndex);
    }
}
