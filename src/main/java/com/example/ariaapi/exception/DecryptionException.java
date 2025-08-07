package com.example.ariaapi.exception;

/**
 * 복호화 과정에서 발생하는 오류를 나타내는 커스텀 예외 클래스입니다.
 */
public class DecryptionException extends RuntimeException {
    public DecryptionException(String message) {
        super(message);
    }

    public DecryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}