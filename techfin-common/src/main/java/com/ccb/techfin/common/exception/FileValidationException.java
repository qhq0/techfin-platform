package com.ccb.techfin.common.exception;

public class FileValidationException extends BusinessException {

    public FileValidationException(String fileName, String code, String message) {
        super(code, message);
    }
}
