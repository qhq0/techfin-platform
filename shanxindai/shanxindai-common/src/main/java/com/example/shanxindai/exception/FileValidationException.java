package com.example.shanxindai.exception;

import lombok.Getter;

@Getter
public class FileValidationException extends BusinessException {

    private final String fileName;

    public FileValidationException(String fileName, String code, String message) {
        super(code, message);
        this.fileName = fileName;
    }
}
