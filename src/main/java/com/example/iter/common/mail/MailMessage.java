package com.example.iter.common.mail;

// 범용 메일 발송 요청. notification 도메인 외에 회원가입 인증 등 다른 곳에서도 재사용할 수 있도록 특정 도메인 지식(rentalId 등)을 담지 않고 to/from/subject/body만 가진다.
public record MailMessage(String to, String from, String subject, String body) {
}
