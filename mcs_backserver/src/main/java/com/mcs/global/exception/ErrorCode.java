package com.mcs.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON-400", "입력값이 올바르지 않습니다."),
    BUSINESS_ERROR(HttpStatus.BAD_REQUEST, "BIZ-400", "업무 규칙을 위반했습니다."),

    // MCS 전용 에러 코드
    ZONE_NOT_FOUND(HttpStatus.NOT_FOUND, "MCS-ZONE-404", "구역 정보를 찾을 수 없습니다."),
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MCS-LOC-404", "로케이션 정보를 찾을 수 없습니다."),
    INBOUND_NOT_FOUND(HttpStatus.NOT_FOUND, "MCS-IB-404", "입고 오더를 찾을 수 없습니다."),
    OUTBOUND_NOT_FOUND(HttpStatus.NOT_FOUND, "MCS-OB-404", "출고 오더를 찾을 수 없습니다."),
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "MCS-TF-404", "이동 오더를 찾을 수 없습니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "MCS-INV-001", "가용 재고가 부족합니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "MCS-STATUS-001", "유효하지 않은 상태 전이입니다."),
    DUPLICATE_ZONE_CD(HttpStatus.CONFLICT, "MCS-ZONE-409", "이미 존재하는 구역코드입니다."),
    DUPLICATE_LOCATION_CD(HttpStatus.CONFLICT, "MCS-LOC-409", "이미 존재하는 로케이션코드입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
