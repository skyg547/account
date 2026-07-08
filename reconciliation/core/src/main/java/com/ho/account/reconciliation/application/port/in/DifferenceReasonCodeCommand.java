package com.ho.account.reconciliation.application.port.in;

/**
 * 대사 차이 사유 코드 생성/수정 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: 차이는 반드시 원인 코드로 정리되어야 합니다. 이 command는 코드명,
 * 사용자 표시명, 조정 전표 필요 여부를 core 업무 흐름에 전달합니다.</p>
 */
public record DifferenceReasonCodeCommand(
        String code,
        String name,
        String description,
        boolean adjustable,
        boolean active
) {
    public DifferenceReasonCodeCommand {
        code = requireText(code, "code");
        name = requireText(name, "name");
        description = trimToNull(description);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}