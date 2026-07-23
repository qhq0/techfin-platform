package com.ccb.techfin.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(0)
                .msg("成功")
                .data(data)
                .build();
    }

    public static <T> Result<T> success(String msg, T data) {
        return Result.<T>builder()
                .code(0)
                .msg(msg)
                .data(data)
                .build();
    }

    public static <T> Result<T> fail(int code, String msg) {
        return Result.<T>builder()
                .code(code)
                .msg(msg)
                .build();
    }
}
