package com.example.teamflow.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final T data;
    private final String message;
    private final ErrorBody error;

    private ApiResponse(T data, String message, ErrorBody error) {
        this.data = data;
        this.message = message;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, "ok", null);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(null, "ok", null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(null, null, new ErrorBody(code, message));
    }

    @Getter
    public static class ErrorBody {
        private final String code;
        private final String message;

        public ErrorBody(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
