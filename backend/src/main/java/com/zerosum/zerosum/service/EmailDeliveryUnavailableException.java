package com.zerosum.zerosum.service;

public class EmailDeliveryUnavailableException extends RuntimeException {
    public EmailDeliveryUnavailableException(String message) { super(message); }
}
