package com.example.bankcards.exception;

public class TransferException extends RuntimeException {
    public TransferException(String message) {
        super(message);
    }
}
