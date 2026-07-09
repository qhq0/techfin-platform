package com.example.shanxindai;

import com.example.shanxindai.dto.response.CommonResp;
import com.example.shanxindai.exception.BusinessException;
import com.example.shanxindai.exception.FileValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResp<Void> handleFileValidation(FileValidationException e) {
        return CommonResp.error(-1, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResp<Void> handleBusiness(BusinessException e) {
        log.warn("Business exception: code={}, message={}", e.getCode(), e.getMessage());
        return CommonResp.error(-1, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResp<Void> handleMaxSize(MaxUploadSizeExceededException e) {
        return CommonResp.error(-1, "文件大小超过限制");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResp<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .filter(m -> m != null)
                .collect(Collectors.joining("; "));
        return CommonResp.error(-1, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResp<Void> handleUnknown(Exception e) {
        log.error("Unexpected error", e);
        return CommonResp.error(-1, "系统繁忙，请稍后重试");
    }
}
