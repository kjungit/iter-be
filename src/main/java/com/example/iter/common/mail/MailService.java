package com.example.iter.common.mail;

// 메일 발송 인터페이스. 구현체(JavaMailSender 등)를 갈아끼울 수 있도록 인터페이스로 분리.
public interface MailService {

    void send(MailMessage message);
}
