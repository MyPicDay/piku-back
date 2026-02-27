package com.pikume.back.diary.exception;

public class DuplicateDiaryException extends RuntimeException {
    public DuplicateDiaryException(String message) {
        super(message);
    }
}
