package com.mcs.global.response;

public record ApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, "OK", "success", null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, data);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
