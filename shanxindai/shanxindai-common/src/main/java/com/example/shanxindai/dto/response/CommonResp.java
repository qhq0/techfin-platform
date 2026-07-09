package com.example.shanxindai.dto.response;

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
public class CommonResp<T> {

    /** 业务码，0 代表成功，非 0 代表失败 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    public static <T> CommonResp<T> success(T data) {
        return CommonResp.<T>builder()
                .code(0)
                .msg("成功")
                .data(data)
                .build();
    }

    public static <T> CommonResp<T> success(String msg, T data) {
        return CommonResp.<T>builder()
                .code(0)
                .msg(msg)
                .data(data)
                .build();
    }

    public static <T> CommonResp<T> error(int code, String msg) {
        return CommonResp.<T>builder()
                .code(code)
                .msg(msg)
                .build();
    }
}
