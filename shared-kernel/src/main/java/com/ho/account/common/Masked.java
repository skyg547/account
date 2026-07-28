package com.ho.account.common;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jackson JSON 출력에서 문자열 마스킹 정책을 선택합니다.
 *
 * <p>{@link JsonSerialize}와 {@link JacksonAnnotationsInside}를 함께 선언해 필드/접근자에 이
 * 애노테이션을 붙이면 {@link MaskingSerializer}가 실제로 호출됩니다.</p>
 */
@JacksonAnnotationsInside
@JsonSerialize(using = MaskingSerializer.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Masked {

    String pattern() default "DEFAULT";
}