package com.example.score.common;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ApiResp<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> ApiResp<T> ok(T data) {
        return new ApiResp<>(0, "OK", data, null);
    }

    public static <T> ApiResp<T> error(int code, String message) {
        return new ApiResp<>(code, message, null, null);
    }
}
