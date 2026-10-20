package com.zerosum.zerosum.service;

public interface EmailSender {
    void send(String recipient, String subject, String body);
}
